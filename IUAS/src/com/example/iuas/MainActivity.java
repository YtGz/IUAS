/**
 * This class is the Main Activity for the android app.
 * It contains all basic robot commands, methods for obstacle avoidance, exercises 2, 3 and
 * it connects it with the BallCatching / COlorBlogDetection Activities.
 * 
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.core.Scalar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	/**************************************************************************************************************************************
	 * Define constants *
	 **************************************************************************************************************************************/	

	protected final boolean DEBUG = true; // enables debug messages
	protected final int DEBUG_DEVICE = 1; // 1: sysout, 2: textLog.append
	protected final int USE_DEVICE = 2; // 1: USB, 2: Bluetooth
	
	protected FTDriver com;
	protected TextView textLog;
	private EditText xIn; // x value input
	private EditText yIn; // y value input
	private EditText phiIn; // phi value input
	
	protected final double K = 1.4; // offset correction for forward movement
	private final double K_DETAIL_SENSOR = 1.52; // offset correction for forward movement while measuring for obstacles
	protected final double L = 1.01; // offset correction for turning angle
	private final double L_DETAIL = 1.05; // offset correction for turning angle of 15 degrees
	private final double L_DETAIL_SENSOR = 1.467; // offset correction for turning angle of 15 degree while measuring for obstacles
	
	private final byte[] SENSOR_OFFSETS = { 1, 1, 1 }; // offsets of the individual sensors
	protected final double R_SPEED = 72; // The default turning speed of the robot in cm/s
	protected final double M_SPEED = 14.2; // The default velocity of the robot in cm/s
	private final int WHEEL_SPACING = 19; // space between the 2 wheels
	private final int DELTA_M = 10; // The distance to travel until robot measures for obstacle
	protected final int DELTA_R = 15; // The degrees to rotate until robot measures for obstacle
	private final int O = 10; // How far the robot should drive after the right sensor doesn't see the obstacle's edge anymore
	
	private Scalar mBlobColorHsv; // Needed for ColorBlobDetection
	private Context context;
	private BluetoothConnection btc;
	
	
	
	/**************************************************************************************************************************************
	 * Important methods *
	 **************************************************************************************************************************************/
	
	/**
	 * Start application and connect device.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textLog = (TextView) findViewById(R.id.textLog);
		//programId = (EditText) findViewById(R.id.programId);
		xIn = (EditText) findViewById(R.id.x);
		yIn = (EditText) findViewById(R.id.y);
		phiIn = (EditText) findViewById(R.id.phi);
		
		if (USE_DEVICE == 1) {
			com = new FTDriver((UsbManager) getSystemService(USB_SERVICE));
			connectUSB(); //!! ALSO in BallCatchingActivity & ColorBlobDetection !!
		}
		
		else if (USE_DEVICE == 2) {
			connectBT();
		}
	}
	
	
	
	/**
	 * Starts BallCatching activity.
	 * 
	 * @param savedInstanceState
	 */
	public void onBallCatchingActivityCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	/**
	 * Write debug log on console or mobile phone.
	 */
	public void showLog(Object text) {
		if (DEBUG) {
			if (DEBUG_DEVICE == 1) {
				System.out.println(text);
			}
			else if (DEBUG_DEVICE == 2) {
				textLog.append(text + "\n");
			}
		}
	}
	


	/**************************************************************************************************************************************
	 * Basic robot commands. *
	 **************************************************************************************************************************************/
	
	/**
	 * Connects USB device.
	 */
	public void connectUSB() {
		if (com.begin(FTDriver.BAUD9600)) {
			showLog("Connected\nWillkommen in der Unterwelt!");
		} else {
			showLog("Not connected");
		}
	}
	
	/**
	 * Disconnects USB device.
	 */
	public void disconnect() {
		com.end();
		if (!com.isConnected()) {
			showLog("Disconnected");
		}
	}
	
	/**
	 * Connects Bluetooth device.
	 */
	public void connectBT() {
		btc = new BluetoothConnection(context);
		btc.setDeviceAddress("94:CE:2C:A2:E8:9E");
		btc.connect();
	}
	
	/**
	 * Send write() command to robot.
	 * 
	 * @param data
	 */
	public void comWrite(byte[] data) {
	/*	if (com.isConnected()) {
			com.write(data);
		} else {
			showLog("Not connected!");
		}*/
		btc.write(data);
	}
	
	/**
	 * Send read() command to robot.
	 * 
	 * @return
	 */
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
	
	/**
	 * Send read() and write() command to robot.
	 * Wait 100 ms between write() and read()
	 * 
	 * @param data
	 * @return
	 */
	public String comReadWrite(byte[] data) {
		/*com.write(data);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// ignore
		}
		return comRead();*/
		btc.write(data);
		return "";
	}
	
	/**
	 * Lets robot drive a given distance in cm.
	 * 
	 * @param distance_cm
	 */
	public void robotDrive(int distance_cm){
		robotDrive(distance_cm, K);
	}
	
	/**
	 * Lets robotz drive a given distance in cm.
	 * Also accepts a calibration factor to get the input distance in real world drive.
	 * 
	 * @param distance_cm
	 * @param calib
	 */
	public void robotDrive(int distance_cm, double calib) {
		distance_cm = ((int) Math.ceil(distance_cm * calib)); // calculate distance with calibration factor
		
		// if distance < 0 use absolute value of distance and turn 129 byte as long as distance value is > 127cm and at the end 256 - distance_cm byte
		if (distance_cm < 0) {
			distance_cm = Math.abs(distance_cm);
			while (distance_cm > 127) {
				comWrite(new byte[] { 'k', (byte) (129), '\r', '\n' });
				distance_cm -= 127;
				try {
					Thread.sleep((long) Math.ceil(127 * 1000 / calib / M_SPEED));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			comWrite(new byte[] { 'k', (byte) (256 - distance_cm), '\r', '\n' });
			try {
				Thread.sleep((long) Math.ceil(distance_cm * 1000 / calib / M_SPEED));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// if distance > 0 turn 127 byte as long as distance value > 127cm and at the end the given distance_cm byte
		else {
			while (distance_cm > 127) {
				comWrite(new byte[] { 'k', (byte) (127), '\r', '\n' });
				distance_cm -= 127;
				try {
					Thread.sleep((long) Math.ceil(127 * 1000 / calib / M_SPEED));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (distance_cm == 10) { // 10 does nothing, therefore set it to 11 if given
				distance_cm = 11;
			}
			comWrite(new byte[] { 'k', (byte) (distance_cm), '\r', '\n' });
			try {
				Thread.sleep((long) Math.ceil(distance_cm * 1000 / calib / M_SPEED));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Lets robot turn a given amount of degrees.
	 * 
	 * @param degree
	 */
	public void robotTurn(int degree) {
		for (int i = Math.abs(degree); i >= DELTA_R;) {
			i -= DELTA_R;
			if(degree < 0){
			degree = -i; 
			robotTurn(-DELTA_R, L);
			}
			else {
				degree = i;
				robotTurn(DELTA_R, L);
			}
		}
		robotTurn(degree, L);
	}
	
	/**
	 * Lets robot turn a given amount of degrees.
	 * Also accepts a calibration factor to get the input degrees in real world turning.
	 * 
	 * @param degree
	 * @param calib
	 */
	public void robotTurn(int degree, double calib) {
		degree = (int) Math.ceil(degree * calib); // calculate degrees with calibration factor
		
		// if degree < 0 use absolute value of degree and turn 129 byte as long as degree value is > 127 and at the end 256 - degree byte
		if (degree < 0) {
			degree = Math.abs(degree);
			while (degree > 127) {
				comWrite(new byte[] { 'l', (byte) (129), '\r', '\n' });
				degree -= 127;
				try {
					Thread.sleep((long) Math.ceil(127 * 1000 / calib / R_SPEED));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			comWrite(new byte[] { 'l', (byte) (256 - degree), '\r', '\n' });
			try {
				Thread.sleep((long) Math.ceil(degree * 1000 / calib / R_SPEED));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// if degree > 0 turn 127 byte as long as degree > 127
		else {
			while (degree > 127) {
				comWrite(new byte[] { 'l', (byte) (127), '\r', '\n' });
				degree -= 127;
				try {
					Thread.sleep((long) Math.ceil(127 * 1000 / calib / R_SPEED));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (degree == 10) { // 10 does nothing, therefore set it to 11 if given
				degree = 11;
			}
			if(degree > 0) { // if left amount of degrees is > 0 turn robot the outstanding degree value
				comWrite(new byte[] { 'l', (byte) (degree), '\r', '\n' });
			}
			try {
				Thread.sleep((long) Math.ceil(degree * 1000 / calib / R_SPEED));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
	 * Method to let robot move forward.
	 */
	public void robotMoveForward() {
		comWrite(new byte[] { 'w', '\r', '\n' });
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
	
	
	/***************************************************************************************************************************************************
	 * Bluetooth connection
	 ***************************************************************************************************************************************************/
	
	/* Will be implemented soon if it is not too complicated.
	 * 
	 * see SampleGatt Classes in package and also
	 * http://developer.android.com/samples/BluetoothLeGatt/src/com.example.android.bluetoothlegatt/DeviceControlActivity.html#l305
	 * http://developer.android.com/guide/topics/connectivity/bluetooth-le.html
	 */
	
	

	/*************************************************************************************************************************************************
	 * Exercise 2 *
	 *************************************************************************************************************************************************/

	/**
	 * The square test is used to determine accuracy of odometry.
	 * 
	 * @param size the side length of the square in cm
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
	 * @param a The length of an arch of the lemniscate.
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
			//showLog(String.valueOf("forward movement: " + r + "cm   turn angle: " + phi + "degree"));
			robotTurn(phi, L_DETAIL);
			robotDrive(r);
			oldX = newX;
			oldY = newY;
		}
	}

	/**
	 * Not very accurate, but works. Robot drives two circles, not a real lemniscate.
	 * 
	 * @param a The length of an arch of the lemniscate.
	 */
	public void lemniscateTestVer2(int a) {
		double r = (WHEEL_SPACING + a) / 2; // the radius of the circle of the outer wheel
		double s = 2 * Math.PI * (r - WHEEL_SPACING / 2);
		double s1 = 2 * Math.PI * (r - WHEEL_SPACING);
		double s2 = 2 * Math.PI * r;
		int v = 14; // how fast the robot should drive
		double t = s / v;
		byte v1 = (byte) Math.round(s1 / t);
		byte v2 = (byte) Math.round(s2 / t);
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
	 * Exercise 3 *
	 ********************************************************************************************************************************************************/

	public String retrieveSensorData() {

		/*
		 * From the bachelor thesis:
		 * 
		 * "Sensor data can be retrieved via the ’q’ command. [...] The
		 * returning string contains measurements of all sensors formated as
		 * space separated hex values, each prepended with 0x."
		 */

		return comReadWrite(new byte[] { 'q', '\r', '\n' });
	}
	
	/**
	 * Parses string with sensor data.
	 * 
	 * @param dataString
	 * @return
	 */
	public int[] parseDataString(String dataString) {
		int[] values = new int[3];
		String[] tokens = dataString.trim().split("\\s++");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("0x")) {
				for (int j = i; j < tokens.length; j++) {
					tokens[j] = tokens[j].substring(2);
				}
				values[0] = (int) Integer.valueOf(tokens[i + 3], 16);
				values[1] = (int) Integer.valueOf(tokens[i + 4], 16);
				values[2] = (int) Integer.valueOf(tokens[i + 5], 16);
				return values;
			}
		}
		return values;
	}
	
	/**
	 * Displays values of the sensors.
	 */
	public void viewSensorOutput() {
		/*int[] temp = parseDataString(retrieveSensorData());
		showLog(String.valueOf(temp[0]) + " " + String.valueOf(temp[1]) + " " + String.valueOf(temp[2]));*/
	}

	/**
	 * Returns true if there is an obstacle roughly ~8cm away from the robot.
	 * 
	 * @param which sensors to use
	 * 
	 * @return true if obstacle was detected, false else
	 */
	public boolean detectObstacle(boolean[] sensors) {
		return detectObstacle(sensors, new int[] { 10, 50 });
	}
	
	/**
	 * Extended method which checks if sensors detect obstacles.
	 * 
	 * @param sensors
	 * @param range
	 * @return true if obstacle is detected, false else
	 */
	public boolean detectObstacle(boolean[] sensors, int[] range) {
		boolean encounteredAnObstacle = false;
		String sensorData = retrieveSensorData(); // get current sensor data
		while (!sensorData.contains("0x")) { // do nothing as long as sensor data does not start (because retrieved string contains other values too; start only if 0x is found which means, that sensor data starts now)
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			sensorData = retrieveSensorData();
		}
		int[] dst = parseDataString(sensorData); // parse sensor data
		for (int i = 0; i < 3; i++) {
			if (sensors[i]) {
				if (dst[i] >= range[0] && dst[i] <= range[1]) { // obstacle is in range & encountered
					encounteredAnObstacle = true;
				}
			}
		}
		return encounteredAnObstacle;
	}
	
	/**
	 * Test method for detecting obstacles.
	 * @param distance
	 */
	public void stopAndGo(int distance) {
		while (distance > DELTA_M) {
			robotDrive(DELTA_M);
			distance -= DELTA_M;
			if (detectObstacle(new boolean[] { true, true, true })) {
				robotFlashLed(0);
				return;
			}
		}
		robotDrive(distance);
	}

	/** 
	 * Robot heads straight for the goal, ignoring obstacles, and in the end rotates according to theta.
	 * 
	 * @param x
	 * @param y
	 * @param theta
	 */
	public void navigateIgnoringObstacles(int x, int y, int theta) {
		int r = (int) Math.sqrt(x * x + y * y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(y, x));
		//showLog(phi);
		phi *= -1;
		robotTurn(phi);
		robotDrive(r);
		robotTurn(theta - phi);
	}

	/**
	 * Robot heads for the goal, also measuring for obstacles and rotates in the end according to theta.
	 * 
	 * @param x
	 * @param y
	 * @param theta
	 */
	public void navigate(int x, int y, int theta) {
		int r = (int) Math.sqrt(x * x + y * y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(y, x));
		phi *= -1;
		robotTurn(phi, L_DETAIL_SENSOR);
		
		detectObstacle(new boolean[] { false, false, true }); //temporary
		while (r > 0) {
			int[] t = bugZero(r, phi);
			r = t[0];
			phi = t[1];
			if (!detectObstacle(new boolean[] { true, true, true })) {
				robotDrive(DELTA_M, K_DETAIL_SENSOR);
				r -= DELTA_M;
			}
		}
		robotTurn(theta - phi);
	}

	
	
	/*****************************************************************************************************************************************
	 * Bug algorithms. *
	 *****************************************************************************************************************************************/
	
	/**
	 * Permanently sleep 100ms, check for obstacles and then stop.
	 * Not sure if this works.
	 */
	public void bugStop() {
		for (;;) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
			if (detectObstacle(new boolean[] { true, true, true })) {
				robotStop();
			}
		}
	}

	/**
	 * Bug 0 algorithm
	 * 
	 * @param r takes the remaining line to the goal as argument
	 * @param phi
	 * @return the new angle & distance to the obstacle
	 */
	public int[] bugZero(int r, int phi) {
		int r2 = 0;
		int phi2 = 0;
		if (detectObstacle(new boolean[] { true, true, true })) { // obstacle detected
			robotFlashLed(0);
			do {
				robotTurn(DELTA_R, L_DETAIL_SENSOR); // turn away to avoid it
				phi2 += DELTA_R;
			} while (!detectObstacle(new boolean[] { true, true, false },
					new int[] { 100, 255 })); // Turn until 90 degree to obstacle wall
			do {
				robotDrive(DELTA_M, K_DETAIL_SENSOR);
				r2 += DELTA_M;
			} while (detectObstacle(new boolean[] { false, false, true })); // check if right (left) sensor still pointing to obstacle wall

			for (int i = 0; i < O; i += DELTA_M) { // Drive past the corner
				robotDrive(DELTA_M, K_DETAIL_SENSOR);
				r2 += DELTA_M;
				detectObstacle(new boolean[] { false, false, true }); //temporary
			}
			
			// recalculate line to goal
			double x = r * Math.cos(Math.toRadians(90));
			double y = r * Math.sin(Math.toRadians(90));
			double x2 = r2 * Math.cos(Math.toRadians(90 + phi2));
			double y2 = r2 * Math.sin(Math.toRadians(90 + phi2));

			r = (int) Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
			showLog("BugZero: " + Math.toDegrees(Math.atan2((y - y2), (x - x2))));
			showLog("BugZero: phi2: " + phi2);
			int psi = (int) Math.ceil(Math.toDegrees(Math.toRadians(90) - Math.atan2((y - y2), (x - x2)))) + phi2;
			psi *= -1;
			showLog("BugZero: Psi: "+ psi);
			robotTurn(psi,L_DETAIL_SENSOR); // let robot face the goal again
			detectObstacle(new boolean[] { false, false, true }); //temporary
			phi += phi2 + psi;
		}
		int[] returnVal = { r, phi };
		return returnVal;
	}
	
	/**
	 * A test method to test rotation and caluclation if obstacle was detected and robot has to re-calculate his way to the goal.
	 * 
	 * @param r
	 * @return
	 */
	public int rotTest(int r) {
		int r2 = 0;
		int phi = 0;
		for (int i = 1; i <= 90; i += DELTA_R) {
			robotTurn(DELTA_R, L_DETAIL);
			phi += DELTA_R;
		}

		for (int i = 0; i < O; i += DELTA_M) { // Drive past the corner
			robotDrive(DELTA_M);
			r2 += DELTA_M;
		}

		// recalculate line to goal
		double x = r * Math.cos(Math.toRadians(90));
		double y = r * Math.sin(Math.toRadians(90));
		double x2 = r2 * Math.cos(Math.toRadians(90 + phi));
		double y2 = r2 * Math.sin(Math.toRadians(90 + phi));

		// showLog(String.valueOf("x: " + x + "y: " + y + "x2: " + x2 + "y2: " + y2 + "\n"));

		r = (int) Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
		showLog("RotTest: " + Math.toDegrees(Math.atan2((y - y2), (x - x2))));
		phi = (int) Math.ceil(Math.toDegrees(Math.toRadians(90) - Math.atan2((y - y2), (x - x2)))) + phi;
		phi *= -1;

		robotTurn(phi); // let robot face the goal again
		int returnVal = r;
		return returnVal;
	}
	
	

	/***************************************************************************************************************************************************
	 * Calibration *
	 ***************************************************************************************************************************************************/
	
	/**
	 * A method to find out the right calibration value for turning angle of the robot.
	 * 
	 * @param calib
	 */
	public void calibrateLDetail(int calib) {
		double c = 1 + calib / 100.0;
		for (int i = 1; i <= 360; i += DELTA_R) {
			robotTurn(DELTA_R, c);
			//detectObstacle(new boolean[] { true, true, false }, new int[] { 100, 255 });
		}
	}
	
	/**
	 * A method to find out the right calibration value for moving forward a given distance.
	 * 
	 * @param calib
	 */
	public void calibrateDistance(int calib){
		double c = 1 + calib /100.0;
		for(int i =1 ; i <= 60; i += DELTA_M){
			robotDrive(DELTA_M, c);
			//detectObstacle(new boolean[] { true, true, false }, new int[] { 100, 255 });
		}
	}
	
	

	/***************************************************************************************************************************************************
	 * UI methods *
	 ***************************************************************************************************************************************************/
	
	/**
	 * Allows to switch Activities within the app.
	 * 
	 * @param view
	 */
	public void switchActivity(View view) {
		Intent intent = new Intent(this, ColorBlobDetectionActivity.class);
		final int result=1;
		startActivityForResult(intent, result);
	}
	
	/**
	 * Sets the mBlobColorHsv on Activity result.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		double[] color = data.getDoubleArrayExtra("color");
		for (double c : color) {
			System.out.println(c);
		}
		mBlobColorHsv = new Scalar(color);
	}
	
	/**
	 * Starts ball catching activity on button click.
	 * @param view
	 */
	public void ballCatchingOnClick(View view) {
		Intent intent = new Intent(this, BallCatchingActivity.class);
		intent.putExtra("mBlobColorHsv", mBlobColorHsv.val);
		startActivity(intent);
	}
	
	/**
	 * Disconnects USB device.
	 * 
	 * @param view
	 */
	public void disconnectOnClick(View view) {
		disconnect();
	}
	
	/**
	 * Starts various actions in Main Activity by clicking "Run" Button.
	 * 
	 * @param view
	 */
	public void runOnClick(View view) {
		showLog("Nothing defined for Run-Button yet");
		/*switch (Integer.parseInt(xIn.getText().toString())) {
		case 0:
			calibrateLDetail(100);
			navigate(0, 120, 0);
			rotTest(60);
			break;
		case 1:
			textLog.setText(retrieveSensorData());
			 viewSensorOutput(); //To (1) calibrate the sensors and (2) see if
			 //data is byte array or String and if it is in cm or V
			break;
		case 2:
			lemniscateTestVer2(50);
			break;
		case 3:
			robotFlashLed(0);
			navigateIgnoringObstacles((byte) 4 , (byte) 5, (byte) 0);
			break;
		case 4:
			 navigate((byte) 4 , (byte) 5, (byte) 0);
			 break;
		defult:
			 robotDrive(Integer.parseInt(programId.getText().toString()));
			 //To calibrate the forward movement (calculate k)
			 robotTurn(Integer.parseInt(programId.getText().toString())); //To
			 calibrate the turning angle
			 stopAndGo(Integer.parseInt(programId.getText().toString()));
			 calibrateLDetail(Integer.parseInt(xIn.getText().toString()));
			 calibrateDistance(Integer.parseInt(xIn.getText().toString()));
			 rotTest(Integer.parseInt(programId.getText().toString()));
		}
		navigateIgnoringObstacles(Integer.parseInt(xIn.getText().toString()), Integer.parseInt(yIn.getText().toString()), Integer.parseInt(phiIn.getText().toString()));
		robotTurn(-180);
		robotDrive(106);*/
	}
}
