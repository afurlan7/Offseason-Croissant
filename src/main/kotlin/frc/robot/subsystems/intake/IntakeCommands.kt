@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package frc.robot.subsystems.intake

import edu.wpi.first.wpilibj.frc2.command.InstantCommand
import frc.robot.Controls
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.mathematics.units.derived.volt
import kotlin.math.abs
import org.ghrobotics.lib.wrappers.hid.getX
import org.ghrobotics.lib.wrappers.hid.getY
import edu.wpi.first.wpilibj.GenericHID
import org.ghrobotics.lib.utils.withDeadband

val closeIntake = InstantCommand(Runnable { Intake.wantsOpen = false })
val openIntake = InstantCommand(Runnable { Intake.wantsOpen = true })

class IntakeHatchCommand(val releasing: Boolean) : FalconCommand(Intake) {

    override fun initialize() {
        println("intaking hatch command")
        Intake.hatchMotorOutput = 12.volt * (if (releasing) -1 else 1)
        Intake.cargoMotorOutput = 0.volt
        Intake.wantsOpen = false
    }

    override fun end(interrupted: Boolean) {
        Intake.wantsOpen = false
        Intake.hatchMotorOutput = 0.volt
        Intake.cargoMotorOutput = 0.volt
    }
}

class IntakeCargoCommand(val releasing: Boolean) : FalconCommand(Intake) {

//    var wasOpen: Boolean = false

    override fun initialize() {
        println("${if (releasing) "releasing" else "intaking"} cargo command!")
//        wasOpen = Intake.wantsOpen
        Intake.wantsOpen = !releasing

        Intake.hatchMotorOutput = 12.volt * (if (releasing) 1 else -1)
        Intake.cargoMotorOutput = 12.volt * (if (!releasing) 1 else -1)

        super.initialize()
    }

    override fun end(interrupted: Boolean) {
        Intake.wantsOpen = false
        Intake.cargoMotorOutput = 3.volt
        Intake.hatchMotorOutput = 3.volt
        GlobalScope.launch {
            delay(500)
            Intake.cargoMotorOutput = 0.volt
            Intake.hatchMotorOutput = 0.volt
        }
        super.end(interrupted)
    }
}

class IntakeTeleopCommand : FalconCommand(Intake) {

    override fun execute() {
        val cargoSpeed = -cargoSource()
        val hatchSpeed = -hatchSource()

        if (abs(cargoSpeed) > 0.2) {
            Intake.hatchMotorOutput = (-12).volt * cargoSpeed
            Intake.cargoMotorOutput = 12.volt * cargoSpeed
        } else {
            Intake.hatchMotorOutput = 12.volt * hatchSpeed
            Intake.cargoMotorOutput = 0.volt
        }
    }

    override fun end(interrupted: Boolean) {
        Intake.hatchMotorOutput = 0.volt
        Intake.cargoMotorOutput = 0.volt
    }

    companion object {
        private const val kDeadband = 0.08
        val cargoSource by lazy { Controls.driverFalconXbox.getY(GenericHID.Hand.kLeft).withDeadband(kDeadband)}
        private val hatchSource by lazy { Controls.driverFalconXbox.getX(GenericHID.Hand.kRight)}
 //      val cargoSource by lazy { Controls.operatorFalconHID.getRawAxis(0) }
//       val hatchSource by lazy { Controls.operatorFalconHID.getRawAxis(1) }
    }
}

class IntakeCloseCommand : FalconCommand(Intake) {
    init {
        Intake.wantsOpen = false
    }
}
