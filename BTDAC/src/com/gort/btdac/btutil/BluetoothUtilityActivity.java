package com.gort.btdac.btutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gort.btdac.R;

public class BluetoothUtilityActivity extends Activity implements OnClickListener, OnItemClickListener {
	
	private static final String TAG = "com.gort.btdac.btutil.BluetoothUtilityActivity";
	private static final String NAME = "BTDAC";
	
	public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private static final int REQUEST_ENABLE_BT = 1;
	protected static final int SUCCESS_CONNECT = 6;

	private static final int DISCOVERABLE_DURATION = 200; // how long in seconds the local device is discoverable
	
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
	
	// Bluetooth object declarations
	private BluetoothAdapter myBluetoothAdapter;  // Local bluetooth adapter found in handset
	private Set<BluetoothDevice> myPairedDevices; // Devices that have been already paired by system
	private ArrayList<BluetoothDevice> myListedDevices; // Complete list of devices found paired & unpaired
	
	private ArrayAdapter<String> myArrayAdapter; // Contains the total list of devices found used for deviceList
	
	// View widgets declarations
	private Switch checkBtEnabled;
	private ToggleButton discoverBtDevice;
	private Button testBtConnection;
	
	private ListView deviceList;
	private TextView usageInstructions;
	private TextView connectionMsgLogs;
	
	private AcceptThread myServerThread;
	
	// Handler for ....
	private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "inside handleMessage - with case:" + msg.what);
            
            super.handleMessage(msg);
            switch(msg.what) {
            case SUCCESS_CONNECT:
                // DO something
                ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                
                Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_SHORT).show();
                
                String s = "successfully connected";
                connectedThread.write(s.getBytes());
                Log.i(TAG, "connected");
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String string = new String(readBuf);
                Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
	
	// Create a BroadcastReceiver for listening to Activity specific Intents
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            Toast.makeText(getApplicationContext(), "bluetooth action - " + action.toString(), Toast.LENGTH_LONG).show();
	            connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	            // Add the name and address to an array adapter to show in a ListView
	            myArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	            myListedDevices.add(device);
	        }
	        
	        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	        	// listens for the state of the bluetooth adapter
	        	switch (myBluetoothAdapter.getState()) {
	        	case BluetoothAdapter.STATE_TURNING_ON:
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		Log.d(TAG, "the btadpter state is turning on");
	        		break;
	        	case BluetoothAdapter.STATE_ON:
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		Log.d(TAG, "the btadpter state is on");
	        		break;
	        	case BluetoothAdapter.STATE_TURNING_OFF:
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		Log.d(TAG, "the btadpter state is turning off");
	        		break;
	        	case BluetoothAdapter.STATE_OFF:
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		Log.d(TAG, "the btadpter state is off");
	        		turnOnBluetooth();
	        		break;	        		
	        	}
	        }
	        
	        if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
	        	switch (myBluetoothAdapter.getScanMode()) {
	        	case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
	        		// not in discoverable mode but still able to receive connections
	        		Log.d(TAG, "the scan mode is connectable");
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		break;
	        	case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
	        		// the device is either in discoverable mode
	        		Log.d(TAG, "the scan mode is connectable & discoverable");
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		break;
	        	case BluetoothAdapter.SCAN_MODE_NONE:
	        		// not in discoverable mode and unable to receive connections
	        		Log.d(TAG, "the scan mode is neither connectable or discoverable");
	        		connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        		break;
	        	}
	        }
	        
	        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
	        	Log.d(TAG, "");
	        	connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        }
	        
	        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	Log.d(TAG, "");
	        	connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        }
	       
	    }
	};

	/********************************************* Activity and View control methods *****************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_utility);
		Log.d(TAG, "Activity onCreate()");
		
		// Setup and initializer methods for the activity
		setupUtilityActivity();
		
		// Show the Up button in the action bar.
		setupActionBar();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (resultCode) {
		case RESULT_OK:
			if (requestCode == REQUEST_ENABLE_BT) Log.d(TAG,"successfully requested enable bt");
			break;
		case RESULT_CANCELED:
			if (requestCode == REQUEST_ENABLE_BT) Log.d(TAG,"failed to request enable bt");
			Toast.makeText(getApplicationContext(), R.string.need_bt_enabled, Toast.LENGTH_SHORT).show();
			finish();
			break;
		case DISCOVERABLE_DURATION:
			if (requestCode == DISCOVERABLE_DURATION) Log.d(TAG, "device is discoverable now");
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_utility, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		//call handlers for button clicks based on id
		switch(v.getId()) {
		case R.id.bt_radio_sw:
			Log.d(TAG, "Activity checkForBtEnabled() has been pressed");
			checkForBtEnabled();
			break;
		case R.id.scan_bt_btn:
			Log.d(TAG, "Activity performDeviceDiscovery() has been pressed");
			performDeviceDiscovery();
			break;
//		case R.id.bt_connect_btn:
//			Log.d(TAG, "Activity commencing Server thread and socket has been pressed");
//			myServerThread = new AcceptThread();
//			myServerThread.start();
//			break;
		default:
			break;
		}	
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.d(TAG, "You clicked this item: " + view.toString() + " at " + position + " with " + id);
		connectionMsgLogs.append("You clicked this item: " + view.toString() + " at " + position + " with " + id);
		// cancel any discovery before attempting to create a connection
		if (myBluetoothAdapter.isDiscovering()) {
			myBluetoothAdapter.cancelDiscovery();
			Log.d(TAG, "Stopped discovery process");
			 connectionMsgLogs.append("canceling discovery");
		}
 

		BluetoothDevice selectedDevice = myListedDevices.get(position);
        
		ConnectThread connect = new ConnectThread(selectedDevice);
        
        connect.start();
        Log.d(TAG, "selectedDevice = " + selectedDevice.getName() + " " + selectedDevice.getBondState() + "\n");
        Log.d(TAG, "selectedDevice = " + connect.getId() + " " + connect.getName() + connect.getState() + "\n");
		connectionMsgLogs.append("selectedDevice = " + selectedDevice.getName() + " " + selectedDevice.getBondState() + "\n");
		connectionMsgLogs.append("selectedDevice = " + connect.getId() + " " + connect.getName() + connect.getState() + "\n");
    }
		
	/************************************ Private Activity methods used for internal setups ********************************************/

	/*** Set up the {@link android.app.ActionBar}. */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	private void setupUtilityActivity() {
		// attach view and onclick listeners to the view buttons
		checkBtEnabled = (Switch) findViewById(R.id.bt_radio_sw);
		checkBtEnabled.setOnClickListener(this);		
		
		discoverBtDevice = (ToggleButton) findViewById(R.id.scan_bt_btn);
		discoverBtDevice.setOnClickListener(this);
		
		testBtConnection = (Button) findViewById(R.id.test_bt_btn);
		testBtConnection.setOnClickListener(this);
		
		usageInstructions = (TextView) findViewById(R.id.usage_instruction);
	
		connectionMsgLogs = (TextView) findViewById(R.id.bt_device_msg_log);
		
		// prepare the adapter array that will hold the values of bluetooth devices - needed for listviews
		// creates individualized textviews for each bt devices added.
		myArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		
		// this holds the complete list of devices found - both paired and unpaired
		myListedDevices = new ArrayList<BluetoothDevice>();
		
		// holds the logical list of bluetooth devices - primarily populated when device discovery occurs
		deviceList = (ListView) findViewById(R.id.device_list);
		deviceList.setOnItemClickListener(this);
		deviceList.setAdapter(myArrayAdapter);
		
		registerBroadcastReceivers();
	}
		
	private void registerBroadcastReceivers() {
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(myReceiver, filter); // Don't forget to unregister during onDestroy
	}
	
	private void checkForBtEnabled() {
		// checks device for a bluetooth adapter
		try {
			myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (myBluetoothAdapter == null) {
			    // Device does not support Bluetooth
				Log.d(TAG, "No bluetooth adapter found on device");
				connectionMsgLogs.append("Failure - No bluetooth adapter found on device.\n");
				Toast.makeText(getApplicationContext(), R.string.device_bt_support, Toast.LENGTH_SHORT).show();
				finish();
			}
			else {
				// checks to see if the bluetooth is enabled
				if (!myBluetoothAdapter.isEnabled()) {
					connectionMsgLogs.append("Success - Bluetooth found. Send Enable request to system.\n");
					Log.d(TAG, "Since bluetooth is not enabled - send enable request");
					turnOnBluetooth();
				}
				
				Log.d(TAG, "Bluetooth has been enabled and is ready to go");
				connectionMsgLogs.append("Success - Bluetooth has been enabled and is ready to go.\n");
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Problem in the checkForBtEnabled() " + e);
		}
	}
	
	private void turnOnBluetooth() {
		Log.d(TAG, "sending enable bluetooth request intent");
	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}
	
	private void performDeviceDiscovery() {
		try {
			checkForBtEnabled();
			myArrayAdapter.clear();
			myListedDevices.clear();

			myPairedDevices = myBluetoothAdapter.getBondedDevices();
			connectionMsgLogs.append("Commencing device discovery - \n");
			
			// If there are paired devices - already known to exist
			if (!myPairedDevices.isEmpty()) {
				Log.d(TAG, "bluetooth adapter has some paired devices - obtaining list");
				connectionMsgLogs.append("Commencing device discovery - \n");
				// Loop through paired devices
			    for (BluetoothDevice device : myPairedDevices) {
			        // Add the name and address to an array adapter to show in a ListView
			    	Log.d(TAG, "Found Device: " + device.getName() + " with MAC: " + device.getAddress());
			    	connectionMsgLogs.append("Found Device: " + device.getName() + " with MAC: " + device.getAddress() + "\n");
			        myArrayAdapter.add(device.getName() + "\n" + device.getAddress() + " \n (Paired)");
//			        myListedDevices.add(device);
			    }
			    
			    myListedDevices.addAll(myPairedDevices);
			}
			
			startBluetoothDiscovery();
			
			Log.d(TAG, "end performDeviceDiscovery()");
		} catch (Exception e) {
			Log.e(TAG, "Failed device discovery: " + e);
		}
	}

	private void startBluetoothDiscovery() {
		String debugmsg;
		// asynchronous - immediately returns with a boolean if discovery has successfully started/canceled
		// be certain that you always cancel/stop discovery before attempting a connection
		// you should not perform discovery while connected
		// performing discovery can significantly reduce the bandwidth available for the connection
		try {
			
			debugmsg = (myBluetoothAdapter.cancelDiscovery()) ? "Cancel any outstanding discoveries" : "Did not cancel discoveries";
			Log.d(TAG, debugmsg);
		
			debugmsg = (myBluetoothAdapter.startDiscovery()) ? "Commence bluetooth device discovery" : "Failed to commmence discoveries";
			Log.d(TAG, debugmsg);

			// make my local device discoverable for the duration in seconds
			Intent discoverableIntent = new	Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
			startActivity(discoverableIntent);
		
		} catch (Exception e) {
			Log.e(TAG, "Device discovery failed: " + e);
		}
	}

    private void manageConnectedSocket(BluetoothSocket socket) {
    	 Log.d(TAG, "Entered the manageConnected socket");
         connectionMsgLogs.append("Entered the manageConnected socket");
	}
    
	/*************************************** Private Bluetooth Connection Classes ***********************************************************/
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	        	// MY_UUID is the app's UUID string, also used by the client code
	            tmp = myBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
	            Log.d(TAG, "AcceptThread - " + tmp.toString());
	            connectionMsgLogs.append("Acceptthread constructor.");
	        } catch (IOException e) { }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
	                Log.d(TAG, "AcceptThread - " + socket.isConnected() + " " + socket.toString());
	                connectionMsgLogs.append("AcceptThread - " + socket.isConnected() + " " + socket.toString());

		            // If a connection was accepted
		            if (socket != null) {
		                // Do work to manage the connection (in a separate thread)
		            	Log.d(TAG, "AcceptThread - " + socket.isConnected() + " " + socket.toString());
			            connectionMsgLogs.append("AcceptThread - " + socket.isConnected() + " " + socket.toString());
		                manageConnectedSocket(socket);
		                mmServerSocket.close();
		                break;
		            }
	            
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
		/** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        myBluetoothAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        manageConnectedSocket(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	                
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
} // END OF CLASS 
