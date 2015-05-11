package com.example.iuas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import at.uibk.informatik.androbot.contracts.Constants;
import at.uibk.informatik.androbot.contracts.IConnection;

public class BluetoothConnection implements IConnection {

	private static final String LOG_TAG = "BluetoothConnection";
	private static final UUID SERVICE_ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothAdapter btAdapter;
	private Handler handler;
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

		// TODO: Activate Bluetooth

		this.state = STATE_NONE;
	}

	/**
	 * Returns the mac address from the remote device of the connection.
	 * 
	 * @return the remote device
	 */
	public String getDeviceAddress() {
		return deviceAddress;
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
		//handler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Sprint.IConnecition#getState()
	 */
	@Override
	public synchronized int getState() {
		return this.state;
	}

	@Override
	public synchronized void setReadHandler(Handler readHandler) {

		if (this.state != STATE_NONE)
			return;

		this.handler = readHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Sprint.IConnecition#connect(android.bluetooth.BluetoothDevice, boolean)
	 */
	@Override
	public synchronized void connect() {

		Log.d(LOG_TAG, "connect to: " + getDeviceAddress());

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
		connectThread = new ConnectThread(this.getDeviceAddress());
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

		Log.d(LOG_TAG, "connected to " + device.getName());

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

		// Send the name of the connected device back to the caller
//		Message msg = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
//		Bundle bundle = new Bundle();
//		bundle.putString(Constants.DEVICE_NAME, device.getName());
//		msg.setData(bundle);
//		handler.sendMessage(msg);
//
		setState(STATE_CONNECTED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Sprint.IConnecition#stop()
	 */
	@Override
	public synchronized void stop() {
		Log.d(LOG_TAG, "stop");

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see Sprint.IConnecition#write(byte[])
	 */
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

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {

		// Send a failure message back to the Activity
		//Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
		//Bundle bundle = new Bundle();
//		bundle.putString(Constants.TOAST, "Unable to connect device");
//		msg.setData(bundle);
//		handler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {

		// Send a failure message back to the Activity
//		Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
//		Bundle bundle = new Bundle();
//		bundle.putString(Constants.TOAST, "Device connection was lost");
//		msg.setData(bundle);
//		handler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was closed and notify the UI Activity.
	 */
	private void connectionClosed() {

		// Send a failure message back to the Activity
//		Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
//		Bundle bundle = new Bundle();
//		bundle.putString(Constants.TOAST, "Device connection closed");
//		msg.setData(bundle);
//		handler.sendMessage(msg);
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
				Log.e(LOG_TAG, "Socket create() failed", e);
			}

			btSocket = tmp;
		}

		public void run() {

			Log.i(LOG_TAG, "BEGIN ConnectThread");
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
				Log.e(LOG_TAG, "Connection failed", e);

				// Close the socket
				try {
					btSocket.close();
				} catch (IOException e2) {
					Log.e(LOG_TAG, "unable to close() socket during connection failure", e2);
				}
				connectionFailed();
				setState(STATE_NONE);
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothConnection.this) {
				connectThread = null;
			}

			// Start the connected thread
			connected(btSocket, btDevice);

			Log.i(LOG_TAG, "END ConnectThread");
		}

		public void cancel() {
			try {
				btSocket.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "close() of connect socket failed", e);
			}
		}
	}

	private class ConnectedThread extends Thread {

		private final BluetoothSocket btSocket;
		private final InputStream btInStream;
		private final OutputStream btOutStream;
		private boolean canceled;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(LOG_TAG, "create ConnectedThread");
			btSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			canceled = false;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(LOG_TAG, "temp sockets not created", e);
			}

			btInStream = tmpIn;
			btOutStream = tmpOut;
		}

		public void run() {

			Log.i(LOG_TAG, "BEGIN ConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;
			int pos = 0;

			boolean lineComplete = false;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					// bytes = btInStream.read(buffer);

					while (true) {
						int r = btInStream.read();
						// Log.d(LOG_TAG, String.format("%d", r));

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
						// Send the obtained bytes to the caller
						Log.d(LOG_TAG, "Message received: " + new String(buffer, 0, pos));
//						handler.obtainMessage(Constants.MESSAGE_READ, pos, -1, buffer).sendToTarget();

						pos = 0;
						lineComplete = false;
					}

				} catch (IOException e) {
					Log.i(LOG_TAG, "disconnected");

					if (canceled) {
						connectionClosed();
					} else {
						Log.e(LOG_TAG, "Exception: ", e);
						connectionLost();
					}

					break;
				} 
			}

			Log.i(LOG_TAG, "END ConnectedThread");
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

				// Share the sent message back to the caller
				// handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();

			} catch (IOException e) {
				Log.e(LOG_TAG, "Exception during write", e);
			}
		}

		public synchronized void cancel() {
			try {
				this.canceled = true;
				btSocket.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "close() of connect socket failed", e);
			}
		}
	}
}