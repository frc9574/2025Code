// Copyright 2021-2024 FRC 6328
// http://github.com/Mechanical-Advantage
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// version 3 as published by the Free Software Foundation or
// available in the root directory of this project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.

package frc.robot.subsystems.flywheel;

import com.revrobotics.CANSparkBase.ControlType;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkPIDController;
import com.revrobotics.SparkPIDController.ArbFFUnits;
import edu.wpi.first.math.util.Units;

/**
 * NOTE: To use the Spark Flex / NEO Vortex, replace all instances of "CANSparkMax" with
 * "CANSparkFlex".
 */
public class FlywheelIOSparkMax implements FlywheelIO {
  private static final double GEAR_RATIO = 1;

  private final CANSparkMax leader = new CANSparkMax(10, MotorType.kBrushless);
  private final CANSparkMax follower = new CANSparkMax(11, MotorType.kBrushless);
  private final RelativeEncoder leaderEncoder = leader.getEncoder();
  private final SparkPIDController leaderPid = leader.getPIDController();
  private final SparkPIDController followerPid = follower.getPIDController();

  public FlywheelIOSparkMax() {
    leader.restoreFactoryDefaults();
    follower.restoreFactoryDefaults();

    leader.setCANTimeout(250);
    follower.setCANTimeout(250);

    leader.setInverted(true);
    follower.setInverted(false);

    leader.setIdleMode(IdleMode.kBrake);
    follower.setIdleMode(IdleMode.kBrake);

    leader.enableVoltageCompensation(12.0);
    leader.setSmartCurrentLimit(40);
    follower.enableVoltageCompensation(12.0);
    follower.setSmartCurrentLimit(40);

    configurePID(0.0001, 0.000001, 0);

    leader.burnFlash();
    follower.burnFlash();
  }

  @Override
  public void updateInputs(FlywheelIOInputs inputs) {
    inputs.positionRad = Units.rotationsToRadians(leaderEncoder.getPosition() / GEAR_RATIO);
    inputs.velocityRadPerSec =
        Units.rotationsPerMinuteToRadiansPerSecond(leaderEncoder.getVelocity() / GEAR_RATIO);
    inputs.appliedVolts = leader.getAppliedOutput() * leader.getBusVoltage();
    inputs.currentAmps = new double[] {leader.getOutputCurrent(), follower.getOutputCurrent()};
  }

  @Override
  public void setVoltage(double volts) {
    leader.setVoltage(volts);
    follower.setVoltage(volts);
  }

  @Override
  public void setVelocity(double velocityRadPerSec, double ffVolts) {
    leaderPid.setReference(
        Units.radiansPerSecondToRotationsPerMinute(velocityRadPerSec) * GEAR_RATIO,
        ControlType.kVelocity,
        0,
        ffVolts,
        ArbFFUnits.kVoltage);
    followerPid.setReference(
        Units.radiansPerSecondToRotationsPerMinute(velocityRadPerSec) * GEAR_RATIO,
        ControlType.kVelocity,
        0,
        ffVolts,
        ArbFFUnits.kVoltage);
  }

  @Override
  public void setVelocity(
      double topVelocityRadPerSec,
      double topffVolts,
      double bottomVelocityRadPerSec,
      double bottomffVolts) {
    leaderPid.setReference(
        Units.radiansPerSecondToRotationsPerMinute(bottomVelocityRadPerSec) * GEAR_RATIO,
        ControlType.kVelocity,
        1,
        topffVolts,
        ArbFFUnits.kVoltage);
    followerPid.setReference(
        Units.radiansPerSecondToRotationsPerMinute(bottomVelocityRadPerSec) * GEAR_RATIO,
        ControlType.kVelocity,
        1,
        bottomffVolts,
        ArbFFUnits.kVoltage);
  }

  @Override
  public void stop() {
    leader.stopMotor();
    follower.stopMotor();
  }

  @Override
  public void configurePID(double kP, double kI, double kD) {
    leaderPid.setP(kP, 0);
    leaderPid.setI(kI, 0);
    leaderPid.setD(kD, 0);
    leaderPid.setFF(0, 0);
    followerPid.setP(kP, 0);
    followerPid.setI(kI, 0);
    followerPid.setD(kD, 0);
    followerPid.setFF(0, 0);
  }
}
