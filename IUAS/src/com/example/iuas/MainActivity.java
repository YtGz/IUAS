package com.example.iuas;

import java.util.ArrayList;

import jp.ksksue.driver.serial.FTDriver;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private FTDriver com;
	private TextView textLog;
	private EditText programId;
	private final double K = .7;	//offset correction for forward movement
	private final int L = 1;	//offset correction for turning angle
	private final byte[] SENSOR_OFFSETS = {1, 1, 1, 1, 1, 1};	//offsets of the individual sensors

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textLog = (TextView) findViewById(R.id.textLog);
		programId = (EditText) findViewById(R.id.programId);
		com = new FTDriver((UsbManager) getSystemService(USB_SERVICE));
		connect();
	}

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
		comReadWrite(new byte[] { 'u', red, blue, '\r', '\n' });
	}

	public void robotDrive(int distance_cm) {
		distance_cm = (int) Math.ceil(distance_cm * K);
		comReadWrite(new byte[] { 'k', (byte) (distance_cm), '\r', '\n' });
	}

	public void robotTurn(int degree) {
		//degree *= L;
		
		// case 1: Turn right
		if (degree > 255 && degree < 360) {
			degree -= 104;
		}
		
		// case 2: Turn left 2 times
		else if (degree > 127 && degree < 255) {
			degree -= 127;
			comReadWrite(new byte[] { 'l', (byte) (127), '\r', '\n' });
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			comReadWrite(new byte[] { 'l', (byte) (degree), '\r', '\n' });
		}
		
		// case 3: Turn left 3 times
		else if (degree == 255) {
			comReadWrite(new byte[] { 'l', (byte) (127), '\r', '\n' });
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			comReadWrite(new byte[] { 'l', (byte) (127), '\r', '\n' });
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			comReadWrite(new byte[] { 'l', (byte) (1), '\r', '\n' });
		}
		
		// case 4: Turn left
		else {
			comReadWrite(new byte[] { 'l', (byte) (degree), '\r', '\n' });
		}
	}

	public void robotSetVelocity(byte left, byte right) {
		comReadWrite(new byte[] { 'i', left, right, '\r', '\n' });
	}

	public void robotSetBar(byte value) {
		comReadWrite(new byte[] { 'o', value, '\r', '\n' });
	}

	public void robotMoveForward() {
		comReadWrite(new byte[] { 'w', '\r', '\n' });
	}

	public void robotStop() {
		comReadWrite(new byte[] { 's', '\r', '\n' });
	}

	/**
	 * The square test is used to determine accuracy of odometry
	 * 
	 * @param size
	 *            the side length of the square in cm
	 */
	public void squareTest(int size) {
		for (int i = 0; i < 4; i++) {
			robotDrive(size);
			try {
				Thread.sleep(size*500);
			} catch (InterruptedException e) {
				// TODO Automatisch generierter Erfassungsblock
				e.printStackTrace();
			}
			robotTurn((byte) (90));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Automatisch generierter Erfassungsblock
				e.printStackTrace();
			}
		}
	}

	/*
	 * The "Square Test"-button issues a square test with size 20 cm.
	 */
	public void squareTestOnClick(View view) {
		//bugStop();
		squareTest((byte) (20));
	}

	/**
	 * If this is not working then have a closer look to:
	 * http://www.iasj.net/iasj?func=fulltext&aId=94193, page 48
	 * 
	 * @param a
	 *            The length of an arch of the lemniscate.
	 */
	public void lemniscateTest(byte a) {
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
			byte phi = (byte) Math.atan2((newY - oldY), (newX - oldX));
			newX = r * Math.cos(phi) + oldX;
        	newY = r * Math.sin(phi) + oldY;
			System.out.println("forward movement: " + r + "cm   turn angle: " + phi + "°");
			robotTurn(phi);
			robotDrive(r);
			oldX = newX;
			oldY = newY;
		}
	}
	
	/*
	 * The "Lemniscate Test"-button issues a lemniscate test with lemniscate arch length of 20 cm.
	 */
	public void lemniscateTestOnClick(View view) {
		lemniscateTest((byte) 20);
	}

	//public byte[] retrieveSensorData() {
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
	 * Returns true if there is an obstacle roughly ~8cm away from the robot.
	 * 
	 * @return
	 */
	public boolean detectObstacle() {

		/*
		 * From the bachelor thesis:
		 * 
		 * "IR sensors output a voltage signal which correlates to the distance
		 * between the sensor and an object in front of the sensor. Do note that
		 * this correlation is not linear (section 3.6). The provided voltage
		 * output is read via an analog to digital converter (ADC) and is then
		 * available as digital value which can be passed to the Android
		 * application."
		 * 
		 * and
		 * 
		 * "The IR sensors are used to detect obstacles. This sensor measures
		 * the distance using a positive sensitive detector (PSD) and an
		 * infrared emitting diode (RED). According to the datasheet [12] its
		 * measuring distance ranges from 100 mm to 800 mm . Using this infrared
		 * sensor it is easy to measure distances fast and accurate. It is
		 * supplied with 5 V and provides analog output voltage related to the
		 * measured distance. Figure 9 shows this characteristic. Note that the
		 * relation between distance and sensor output is not linear. The output
		 * of the sensors are read by ADCs on the control board."
		 * 
		 * Figure 9 suggests that to check for objects ~8cm away we have to
		 * measure if the voltage is higher than 2 or 3 Volt, depending on how
		 * big the threshold for the distance shall be.
		 */
		boolean encounteredAnObstacle = false;
		String sensorData = retrieveSensorData();
		ArrayList<Float> volts = parseDataString(sensorData);
		for (float v : volts) {
			if (v > 2.5) {
				encounteredAnObstacle = true;
			}
		}
		return encounteredAnObstacle;
	}

	public ArrayList<Float> parseDataString(String dataSring) {
		String[] tokens = dataSring.trim().split("\\s++");
		ArrayList<Float> values = new ArrayList<Float>();
		for (String t : tokens) {
			values.add(Float.parseFloat(t));
		}
		return values;
	}

	// Just for the case simultaneous driving and measuring don't work.
	/**
	 * Robot stops after once every cm to see if there are obstacles.
	 * 
	 * @param distance_cm
	 */
	/*
	 * public void robotDriveCareful(byte distance_cm) { while (distance_cm > 1)
	 * { comReadWrite(new byte[] { 'k', 1, '\r', '\n' }); distance_cm--;
	 * if(detectObstacle()) { return; //Stop when detecting an obstacle }
	 * comReadWrite(new byte[] { 'k', distance_cm, '\r', '\n' }); }
	 */
	
	public void connectOnClick(View view) {
		connect();
	}
	
	public void disconnectOnClick(View view) {
		disconnect();
	}
	
	public void runOnClick(View view) {
		switch (Integer.parseInt(programId.getText().toString())) {
			case 0:
				textLog.append("0");
				squareTest((byte) 20);
				break;
			case 1:
				//textLog.append("1");
				viewSensorOutput();			//To (1) calibrate the sensors and (2) see if data is byte array or String and if it is in cm or V
				break;
			case 2:
				//lemniscateTest((byte) 20);
				break;
			case 3:
				//navigateIgnoringObstacles((byte) 4 , (byte) 5, (byte) 0);
				break;
			case 4:
				//navigate((byte) 4 , (byte) 5, (byte) 0);
				break;
			default:
				//textLog.append("The subroutine " + programId.getText().toString() + "does not exist");
				//System.out.println("The subroutine " + programId.getText().toString() + "does not exist");
				//textLog.append(programId.getText().toString());
				robotDrive((int) (Integer.parseInt(programId.getText().toString())));			//To calibrate the forward movement (calculate k)
				//robotTurn((byte)Integer.parseInt(programId.getText().toString()));			//To calibrate the turning angle
		}
	}
	
	public void viewSensorOutput() {
		//System.out.println(retrieveSensorData());		//if byte[] and not String
		String text = retrieveSensorData();
		textLog.append(text);
		System.out.println(text);
	}
	
	//Robot heads straight for the goal, and in the end rotates according to theta
	public void navigateIgnoringObstacles(byte x, byte y, byte theta) {
		byte r = (byte) Math.sqrt(x * x + y * y);
		byte phi = (byte) Math.atan2(y, x);
		robotTurn(phi);
		robotDrive(r);
		robotTurn((byte) (theta - phi));
	}
	
	//Includes obstacle detection
	public void navigate(byte x, byte y, byte theta) {
		byte r = (byte) Math.sqrt(x * x + y * y);
		byte phi = (byte) Math.atan2(y, x);
		robotTurn(phi);
		for(; r > 0; r--) {
			robotDrive((byte) 1);
			/*byte[] t = bugZero(r, phi);
			r = t[0];
			phi = t[1];*/
		}
		robotTurn((byte) (theta - phi));
	}

	/*****************************************************************************************************************************************
	 * Bug algorithms. *
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
	/*public byte[] bugZero(byte r, byte phi) {
		byte[] sensors = retrieveSensorData();
		byte r2 = r;
		byte phi2 = phi;
		for(int i = 0; i < 6; i++) {
			byte d = (byte) (sensors[i] + SENSOR_OFFSETS[i]);
			if(d > 10 && d < 80) {
				if(i == 2 || i == 3) {	//front sensors
					//bug alg
					do {
						robotTurn((byte) 1);
						phi2 += 1;
						sensors = retrieveSensorData();
					} while(sensors[i] < 10 || sensors[i] > 80);	//Turn until 90 degree to obstacle wall
					do {
						robotDrive((byte) 1);
						r2 += 1;
						sensors = retrieveSensorData();
					} while(sensors[5] < 10 || sensors[5] > 80);	//check if right (left) sensor still pointing to obstacle wall
					
					//recalculate line to goal
					double x = r * Math.cos(phi);
					double y = r * Math.sin(phi);
					double x2 = r2 * Math.cos(phi2);
					double y2 = r2 * Math.sin(phi2);
					
					r = (byte) Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
					phi = (byte) Math.atan2((y2 - y), (x2 - x));
					
					robotTurn(phi);		//let robot face the goal again
					
					byte[] returnVal = {r, phi};
					return returnVal;
				}
			}
		}
		byte[] returnVal = {r, phi};
		return returnVal;
	}*/

	public void bugOne() {
		// TODO
	}

	public void bugTwo() {
		// TODO
	}
}
