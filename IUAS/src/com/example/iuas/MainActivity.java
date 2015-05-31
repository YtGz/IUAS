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

import com.example.iuas.BallAndBeaconDetection.COLOR;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	/**************************************************************************************************************************************
	 * Define constants *
	 **************************************************************************************************************************************/	

	public final static boolean DEBUG = true; // enables debug messages
	public final static int DEBUG_DEVICE = 1; // 1: sysout, 2: textLog.append
	public final static int USE_DEVICE = 2; // 1: USB, 2: Bluetooth
	
	protected static TextView textLog;
	private EditText xIn; // x value input
	private EditText yIn; // y value input
	private EditText phiIn; // phi value input
	private Spinner mySpinner;
	
	
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
		xIn = (EditText) findViewById(R.id.x);
		yIn = (EditText) findViewById(R.id.y);
		phiIn = (EditText) findViewById(R.id.phi);
		mySpinner = (Spinner) findViewById(R.id.chooseColor);
		mySpinner.setAdapter(new ArrayAdapter<COLOR>(this, android.R.layout.simple_spinner_item, COLOR.values()));
		mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { 
		    	BallAndBeaconDetection.setBALL_COLOR((COLOR) mySpinner.getItemAtPosition(i));
		    } 

		    public void onNothingSelected(AdapterView<?> adapterView) {
		        return;
		    } 
		}); 
		connect();
	}
	
	public void connect() {
		if (USE_DEVICE == 1) {
			RobotControl.com = new FTDriver((UsbManager) getSystemService(USB_SERVICE));
			connectUSB();
		}
		
		else if (USE_DEVICE == 2) {
			if(RobotControl.btc == null)
				connectBT();
		}
	}
	
	/**
	 * Connects USB device.
	 */
	private void connectUSB() {
		if (RobotControl.com.begin(FTDriver.BAUD9600)) {
			System.out.println("Connected");
		} else {
			System.out.println("Not connected");
		}
	}
	
	/**
	 * Connects Bluetooth device.
	 */
	private void connectBT() {
		RobotControl.btc = new BluetoothConnection();
		RobotControl.btc.setDeviceAddress("20:13:08:16:08:95");
		RobotControl.btc.connect();
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
	public static void showLog(Object text) {
		if (DEBUG) {
			if (DEBUG_DEVICE == 1) {
				System.out.println(text);
			}
			else if (DEBUG_DEVICE == 2) {
				textLog.append(text + "\n");
			}
		}
	}

	/***************************************************************************************************************************************************
	 * UI methods *
	 ***************************************************************************************************************************************************/
	
	/**
	 * Starts ball catching activity on button click.
	 * @param view
	 */
	public void cameraFrameProcessingOnClick(View view) {
		Intent intent = new Intent(this, CameraFrameProcessingActivity.class);
		startActivity(intent);
	}
	
	/**
	 * Starts homography activity on button click.
	 * @param view
	 */
	public void homographyOnClick(View view) {
		Intent intent = new Intent(this, HomographyActivity.class);
		startActivity(intent);
	}
	
	/**
	 * Starts various actions in Main Activity by clicking "Run" Button.
	 * 
	 * @param view
	 */
	public void runOnClick(View view) {
		//showLog("Nothing defined for Run-Button yet");
		//squareTest(30);
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
		default:
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
		//robotFlashLed(0);
		//robotDrive(Integer.parseInt(xIn.getText().toString()));
		//robotFlashLed(0);
	}
}
