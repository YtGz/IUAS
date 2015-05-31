/**
 * This class provides the Bluetooth connection
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

public class BluetoothConnection {

	private static final UUID SERVICE_ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private String deviceAddress;
	private final BluetoothAdapter btAdapter;
	private BluetoothDevice btDevice;
	private BluetoothSocket btSocket;

	public BluetoothConnection(Context context) {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}


	public void setDeviceAddress(String deviceAddress) {
		this.deviceAddress = deviceAddress;
	}
	
	
	public synchronized void connect() {
		System.out.println("Trying to connect to: " + deviceAddress);
		btDevice = btAdapter.getRemoteDevice(deviceAddress);
		try {
			btSocket = btDevice.createRfcommSocketToServiceRecord(SERVICE_ID);

		} catch (IOException e) {
			System.out.println("Socket create() failed");
			e.printStackTrace();
		}
		
		// Always cancel discovery because it will slow down a connection
		btAdapter.cancelDiscovery();
		
		// Make a connection to the BluetoothSocket
		try {
			// This is a blocking call and will only return on a successful connection or an exception
			btSocket.connect();
			System.out.println("BT connected");
		} catch (IOException e) {
			System.out.println("BT Connection failed");
			e.printStackTrace();

			// Close the socket
			try {
				btSocket.close();
			} catch (IOException e2) {
				System.out.println("unable to close() socket during connection failure");
				e2.printStackTrace();
			}
			return;
		}
	}

	public void write(byte[] out) {
		try {
			btSocket.getOutputStream().write(out);
		} catch (IOException e) {
			System.out.println("Exception during write");
			e.printStackTrace();
		}
	}
}