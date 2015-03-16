package com.example.iuas;

import java.util.ArrayList;

import jp.ksksue.driver.serial.FTDriver;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private FTDriver com;
	private TextView textLog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textLog = (TextView) findViewById(R.id.textLog);
		com = new FTDriver((UsbManager) getSystemService(USB_SERVICE));
		connect();
	}

	public void connect() {
		if (com.begin(FTDriver.BAUD9600)) {
			textLog.append("Connected\n");
		} else {
			textLog.append("Not connected\n");
		}
	}

	public void disconnect() {
		com.end();
		if (!com.isConnected()) {
			textLog.append("disconnected\n");
		}
	}

	public void comWrite(byte[] data) {
		if (com.isConnected()) {
			com.write(data);
		} else {
			textLog.append("not connected\n");
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

	public void robotDrive(byte distance_cm) {
		comReadWrite(new byte[] { 'k', distance_cm, '\r', '\n' });
	}

	public void robotTurn(byte degree) {
		comReadWrite(new byte[] { 'l', degree, '\r', '\n' });
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
	public void squareTest(byte size) {
		for (int i = 0; i < 4; i++) {
			robotDrive(size);
			robotTurn((byte) 90);
		}
	}

	/*
	 * The "Square Test"-button issues a square test with size 20 cm.
	 */
	public void squareTestOnClick(View view) {
		bugStop();
		squareTest((byte) 20);
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

	/*****************************************************************************************************************************************
	 * Bug algorithms. *
	 *****************************************************************************************************************************************/
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

	public void bugZero() {
		// TODO
	}

	public void bugOne() {
		// TODO
	}

	public void bugTwo() {
		// TODO
	}
}
