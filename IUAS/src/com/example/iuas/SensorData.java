package com.example.iuas;



public class SensorData{
	
	
	 private final byte[] SENSOR_OFFSETS = { 1, 1, 1 }; // offsets of the individual sensors
	 

	 

	public String retrieveSensorData() {
		/*
		* From the bachelor thesis:
		*
		* "Sensor data can be retrieved via the ’q’ command. [...] The
		* returning string contains measurements of all sensors formated as
		* space separated hex values, each prepended with 0x."
		*/
		return RobotControl.comWrite(new byte[] { 'q', '\r', '\n' });
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

}