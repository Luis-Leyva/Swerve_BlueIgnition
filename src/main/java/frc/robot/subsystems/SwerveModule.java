// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkBase.ControlType;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.AbsoluteSensorRangeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;

public class SwerveModule extends SubsystemBase {
	// Motores
	private CANSparkMax driveMotor;
	private CANSparkMax turnMotor;

	// Sensores
	private CANcoder absoluteEncoder;
	private RelativeEncoder turnEncoder;
	private RelativeEncoder driveEncoder;

	// Module Name
	private String name;

	// Module State
	private SwerveModuleState currentState = new SwerveModuleState();

	/** Creates a new SwerveModule. */
	public SwerveModule(int driveMotorID, int turnMotorID, int absoluteEncoderID, double offSet, String name,
			String canBus) {

		this.name = name;
		this.driveMotor = new CANSparkMax(driveMotorID, CANSparkMax.MotorType.kBrushless);
		this.turnMotor = new CANSparkMax(turnMotorID, CANSparkMax.MotorType.kBrushless);
		this.absoluteEncoder = new CANcoder(absoluteEncoderID, canBus);

		// Enconder configuration
		CANcoderConfiguration encoderConfig = new CANcoderConfiguration();
		encoderConfig.MagnetSensor.SensorDirection = SensorDirectionValue.CounterClockwise_Positive;
		encoderConfig.MagnetSensor.AbsoluteSensorRange = AbsoluteSensorRangeValue.Signed_PlusMinusHalf;
		encoderConfig.MagnetSensor.MagnetOffset = offSet;

		// Configure Relative Encoder
		this.turnEncoder = turnMotor.getEncoder();
		this.driveEncoder = driveMotor.getEncoder();

		// Set encoder configuration
		turnEncoder.setPositionConversionFactor(Constants.SwerveModuleConstants.kRotationToDegree);
		turnEncoder.setVelocityConversionFactor(Constants.SwerveModuleConstants.kRPMToDegreePerSecond);

		driveEncoder.setPositionConversionFactor(Constants.SwerveModuleConstants.kRotationToMeter);
		driveEncoder.setVelocityConversionFactor(Constants.SwerveModuleConstants.kRPMToMeterPerSecond);

		// Set motor configs
		turnMotor.setInverted(false);
		turnMotor.setClosedLoopRampRate(0.1);
		turnMotor.setSmartCurrentLimit(20);
		turnMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);

		driveMotor.setInverted(false);
		driveMotor.setOpenLoopRampRate(0.1);
		driveMotor.setSmartCurrentLimit(20);
		driveMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);

		// Set PID values
		turnMotor.getPIDController().setP(0.1);
		turnMotor.getPIDController().setI(0.0);
		turnMotor.getPIDController().setD(0.0);

		// Set encoders to zero
		resetTurnEncoder();
		driveEncoder.setPosition(0.0);
	}

	// Reset the turn encoder to match the absolute encoder
	public void resetTurnEncoder() {
		// Get the absolute encoder position in degrees
		double absolutePosition = absoluteEncoder.getAbsolutePosition().getValue() * 360.0;
		// Set the turn encoder position to the absolute encoder position
		turnEncoder.setPosition(absolutePosition);
	}

	// Get the speed of the drive motor in meters per second
	public double getDriveSpeed() {
		return driveEncoder.getVelocity();
	}

	// Get the angle of the turn motor in degrees
	public double getTurnAngle() {
		return absoluteEncoder.getAbsolutePosition().getValue() * 360.0;
	}

	// Set the position in meters from the drive motor
	public double getDrivePosition() {
		return driveEncoder.getPosition();
	}

	// Set the velocity in meters per second to the drive motor
	public void setDriveSpeed(double speed) {
		driveMotor.set(speed);
	}

	// Get the actual module state
	public SwerveModuleState getActualState() {
		return new SwerveModuleState(getDriveSpeed(), Rotation2d.fromDegrees(getTurnAngle()));
	}

	// get the module position
	public SwerveModulePosition getModulePosition() {
		return new SwerveModulePosition(getDrivePosition(), Rotation2d.fromDegrees(getTurnAngle()));
	}

	public void setDesiredState(SwerveModuleState desiredState) {
		currentState = SwerveModuleState.optimize(desiredState, currentState.angle);

		// Set the drive motor speed
		setDriveSpeed(currentState.speedMetersPerSecond);

		// Set the turn motor angle
		turnMotor.getPIDController().setReference(currentState.angle.getDegrees(), ControlType.kPosition);
	}

	// Get the module position

	@Override
	public void periodic() {
		SmartDashboard.putNumber(name + " Speed", getDriveSpeed());
		SmartDashboard.putNumber(name + " Position", getDrivePosition());
		SmartDashboard.putNumber(name + " Angle", getTurnAngle());
		SmartDashboard.putNumber(name + " Target Angle", currentState.angle.getDegrees());

		// This method will be called once per scheduler run
	}
}
