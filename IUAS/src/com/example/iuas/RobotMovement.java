package com.example.iuas;

import jp.ksksue.driver.serial.FTDriver;

import java.util.StringTokenizer;

import java.io.Flushable;
import java.lang.Math;

public class RobotMovement {

	public static FTDriver com;
	public static volatile double xRobot = 0;
	public static volatile double yRobot = 0;
	public static volatile double xBall = 0;
	public static volatile double yBall = 0;
	public static volatile double thetaRobot = 0;
	public static volatile boolean isSearching = false;
	public static volatile double velocityOffset = 53.8;
	public static volatile double velocityTurn = 9.51;
	public static int[] sensors = new int[8];
	
	public static void connect(FTDriver comm) {
		com = comm;
		if (com.begin(9600)) {
		System.out.println("Robot connected!\n");
		} else {
			System.out.println("Connection failed!\n");
		}
	}
	
	public static void robotSetVelocity(byte left, byte right) {
		comWrite(
				new byte[] { 'i', left, right, '\r', '\n' }
				);
	}
	
	public static boolean robotDrive(double distance, double speed) {
		System.out.println("Driving " + distance + " cm");
		
		boolean interrupted = false;
		robotSetVelocity((byte) speed, (byte) speed);
		double start = System.currentTimeMillis();
				
		try {
			Thread.sleep( (long) (Math.abs(distance) * velocityOffset * (20/Math.abs(speed))));
		} catch (InterruptedException e) {
			System.out.println("RobotDrive interrupted");
			System.out.flush();
			interrupted = true;
		} finally {
			if(interrupted) {
				xRobot += Math.cos(Math.toRadians(thetaRobot)) * 
						(System.currentTimeMillis() - start) * (1/velocityOffset) * (speed/20);
				yRobot += Math.sin(Math.toRadians(thetaRobot)) * 
						(System.currentTimeMillis() - start) * (1/velocityOffset) * (speed/20);
			} else {
				xRobot += Math.cos(Math.toRadians(thetaRobot)) * distance;
				yRobot += Math.sin(Math.toRadians(thetaRobot)) * distance;
			}
		}
		
		robotSetVelocity((byte) 0, (byte) 0);
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printPose();
		return interrupted;
	}
	
	public static boolean robotTurn(double theta, double speed) {
		System.out.println("Turning " + theta + " Degrees");
		
		boolean interrupted = false;
		
		if(theta >= 0)
			robotSetVelocity((byte) (-1 * speed), (byte) speed);
		if(theta < 0)
			robotSetVelocity((byte) speed, (byte) (-1 *speed));
		
		double start = System.currentTimeMillis();

		try {
			Thread.sleep( (long) (3.14159 * velocityTurn * (Math.abs(theta)/180) * velocityOffset * (20/Math.abs(speed))) );
		} catch (InterruptedException e) { 
			System.out.println("RobotTurn interrupted");
			System.out.flush();
			interrupted = true;
		} finally {
			if(interrupted) {
				double tmp = (System.currentTimeMillis() - start) * (1/velocityOffset) * (180/(3.14159 * velocityTurn)) * (speed/20);
				thetaRobot += tmp;
				System.out.println("Only Turned " + tmp + " Degrees");
			} else {
				thetaRobot += theta;
			}
			thetaRobot = thetaRobot % 360;
		}
		robotSetVelocity((byte) 0, (byte) 0);
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printPose();
		return interrupted;
	}
	
	
	public static boolean goTo(double x, double y, double theta) {
		System.out.println("Going to x " + x + " y " + y + " theta " + theta);
		
		boolean interrupted = false;
		double alpha = 0;
		
		alpha +=  Math.toDegrees( Math.atan2( Math.abs(y-yRobot), Math.abs(x-xRobot) )) - thetaRobot;
		
		alpha = alpha % 360;
		
		System.out.println("alpha "+ alpha);
		interrupted = robotTurn(alpha, 12);
		
		interrupted = robotDrive(Math.sqrt( ((y-yRobot)*(y-yRobot)) + ((x-xRobot)*(x-xRobot)) ), 15);
		interrupted = robotTurn(theta-thetaRobot, 12);
		printPose();
		
		return interrupted;
	}
	
	public static boolean goTo(double x, double y) {
		System.out.println("Going to x " + x + " y " + y);
		
		boolean interrupted = false;
		double alpha = 0;
		
		alpha +=  Math.toDegrees( Math.atan2( Math.abs(y-yRobot), Math.abs(x-xRobot) )) - thetaRobot;
			
		alpha = alpha % 360;
		
		System.out.println("alpha "+ alpha);
		interrupted = robotTurn(alpha, 12);
		
		interrupted = robotDrive(Math.sqrt( ((y-yRobot)*(y-yRobot)) + ((x-xRobot)*(x-xRobot)) ), 15);
		printPose();
		
		return interrupted;
	}
	
	
	public static void setBar(byte value) {
		comReadWrite(new byte[] { 'o', value, '\r', '\n' });
	}

	public static void comWrite(byte[] data) {
		if (com.isConnected()) {
			com.write(data);
		} else {
			System.out.println("not connected\n");
		}
	}

	public static String comRead() {
		String s = "";
		int i = 0;
		int n = 0;
		while (i < 3 || n > 0) {
			byte[] buffer = new byte[256];
			n = com.read(buffer);
			s += new String(buffer, 0, n);
			i++;
		}
		return s;
	}

	public static String comReadWrite(byte[] data) {
		com.write(data);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		return comRead();
	}

	
	public static boolean readSensors() { 
		
		com.write(new byte[] { 'q', '\r', '\n' });
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		String s = comRead();
		StringTokenizer st = new StringTokenizer(s); 
		String tmp;
		int sensorCount = 0;
		while(st.hasMoreTokens())
		{
				tmp = st.nextToken();
				if(tmp.startsWith("0x")){
					sensors[sensorCount] = Integer.parseInt(tmp.substring(2, tmp.length()), 16);
					sensorCount++;
				}
		}
		
		return true;
	}

	public static void printPose() {
		System.out.printf("xRobot = %.1f yRobot = %.1f thetaRobot = %.1f ", 
				RobotMovement.xRobot, RobotMovement.yRobot, RobotMovement.thetaRobot);
		System.out.flush();
	}
}