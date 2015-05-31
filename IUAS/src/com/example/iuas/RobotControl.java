package com.example.iuas;

import jp.ksksue.driver.serial.FTDriver;

public class RobotControl implements Runnable {
	
	private static String command;
	private static double[] values;
	public static Thread robotControlThread;
	
	public static void control(String command, double... values) {
		RobotControl.command = command;
		RobotControl.values = values;
		robotControlThread = new Thread(new RobotControl());
		robotControlThread.start();
		try {
			robotControlThread.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void run() {
		if(command.equalsIgnoreCase("turn")) {
			
		}
		else if(command.equalsIgnoreCase("drive")) {
			robotDrive(values[0]);
		}
		else if(command.equalsIgnoreCase("flashLeds")) {
			
		}
	}
	
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static FTDriver com;
	public static BluetoothConnection btc;
	private final static double velocityDriveCalibration = 53.8; // calibration factor for drive
	private final static double velocityTurnCalibration = 10; // calibration factor for turn
	private static double x = 0; // x pos. of robot
	private static double y = 0; // y pos. of robot
	private static double theta = 0; // theta of robot
	private final static byte speed = 20; // speed of robot's wheels
	
	/**
	 * Robot drives a given distance in cm.
	 * Also accepts a calibration factor to match the input distance with the distance driven in real world.
	 * 
	 * @param distance_cm
	 * @param calib
	 */
	public void robotDrive(double distance) {
		boolean interruption = false;
		robotSetVelocity((byte) speed, (byte) speed);
		double startTime = System.currentTimeMillis();

		try {
			Thread.sleep((long) (Math.abs(distance) * velocityDriveCalibration * (20/Math.abs(speed))));
		}
		catch (InterruptedException e) {
			interruption = true;
		}
		finally {
			if (interruption) {
				x += Math.cos(Math.toRadians(theta)) * (System.currentTimeMillis() - startTime) * (1/velocityDriveCalibration) * (speed/20);
				y += Math.sin(Math.toRadians(theta)) * (System.currentTimeMillis() - startTime) * (1/velocityDriveCalibration) * (speed/20);
			}
			else {
				x += Math.cos(Math.toRadians(theta)) * distance;
				y += Math.sin(Math.toRadians(theta)) * distance;
			}
			robotStop();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Current position: x: " + x + ", y: " + y + ", theta: " + theta);
		}
	}
	
	
	/**
	 * Robot turns a given amount of degrees.
	 * Also accepts a calibration factor to match the input degrees with the degrees turned in real world.
	 * 
	 * @param degree
	 * @param calib
	 */
	public void robotTurn(int degree) {
		boolean interruption = false;
		
		if (degree >= 0) robotSetVelocity((byte) (-1 * speed), (byte) speed);
		else if (degree < 0) robotSetVelocity((byte) speed, (byte) (-1 *speed));
		
		double startTime = System.currentTimeMillis();
		
		try {
			Thread.sleep((long) (3.14159 * velocityTurnCalibration * (Math.abs(degree)/180) * velocityDriveCalibration * (20/Math.abs(speed))));
		}
		catch (InterruptedException e) {
			interruption = true;
		}
		finally {
			if (interruption) {
				double temp = (System.currentTimeMillis() - startTime) * (1/velocityDriveCalibration) * (180/(3.14159 * velocityTurnCalibration)) * (speed/20);
				theta += temp;
				System.out.println("Could only turn " + temp + "degrees");
			}
			else {
				theta += degree;
			}
			theta = theta % 360;
			robotStop();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Current position: x: " + x + ", y: " + y + ", theta: " + theta);
		}
	}
	
	
	/**
	 * Method to set velocity.
	 * @param left
	 * @param right
	 */
	public void robotSetVelocity(byte left, byte right) {
		comWrite(new byte[] { 'i', left, right, '\r', '\n' });
	}
	
	/**
	 * Method to control the Bar.
	 * @param value
	 */
	public void robotSetBar(byte value) {
		comWrite(new byte[] { 'o', value, '\r', '\n' });
	}
	

	/**
	 * Method to stop robot's movement.
	 */
	public void robotStop() {
		comWrite(new byte[] { 's', '\r', '\n' });
	}
	
	/**
	 * Method to set LEDs.
	 * @param red
	 * @param blue
	 */
	public void robotSetLeds(byte red, byte blue) {
		comWrite(new byte[] { 'u', red, blue, '\r', '\n' });
	}

	/** Let the robot shortly lit the red led, blue led or a mix of both.
	 * mode = 0: Show a mix of both LEDs.
	 * mode = 1: Show red LEDs.
	 * mode = 2: Show blue LEDs.
	 * 
	 * @param mode
	 */
	public void robotFlashLed(int mode) {
		switch (mode) {
		case 0:
			for (int i = 0; i < 4; i++) {
				robotSetLeds((byte) 0, (byte) 128);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				robotSetLeds((byte) 255, (byte) 0);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			robotSetLeds((byte) 0, (byte) 0);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;

		case 1:
			robotSetLeds((byte) 255, (byte) 0);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			robotSetLeds((byte) 0, (byte) 0);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		case 2:
			robotSetLeds((byte) 0, (byte) 128);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			robotSetLeds((byte) 0, (byte) 0);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}
	}
	
	/**
	 * Send write() command to robot.
	 * 
	 * @param data
	 */
	public void comWrite(byte[] data) {
		if (MainActivity.USE_DEVICE == 1) {
			if (com.isConnected()) {
				com.write(data);
			} else {
				System.out.println("Not connected!");
			}
		}
		else if (MainActivity.USE_DEVICE == 2) {
			if (btc == null) {
				System.out.println("BT not connected!");
			}
			else {
				btc.write(data);
			}
		}
	}

}
