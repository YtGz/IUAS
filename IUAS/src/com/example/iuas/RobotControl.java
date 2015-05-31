package com.example.iuas;

public class RobotControl implements Runnable {
	
	private static String command;
	private static int[] values;
	public static Thread robotControlThread;
	
	public static void control(String command, int... values) {
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
			
		}
		else if(command.equalsIgnoreCase("flashLeds")) {
			
		}
	}
}
