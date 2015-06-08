package com.example.iuas;

public class Calibration implements Runnable {
	
	private static String command;
	private static float[] values;
	public static Thread calibrationThread;
	
	public static void calibrate(String command, float... values) {
		if(calibrationThread == null || !calibrationThread.isAlive()) {
			Calibration.command = command;
			Calibration.values = values;
			calibrationThread = new Thread(new Calibration());
			Utils.showLog("Thread started");
			calibrationThread.start();
		}
	}

	@Override
	public void run() {
		if(command.equalsIgnoreCase("turn")) {
			calibrateTurn((int)values[0], values[1]);
		}
		else if(command.equalsIgnoreCase("drive")) {
			calibrateDrive((int)values[0], values[1]);
		}
	}
	
	
	public static void calibrateDrive(int dist, float calib) {
		RobotControl.velocityOffset = calib;
		RobotControl.control("drive", dist);
	}

	public static void calibrateTurn(int deg, float calib) {
		RobotControl.velocityTurnCalibration = calib;
		RobotControl.control("turn", deg);
	}
	

}
