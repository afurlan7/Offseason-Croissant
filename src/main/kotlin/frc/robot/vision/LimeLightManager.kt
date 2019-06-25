package frc.robot.vision

import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.Timer
import frc.robot.Constants
import frc.robot.subsystems.drive.Drive
//import frc.robot.subsystems.DriveTrain
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d
import org.ghrobotics.lib.mathematics.twodim.geometry.Translation2d
import org.ghrobotics.lib.mathematics.units.degree
import org.ghrobotics.lib.mathematics.units.inch
import org.ghrobotics.lib.mathematics.units.radian
import org.opencv.core.*
import org.opencv.core.MatOfPoint2f
import org.opencv.calib3d.Calib3d
import org.opencv.core.Mat
import kotlin.math.*
import org.opencv.core.CvType

@Deprecated("SolvePNP boroken, depreciated until alternative distance estimation method is tested (ahem target width?)")
object LimeLightManager {

    val txEntry = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tcornx")
    val tyEntry = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tcorny")

    init {
        NetworkTableInstance.getDefault().getTable("limelight").addEntryListener("tcornx",
                { table, _, _, _, _ ->

                    updateTrackedTargetsFromTcorner(table,
                            Drive.robotPosition,
                            Timer.getFPGATimestamp())
                },
                EntryListenerFlags.kNew or EntryListenerFlags.kUpdate)
    }

    private fun updateTrackedTargetsFromTcorner(table: NetworkTable, robotPosition: Pose2d, timestamp: Double) {
        val tx = txEntry.getDoubleArray(arrayOf(0.0)).toList()
        val ty = tyEntry.getDoubleArray(arrayOf(0.0)).toList()

        val corners = listOf(tx, ty)

        val pose = solvePNPFromCorners(corners) ?: return // return if null, continue if non-null

        if (pose.translation.x.absoluteValue > (Constants.kRobotLength / 2.0 - 5.inch).meter ||
                pose.translation.y.absoluteValue > (Constants.kRobotWidth / 2.0).meter)
            return

        val globalPose = Constants.kCenterToFrontCamera + (pose + robotPosition)

        TargetTracker.addSamples(
                timestamp, listOf(globalPose)
        )
    }

    private fun solvePNPFromCorners(input: List<List<Double>>): Pose2d? {

        if (input[0].size != 4 || input[1].size != 4) return null

        val corners = listOf(
                Point(input[0][0], input[0][1]),
                Point(input[1][0], input[1][1]),
                Point(input[2][0], input[2][1]),
                Point(input[3][0], input[3][1])
        ).sortedBy { it.x }

        val leftCorners = corners.let { list ->
            // find the minimum

            val sorted = list.sortedBy { it.x }

            val leftMost = listOf(sorted[0], sorted[1])

            return@let leftMost.sortedBy { it.y }
        }

        val rightCorners = listOf(corners[2], corners[3]).sortedBy { it.y }

        // in the form left top, left bottom, right bottom, right top
        // note that positive y is down!
        val imagePoints = MatOfPoint2f(
                leftCorners[0],
                leftCorners[1],
                rightCorners[1],
                rightCorners[0]
        )

        val rvec = Mat()
        val tvec = Mat()

        val retval = Calib3d.solvePnP(mObjectPoints, imagePoints, mCameraMatrix,
                mDistortionCoefficients, rvec, tvec)

        if (!retval) return null

        return computeOutput(rvec, tvec)

    }

    private fun computeOutput(rvec: Mat, tvec: Mat): Pose2d? {

        val x = tvec.get(0, 0)[0]
        // TODO behavior of the mat access boi
        val z = sin(tilt_angle) * tvec.get(1, 0)[0] + cos(tilt_angle) * tvec.get(2, 0)[0]

        // distance in the horizontal plane between camera and target
        val distance = sqrt(x * x + z * z)

        // horizontal angle between camera center line and target
        val angle1 = atan2(x, z)

        val rot = Mat()
        Calib3d.Rodrigues(rvec, rot)

        // implementation from https://www.chiefdelphi.com/t/limelight-real-world-camera-positioning/343941/29?u=thatmattguy

        val projectionMatrix = Mat(3, 4, CvType.CV_64F)
        projectionMatrix.put(0, 0,
                rot.get(0, 0)[0], rot.get(0, 1)[0], rot.get(0, 2)[0], tvec.get(0, 0)[0],
                rot.get(1, 0)[0], rot.get(1, 1)[0], rot.get(1, 2)[0], tvec.get(1, 0)[0],
                rot.get(2, 0)[0], rot.get(2, 1)[0], rot.get(2, 2)[0], tvec.get(2, 0)[0]
        )

        val cameraMatrix = Mat()
        val rotMatrix = Mat()
        val transVect = Mat()
        val rotMatrixX = Mat()
        val rotMatrixY = Mat()
        val rotMatrixZ = Mat()
        val eulerAngles = Mat()
        Calib3d.decomposeProjectionMatrix(projectionMatrix, cameraMatrix, rotMatrix, transVect, rotMatrixX, rotMatrixY, rotMatrixZ, eulerAngles)

        val yawInDegrees = eulerAngles.get(1, 0)[0]

        return Pose2d(Translation2d(distance.inch, angle1.radian.toRotation2d()), yawInDegrees.degree)
    }

    val TARGET_WIDTH = 14.627 // inch
    val TARGET_HEIGHT = 5.826 // inch
    val tilt_angle = Math.toRadians(25.0) // TODO confirm

    val mObjectPoints = MatOfPoint3f(
            Point3(-TARGET_WIDTH / 2.0, TARGET_HEIGHT / 2.0, 0.0),
                Point3(-TARGET_WIDTH / 2.0, -TARGET_HEIGHT / 2.0, 0.0),
                Point3(TARGET_WIDTH / 2.0, -TARGET_HEIGHT / 2.0, 0.0),
                Point3(TARGET_WIDTH / 2.0, TARGET_HEIGHT / 2.0, 0.0)
    )

    val mCameraMatrix = Mat.eye(3, 3, CvType.CV_64F)

    init {
        mCameraMatrix.put(0, 0, 2.5751292067328632e+02)
        mCameraMatrix.put(0, 2, 1.5971077914723165e+02)
        mCameraMatrix.put(1, 1, 2.5635071715912881e+02)
        mCameraMatrix.put(1, 2, 1.1971433393615548e+02)
    }

    val mDistortionCoefficients = MatOfDouble(2.9684613693070039e-01, -1.4380252254747885e+00, -2.2098421479494509e-03, -3.3894563533907176e-03, 2.5344430354806740e+00)
}