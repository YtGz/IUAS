/*
 * Original source for this code is
 * https://developer.android.com/samples/BluetoothChat
 */

package at.uibk.informatik.androbot.contracts;

import com.example.iuas.BluetoothConnection;

/**
 * Defines several constants used between {@link BluetoothConnection} and the caller thread.
 */
public interface Constants {
 
    // Message types sent from the BluetoothConnection Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
 
    // Key names received from the BluetoothConnection Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
    
    public static final int REQUEST_EVENT = 100;
    public static final int REQUEST_SENT = 101;    
}