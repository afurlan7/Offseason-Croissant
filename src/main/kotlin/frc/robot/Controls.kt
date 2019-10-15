package frc.robot

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.Joystick
import edu.wpi.first.wpilibj.XboxController
import edu.wpi.first.wpilibj.frc2.command.* // ktlint-disable no-wildcard-imports
import frc.robot.Controls.driverFalconXbox
import frc.robot.auto.routines.BottomRocketRoutine2
import frc.robot.auto.routines.TestRoutine
import frc.robot.subsystems.climb.ClimbSubsystem
import frc.robot.subsystems.drive.CharacterizationCommand
import frc.robot.subsystems.drive.DriveSubsystem
import frc.robot.subsystems.drive.VisionDriveCommand
import frc.robot.subsystems.intake.IntakeCargoCommand
import frc.robot.subsystems.intake.IntakeHatchCommand
import frc.robot.subsystems.superstructure.* // ktlint-disable no-wildcard-imports
import org.ghrobotics.lib.commands.sequential
import org.ghrobotics.lib.mathematics.units.derived.degree
import org.ghrobotics.lib.mathematics.units.inch
import org.ghrobotics.lib.wrappers.hid.* // ktlint-disable no-wildcard-imports
import org.team5940.pantry.lib.Updatable
//switch statement for D,O & E


object Controls : Updatable {
    override fun update() {
        driverFalconXbox.update()
       // operatorFalconHID.update()
    }

    var isClimbing = false

    private val zero = ZeroSuperStructureRoutine()

    val driverControllerLowLevel = XboxController(0)
    val driverFalconXbox = driverControllerLowLevel.mapControls {
        registerEmergencyMode()

        state({ driverControllerLowLevel.getRawButton(9) }) {

            state({ driverControllerLowLevel.getRawButton((10)) }) {
                //button(kBumperLeft).change(IntakeHatchCommand(releasing = true))

//                button(kBumperLeft).changeOn(Superstructure.kHatchLow).changeOff { Superstructure.kStowed.schedule() }

                state({ driverControllerLowLevel.getBumper(GenericHID.Hand.kRight) }) {
                    button(kY).changeOn(Superstructure.kHatchHigh).changeOff { Superstructure.kStowed.schedule() }
                    triggerAxisButton(GenericHID.Hand.kLeft).changeOn(Superstructure.kHatchMid).changeOff { Superstructure.kStowed.schedule() }
                    button(kBumperLeft).changeOn(Superstructure.kHatchLow).changeOff { Superstructure.kStowed.schedule() }
                }
                /* state({ operatorXboxController.getBumper(GenericHID.Hand.kLeft) }) {
                     button(kY).changeOn(Superstructure.kCargoHigh).changeOff { Superstructure.kStowed.schedule() }
                     button(kBumperRight).changeOn(Superstructure.kCargoMid).changeOff { Superstructure.kStowed.schedule() }
                     triggerAxisButton(GenericHID.Hand.kLeft).changeOn(Superstructure.kCargoLow).changeOff { Superstructure.kStowed.schedule() }
                     button(kB).changeOn(Superstructure.kCargoShip).changeOff { Superstructure.kStowed.schedule() }
                 } */
            }
        }


}

//    val auxXbox = XboxController(1)
//    val auxFalconXbox = auxXbox.mapControls {
//        button(kY).changeOn(ClimbSubsystem.fullS3ndClimbCommand)
//    }

val operatorXboxController = XboxController(5)
val operatorFalconHID = operatorXboxController.mapControls {
    //  val operatorJoy = Joystick (5)
    // val operatorFalconHID = operatorJoy.mapControls {
    state({ driverControllerLowLevel.getRawButton(9) }) {
        state({ driverControllerLowLevel.getRawButton((10)) }) {
            //button(kBumperLeft).change(IntakeHatchCommand(releasing = true))


            state({ operatorXboxController.getBumper(GenericHID.Hand.kRight) }) {
                triggerAxisButton(GenericHID.Hand.kRight).changeOn(Superstructure.kHatchHigh).changeOff { Superstructure.kStowed.schedule() }
                triggerAxisButton(GenericHID.Hand.kLeft).changeOn(Superstructure.kHatchMid).changeOff { Superstructure.kStowed.schedule() }
                button(kBumperLeft).changeOn(Superstructure.kHatchLow).changeOff { Superstructure.kStowed.schedule() }
            }
           /* state({ operatorXboxController.getBumper(GenericHID.Hand.kLeft) }) {
                button(kY).changeOn(Superstructure.kCargoHigh).changeOff { Superstructure.kStowed.schedule() }
                button(kBumperRight).changeOn(Superstructure.kCargoMid).changeOff { Superstructure.kStowed.schedule() }
                triggerAxisButton(GenericHID.Hand.kLeft).changeOn(Superstructure.kCargoLow).changeOff { Superstructure.kStowed.schedule() }
                button(kB).changeOn(Superstructure.kCargoShip).changeOff { Superstructure.kStowed.schedule() }
            } */
        }
    }
    // intake hatch

    // intake ball
    // cargo -- intake is a bit tricky, it'll go to the intake preset automatically
    // the lessThanAxisButton represents "intaking", and the greaterThanAxisButton represents "outtaking"
    val cargoCommand = sequential {
        +PrintCommand("running cargoCommand")
        +Superstructure.kCargoIntake
        +IntakeCargoCommand(releasing = false)
    }
    triggerAxisButton(GenericHID.Hand.kLeft).changeOff {
        (sequential { +ClosedLoopWristMove(40.degree); +Superstructure.kStowed; }).schedule()
    }.change(cargoCommand)


    val cargoRelease = sequential {
        +PrintCommand("running cargoRelease")
        +Superstructure.kCargoIntake
        +IntakeCargoCommand(releasing = true)
    }

/*      **** OLD JOYSTICK CODE *****
        // climbing

           // cargo presets
           // button(12).changeOn(Superstructure.kCargoIntake.andThen { Intake.wantsOpen = true }) // .changeOff { Superstructure.kStowed.schedule() }
            //button(kX).changeOn(DriveSubsystem.lowGear)
           // button(kBumperLeft).changeOn()
            button(7).changeOn(Superstructure.kCargoLow) // .changeOff { Superstructure.kStowed.schedule() }
            button(6).changeOn(Superstructure.kCargoMid) // .changeOff { Superstructure.kStowed.schedule() }
            button(5).changeOn(Superstructure.kCargoHigh) // .changeOff { Superstructure.kStowed.schedule() }
            button(8).changeOn(Superstructure.kCargoShip) // .changeOff { Superstructure.kStowed.schedule() }

            // hatch presets
            button(3).changeOn(Superstructure.kHatchLow) // .changeOff { Superstructure.kStowed.schedule() }
            button(2).changeOn(Superstructure.kHatchMid) // .changeOff { Superstructure.kStowed.schedule() }
            button(1).changeOn(Superstructure.kHatchHigh) // .changeOff { Superstructure.kStowed.schedule() }

            // Stow (for now like this coz i dont wanna break anything
            button(10).changeOn(Superstructure.kStowed)

            button(9).changeOn(ClosedLoopElevatorMove { Elevator.currentState.position + 1.inch })
            button(11).changeOn(ClosedLoopElevatorMove { Elevator.currentState.position - 1.inch })

            // that one passthrough preset that doesnt snap back to normal
//            button(4).changeOn(Superstructure.kBackHatchFromLoadingStation)

            // hatches
            lessThanAxisButton(1).change(IntakeHatchCommand(releasing = false))
            greaterThanAxisButton(1).change(IntakeHatchCommand(releasing = true))

            // cargo -- intake is a bit tricky, it'll go to the intake preset automatically
            // the lessThanAxisButton represents "intaking", and the greaterThanAxisButton represents "outtaking"
            val cargoCommand = sequential { +PrintCommand("running cargoCommand"); +Superstructure.kCargoIntake; +IntakeCargoCommand(releasing = false) }
            lessThanAxisButton(0).changeOff { (sequential { +ClosedLoopWristMove(40.degree) ; +Superstructure.kStowed; }).schedule() }.change(cargoCommand)
            greaterThanAxisButton(0).changeOff { }.change(IntakeCargoCommand(true))
        state({ isClimbing }) {
            button(12).changeOn(ClimbSubsystem.fullS3ndClimbCommand)
        }

        button(4).changeOn(ClimbSubsystem.prepMove).changeOn { isClimbing = true }
*/


}


    private fun Command.andThen(block: () -> Unit) = sequential { +this@andThen; +InstantCommand(Runnable(block)) }

    private fun FalconXboxBuilder.registerEmergencyMode() {
        button(kBack).changeOn {
            Robot.activateEmergency()
        }
        button(kStart).changeOn {
            Robot.recoverFromEmergency()
        }
    }

}
