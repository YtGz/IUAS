/**
 * This class is responsible for the basic commands of the robot
 * It implements Runnable
 * 
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import com.example.iuas.circle.Vector2;

import android.util.Pair;
import jp.ksksue.driver.serial.FTDriver;

public class RobotControl implements Runnable {
	
	private static String command;
	private static int[] values;
	public static Thread robotControlThread;
	
	public static void control(String command, int... values) {
		RobotControl.command = command;
		RobotControl.values = values;
		robotControlThread = new Thread(new RobotControl());
		System.out.println("Thread started");
		robotControlThread.start();
		try {
			robotControlThread.join();
			System.out.println("Thread joined");
		} catch (InterruptedException e) {
			System.out.println("Thread was interrupted");
		}
	}

	@Override
	public void run() {
		if(command.equalsIgnoreCase("turn")) {
			robotTurn(values[0]);
		}
		else if(command.equalsIgnoreCase("drive")) {
			robotDrive(values[0]);
		}
		else if(command.equalsIgnoreCase("flashLeds")) {
			robotFlashLed(values[0]);
		}
		
		else if(command.equalsIgnoreCase("setBar")){
			robotSetBar(values[0]);
		}
		
		else if(command.equalsIgnoreCase("setLed")){
			robotSetLeds(values[0], values[1]);
		}
		
		else if(command.equalsIgnoreCase("setVelocity")){
			robotSetVelocity(values[0], values[1]);
		}
		
	}
	
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static FTDriver com;
	public static BluetoothConnection btc;
	private final static double velocityOffset = 53.8; // calibration factor for drive
	private final static double velocityTurnCalibration = 9.51; // calibration factor for turn
	private static double x = 0; // x pos. of robot
	private static double y = 0; // y pos. of robot
	private static double theta = 0; // theta of robot
	private final static double speed = 20; // speed of robot's wheels
	
	/**
	 * Robot drives a given distance in cm.
	 * Also accepts a calibration factor to match the input distance with the distance driven in real world.
	 * 
	 * @param distance_cm
	 * @param calib
	 */
	public void robotDrive(int distance) {
		if(distance != 0) {
			boolean interruption = false;
			if(distance == 10 || distance == 13) {
				++distance;
			}
			if (distance > 0) robotSetVelocity((byte) speed, (byte) speed);
			else if (distance < 0) robotSetVelocity((byte) (-1 * speed), (byte) (-1 *speed));
			double startTime = System.currentTimeMillis();
	
			try {
				Thread.sleep((long) (Math.abs(distance) * velocityOffset * (20.0/Math.abs(speed))));
			}
			catch (InterruptedException e) {
				interruption = true;
			}
			finally {
				if (interruption) {
					x += Math.cos(Math.toRadians(theta)) * (System.currentTimeMillis() - startTime) * (1.0/velocityOffset) * (speed/20.0);
					y += Math.sin(Math.toRadians(theta)) * (System.currentTimeMillis() - startTime) * (1.0/velocityOffset) * (speed/20.0);
				}
				else {
					x += Math.cos(Math.toRadians(theta)) * distance;
					y += Math.sin(Math.toRadians(theta)) * distance;
				}
				//CameraFrameProcessingActivity.localization.setOdometryData(new Pair<Vector2, Double>(new Vector2(x, y), theta));
				robotStop();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Current position: x: " + x + ", y: " + y + ", theta: " + theta);
			}
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
		if(degree != 0) {
			boolean interruption = false;
			if(degree == 10 || degree == 13) {
				++degree;
			}
			if (degree > 0) robotSetVelocity((byte) (-1 * speed), (byte) speed);
			else if (degree < 0) robotSetVelocity((byte) speed, (byte) (-1 *speed));
			
			double startTime = System.currentTimeMillis();
			
			try {
				Thread.sleep((long) (3.14159 * velocityTurnCalibration * (Math.abs(degree)/180.0) * velocityOffset * (20.0/Math.abs(speed))));
			}
			catch (InterruptedException e) {
				interruption = true;
			}
			finally {
				if (interruption) {
					double temp = (System.currentTimeMillis() - startTime) * (1.0/velocityOffset) * (180.0/(3.14159 * velocityTurnCalibration)) * (speed/20.0);
					theta += temp;
					System.out.println("Could only turn " + temp + "degrees");
				}
				else {
					theta += degree;
				}
				theta = theta % 360;
				robotStop();
				//robotSetVelocity((byte) 0, (byte) 0);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Current position: x: " + x + ", y: " + y + ", theta: " + theta);
			}
		}
	}
	
	
	/**
	 * Method to set velocity.
	 * @param left
	 * @param right
	 */
	public void robotSetVelocity(int left, int right) {
		comWrite(new byte[] { 'i', (byte) left, (byte) right, '\r', '\n' });
	}
	
	/**
	 * Method to control the Bar.
	 * @param value
	 */
	public void robotSetBar(int value) {
		comWrite(new byte[] { 'o', (byte) value, '\r', '\n' });
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
	public void robotSetLeds(int red, int blue) {
		comWrite(new byte[] { 'u', (byte) red, (byte) blue, '\r', '\n' });
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
	public static void comWrite(byte[] data) {
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
