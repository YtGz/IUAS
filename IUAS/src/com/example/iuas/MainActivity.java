package com.example.iuas;

import java.util.ArrayList;

import jp.ksksue.driver.serial.FTDriver;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private FTDriver com;
	private TextView textLog;
	private EditText programId;
	private final double K = 1.358;	//offset correction for forward movement
	private final double L = 1.14;	//offset correction for turning angle
	private final byte[] SENSOR_OFFSETS = {1, 1, 1};	//offsets of the individual sensors
	private final int R_TIME = 1500;	//The number of milliseconds to wait for a rotation of max. 127°
	private final double M_SPEED = 14.2;	//The default velocity of the robot in cm/s
	private final int WHEEL_SPACING = 19;
	private final int DELTA_M = 15;		//The distance to travel until robot measures for obstacle
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textLog = (TextView) findViewById(R.id.textLog);
		programId = (EditText) findViewById(R.id.programId);
		com = new FTDriver((UsbManager) getSystemService(USB_SERVICE));
		connect();
	}
	
	
	
	
	
	/**************************************************************************************************************************************
	 * Basic robot commands.																											  *
	 **************************************************************************************************************************************/

	public void connect() {
		if (com.begin(FTDriver.BAUD9600)) {
			textLog.append("Connected\nWillkommen in der Unterwelt!\n");
		} else {
			textLog.append("Not connected\n");
		}
	}

	public void disconnect() {
		com.end();
		if (!com.isConnected()) {
			textLog.append("Disconnected\n");
		}
	}

	public void comWrite(byte[] data) {
		if (com.isConnected()) {
			com.write(data);
		} else {
			textLog.append("Not connected\n");
		}
	}

	public String comRead() {
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

	public String comReadWrite(byte[] data) {
		com.write(data);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		return comRead();
	}

	public void robotSetLeds(byte red, byte blue) {
		comWrite(new byte[] { 'u', red, blue, '\r', '\n' });
	}

	public void robotDrive(int distance_cm) {
		distance_cm = (int) Math.ceil(distance_cm * K);
		
		if(distance_cm < 0) {
			distance_cm = Math.abs(distance_cm);
			while(distance_cm > 127) {
				comWrite(new byte[] { 'k', (byte) (129), '\r', '\n' });
				distance_cm -= 127;						
				try {
					Thread.sleep((long) Math.ceil(127*1000/K/M_SPEED));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			comWrite(new byte[] { 'k', (byte) (256-distance_cm), '\r', '\n' });
			try {
				Thread.sleep((long) Math.ceil(distance_cm*1000/K/M_SPEED));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		else {
			while(distance_cm > 127) {
				comWrite(new byte[] { 'k', (byte) (127), '\r', '\n' });
				distance_cm -= 127;
				try {
					Thread.sleep((long) Math.ceil(127*1000/K/M_SPEED));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (distance_cm == 10) {
				distance_cm = 11;
			}
			comWrite(new byte[] { 'k', (byte) (distance_cm), '\r', '\n' });
			try {
				Thread.sleep((long) Math.ceil(distance_cm*1000/K/M_SPEED));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}

	public void robotTurn(int degree) {
		degree = (int) Math.ceil(degree * L);
		
		if(degree < 0) {
			degree = Math.abs(degree);
			while(degree > 127) {
				comWrite(new byte[] { 'l', (byte) (129), '\r', '\n' });
				degree -= 127;
				try {
					Thread.sleep(R_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			comWrite(new byte[] { 'l', (byte) (256-degree), '\r', '\n' });
			try {
				Thread.sleep(R_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		else {
			while(degree > 127) {
				comWrite(new byte[] { 'l', (byte) (127), '\r', '\n' });
				degree -= 127;
				try {
					Thread.sleep(R_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (degree == 10) {
				degree = 11;
			}
			comWrite(new byte[] { 'l', (byte) (degree), '\r', '\n' });
			try {
				Thread.sleep(R_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void robotSetVelocity(byte left, byte right) {
		comWrite(new byte[] { 'i', left, right, '\r', '\n' });
	}

	public void robotSetBar(byte value) {
		comWrite(new byte[] { 'o', value, '\r', '\n' });
	}

	public void robotMoveForward() {
		comWrite(new byte[] { 'w', '\r', '\n' });
	}

	public void robotStop() {
		comWrite(new byte[] { 's', '\r', '\n' });
	}
	
	
	
	
	/*************************************************************************************************************************************************
	 * Exercise 2																																	 *
	 *************************************************************************************************************************************************/
	
	
	
	/**
	 * The square test is used to determine accuracy of odometry
	 * 
	 * @param size
	 *            the side length of the square in cm
	 */
	public void squareTest(int size) {
		for (int i = 0; i < 4; i++) {
			robotDrive(size);
			robotTurn(90);
		}
	}

	/**
	 * If this is not working then have a closer look to:
	 * http://www.iasj.net/iasj?func=fulltext&aId=94193, page 48
	 * 
	 * @param a
	 *            The length of an arch of the lemniscate.
	 */
	public void lemniscateTest(int a) {
		/*
		 * The smaller this value the more frequently the robot repeats it's
		 * move & turn cycles along the path. A small value leads to more
		 * accuracy but the robot will get slower. Number of move & turn cycles:
		 * 2*pi/resolution.
		 */
		double resolution = .1;

		double oldX = 0;
		double oldY = 0;
		for (double t = 0; t < 2 * Math.PI; t += resolution) {
			double newX = a * Math.sqrt(2) * Math.cos(t) / (Math.sin(t) * Math.sin(t) + 1);
			double newY = a * Math.sqrt(2) * Math.cos(t) * Math.sin(t) / (Math.sin(t) * Math.sin(t) + 1);

			/*
			 * converting to polar coordinates relative to current robot
			 * position
			 */
			byte r = (byte) Math.sqrt((newX - oldX) * (newX - oldX) + (newY - oldY) * (newY - oldY));
			byte phi = (byte) Math.ceil(Math.atan2((newY - oldY), (newX - oldX)));
			newX = r * Math.cos(phi) + oldX;
        	newY = r * Math.sin(phi) + oldY;
			textLog.setText(String.valueOf("forward movement: " + r + "cm   turn angle: " + phi + "°"));
			robotTurn(phi);
			robotDrive(r);
			oldX = newX;
			oldY = newY;
		}
	}

	
	/**
	 * Not very accurate, but works.
	 * Robot drives two circles, not a real lemniscate.
	 * 
	 * @param a  The length of an arch of the lemniscate.
	 */
	public void lemniscateTestVer2(int a) {
		double r = (WHEEL_SPACING + a) / 2;		//the radius of the circle of the outer wheel
		double s = 2 * Math.PI * (r - WHEEL_SPACING/2);
		double s1 = 2 * Math.PI * (r - WHEEL_SPACING);
		double s2 = 2 * Math.PI * r;
		int v = 14;		//how fast the robot should drive
		double t = s/v;
		byte v1 = (byte) Math.round(s1/t);
		byte v2 = (byte) Math.round(s2/t);
		robotSetVelocity(v1, v2);
		try {
			Thread.sleep((long) t * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		robotSetVelocity(v2, v1);
		try {
			Thread.sleep((long) t * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		robotSetVelocity((byte) 0, (byte) 0);
	}
	
	
	
	
	
	/********************************************************************************************************************************************************
	 * Exercise 3																																			*
	 ********************************************************************************************************************************************************/

	
	public String retrieveSensorData() {
		
		/* From the bachelor thesis:
		 * 
		 * "Sensor data can be retrieved via the ’q’ command. [...] The
		 * returning string contains measurements of all sensors formated as
		 * space separated hex values, each prepended with 0x."
		 */

		return comReadWrite(new byte[] { 'q', '\r', '\n' });
	}

	public int[] parseDataString(String dataString) {
		int[] values = new int[3];
		String[] tokens = dataString.trim().split("\\s++");
		for(int i = 0; i < tokens.length; i++){
			if(tokens[i].contains("0x")) {
				for(int j = i; j < tokens.length; j++){
					tokens[j] = tokens[j].substring(2);
				}
				values [0] = (int) Integer.valueOf(tokens[i+3],16);
				values [1] = (int) Integer.valueOf(tokens[i+4],16);
				values [2] = (int) Integer.valueOf(tokens[i+5],16);
				return values;
			}
		}
		return values;
	}

	public void viewSensorOutput() {
		int [] temp = parseDataString(retrieveSensorData());
		textLog.setText(String.valueOf(temp[0])+ " " + String.valueOf(temp[1]) + " " + String.valueOf(temp[2]));
	}
	
	/**
	 * Returns true if there is an obstacle roughly ~8cm away from the robot.
	 * 
	 * @return
	 */
	public boolean detectObstacle() {
		boolean encounteredAnObstacle = false;
		String sensorData = retrieveSensorData();
		if(!sensorData.equalsIgnoreCase("") && sensorData.contains("0x")) {
			int[] dst = parseDataString(sensorData);
			for (int i = 0; i < 3; i++) {
				if (dst[i] > 10 && dst[i] < 30) {
					encounteredAnObstacle = true;
				}
			}
		}
		return encounteredAnObstacle;
	}

	
	public boolean checkAngleToObstacle() {
		boolean retVal = true;
		String sensorData = retrieveSensorData();
		if(!sensorData.equalsIgnoreCase("") && sensorData.contains("0x")) {
			int[] dst = parseDataString(sensorData);
			for (int i = 0; i < 2; i++) {
				if (dst[i] > 10 && dst[i] < 30) {
					retVal = false;
				}
			}
		}
		return retVal;	
	}
	
	public boolean rightSensor() {
		boolean retVal = false;
		String sensorData = retrieveSensorData();
		if(!sensorData.equalsIgnoreCase("") && sensorData.contains("0x")) {
			int[] dst = parseDataString(sensorData);
			if (dst[3] > 10 && dst[3] < 30) {
				retVal = true;
			}
		}
		return retVal;	
	}
	
	public void stopAndGo(int distance) {
		
		while(distance > DELTA_M) {
			robotDrive(DELTA_M);
			distance -= DELTA_M;
			if(detectObstacle()) {
				robotStop();
				for(int i = 0; i < 4; i++) {
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
					robotSetLeds((byte) 0, (byte) 0);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				return;
			}
		}
		robotDrive(distance);
	}
	
	//Robot heads straight for the goal, and in the end rotates according to theta
	public void navigateIgnoringObstacles(int x, int y, int theta) {
		int r = (int) Math.sqrt(x * x + y * y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(y, x));
		phi *= -1;
		robotTurn(phi);
		robotDrive(r);
		robotTurn(theta - phi);
	}
	
	//Includes obstacle detection
	public void navigate(int x, int y, int theta) {
		int r = (int) Math.sqrt(x * x + y * y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(y, x));
		phi *= -1;
		robotTurn(phi);
		while(r > 0) {
			robotDrive(DELTA_M);
			r -= DELTA_M;
			int[] t = bugZero(r, phi);
			r = t[0];
			phi = t[1];
		}
		robotTurn(theta - phi);
	}

	
	
	
	
	
	/*****************************************************************************************************************************************
	 * Bug algorithms. 																														 *
	 *****************************************************************************************************************************************/
	//May not work
	public void bugStop() {
		for (;;) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
			if (detectObstacle()) {
				robotStop();
			}
		}
	}

	/**
	 * Bug 0 algorithm
	 * @param r		takes the remaining line to the goal as argument
	 * @param phi
	 * @return the new angle & distance to the obstacle
	 */
	public int[] bugZero(int r, int phi) {
		int r2 = r;
		int phi2 = phi;
		if(detectObstacle()) {
			for(int i = 0; i < 4; i++) {
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
				robotSetLeds((byte) 0, (byte) 0);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			do {
				robotTurn(DELTA_M);
				phi2 += DELTA_M;
			} while(!checkAngleToObstacle());	//Turn until 90 degree to obstacle wall
			/*do {
				robotDrive(DELTA_M);
				r2 += DELTA_M;
			} while(rightSensor());	//check if right (left) sensor still pointing to obstacle wall
			*/
			
			//recalculate line to goal
			double x = r * Math.cos(Math.toRadians(phi));
			double y = r * Math.sin(Math.toRadians(phi));
			double x2 = r2 * Math.cos(Math.toRadians(phi2));
			double y2 = r2 * Math.sin(Math.toRadians(phi2));
			
			//textLog.append(String.valueOf("x: " + x + "y: " + y + "x2: " + x2 + "y2: " + y2 + "\n"));
			
			r = (int) Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
			phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2((y2 - y), (x2 - x)));
			
			textLog.append(String.valueOf("phi: " + phi));
			robotTurn(phi);		//let robot face the goal again
		}
		int[] returnVal = {r, phi};
		return returnVal;
	}

	public void bugOne() {
		// TODO
	}

	public void bugTwo() {
		// TODO
	}
	
	
	
	
	
	
	/***************************************************************************************************************************************************
	 *	UI methods																																	   *
	 ***************************************************************************************************************************************************/
	
	public void connectOnClick(View view) {
		connect();		
	}
	
	public void disconnectOnClick(View view) {
		disconnect();
	}
	
	public void runOnClick(View view) {
		switch (Integer.parseInt(programId.getText().toString())) {
			case 0:
				navigate(0, 60, 0);
				break;
			case 1:
				textLog.setText(retrieveSensorData());
				//viewSensorOutput();			//To (1) calibrate the sensors and (2) see if data is byte array or String and if it is in cm or V
				break;
			case 2:
				lemniscateTestVer2(50);
				break;
			case 3:
				//navigateIgnoringObstacles((byte) 4 , (byte) 5, (byte) 0);
				break;
			case 4:
				//navigate((byte) 4 , (byte) 5, (byte) 0);
				break;
			default:
				robotDrive(Integer.parseInt(programId.getText().toString()));			//To calibrate the forward movement (calculate k)
				//robotTurn(Integer.parseInt(programId.getText().toString()));			//To calibrate the turning angle
				//stopAndGo(Integer.parseInt(programId.getText().toString()));
			}
	}
}
