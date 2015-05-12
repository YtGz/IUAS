package com.example.iuas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

public class BluetoothConnection implements IConnection {

	private static final UUID SERVICE_ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothAdapter btAdapter;
	private int state;

	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	private String deviceAddress;

	/**
	 * CONSTRUCTOR: Creates a new instance of an BluetoothConnecion
	 * 
	 * @param context
	 *            The UI Activity context
	 */
	public BluetoothConnection(Context context) {

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		this.state = STATE_NONE;
	}

	/**
	 * Sets the mac address from the remote device of the connection
	 * 
	 * @param device
	 *            the mac address to set
	 */
	public void setDeviceAddress(String deviceAddress) {
		this.deviceAddress = deviceAddress;
	}

	/**
	 * Set the current state of the connection
	 * 
	 * @param state
	 *            An integer representing the new state of the connection
	 */
	private synchronized void setState(int state) {
		this.state = state;
	}

	@Override
	public synchronized int getState() {
		return this.state;
	}
	
	@Override
	public synchronized void connect() {

		System.out.println("Trying to connect to: " + deviceAddress);

		// Cancel any thread attempting to make a connection
		if (state == STATE_CONNECTING) {
			if (connectThread != null) {
				connectThread.cancel();
				connectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Start the thread to connect with the given device
		connectThread = new ConnectThread(deviceAddress);
		connectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 *
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

		System.out.println("connected to " + device.getName());

		// Cancel the thread that completed the connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel any thread currently running a connection
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();

		setState(STATE_CONNECTED);
	}

	@Override
	public synchronized void stop() {
		System.out.println("Buetooth connection stopped.");

		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		setState(STATE_NONE);
	}

	@Override
	public void write(byte[] out) {

		// Create temporary object
		ConnectedThread r;

		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (state != STATE_CONNECTED)
				return;
			r = connectedThread;
		}

		// Perform the write unsynchronized
		r.write(out);
	}

	private class ConnectThread extends Thread {

		private final BluetoothSocket btSocket;
		private final BluetoothDevice btDevice;

		public ConnectThread(String deviceAdress) {

			btDevice = btAdapter.getRemoteDevice(deviceAdress);
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = btDevice.createRfcommSocketToServiceRecord(SERVICE_ID);

			} catch (IOException e) {
				System.out.println("Socket create() failed");
				e.printStackTrace();
			}

			btSocket = tmp;
		}

		public void run() {

			System.out.println("ConnectThread has begun.");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			btAdapter.cancelDiscovery();
			
			System.out.println(btAdapter);
			System.out.println(btSocket);
			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				btSocket.connect();
			} catch (IOException e) {
				System.out.println("Connection failed");
				e.printStackTrace();

				// Close the socket
				try {
					btSocket.close();
				} catch (IOException e2) {
					System.out.println("unable to close() socket during connection failure");
					e2.printStackTrace();
				}
				setState(STATE_NONE);
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothConnection.this) {
				connectThread = null;
			}

			// Start the connected thread
			connected(btSocket, btDevice);
			System.out.println("ConnectThread finished.");
		}

		public void cancel() {
			try {
				btSocket.close();
			} catch (IOException e) {
				System.out.println("close() of connect socket failed");
				e.printStackTrace();
			}
		}
	}

	private class ConnectedThread extends Thread {

		private final BluetoothSocket btSocket;
		private final InputStream btInStream;
		private final OutputStream btOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			System.out.println("create ConnectedThread");
			btSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				System.out.println("temp sockets not created");
				e.printStackTrace();
			}

			btInStream = tmpIn;
			btOutStream = tmpOut;
		}

		public void run() {

			System.out.println("ConnectedThread has begun.");
			byte[] buffer = new byte[1024];
			int pos = 0;

			boolean lineComplete = false;

			// Keep listening to the InputStream while connected
			while (true) {
				try {

					while (true) {
						int r = btInStream.read();

						if (r == -1) {
							break;
						} else if (r == 13) {
							break;
						} else if (r == 10) {

							lineComplete = true;
							break;
						} else {
							buffer[pos] = (byte) r;
							pos++;
						}
					}

					if (lineComplete) {
						System.out.println("Message received: " + new String(buffer, 0, pos));
						pos = 0;
						lineComplete = false;
					}

				} catch (IOException e) {
					System.out.println("disconnected");
					e.printStackTrace();
					cancel();
					break;
				} 
			}

			System.out.println("ConnectedThread finished.");
		}

		/**
		 * Write to the connected OutStream.
		 *
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				btOutStream.write(buffer);

			} catch (IOException e) {
				System.out.println("Exception during write");
				e.printStackTrace();
			}
		}

		public synchronized void cancel() {
			try {
				btSocket.close();
			} catch (IOException e) {
				System.out.println("close() of connect socket failed");
				e.printStackTrace();
			}
		}
	}
}