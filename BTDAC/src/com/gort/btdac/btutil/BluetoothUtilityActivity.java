package com.gort.btdac.btutil;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gort.btdac.R;

public class BluetoothUtilityActivity extends Activity implements OnClickListener, OnItemClickListener, OnCheckedChangeListener {
	
	private static final String TAG = "com.gort.btdac.btutil.BluetoothUtilityActivity";

	public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int DISCOVERABLE_DURATION = 200; // how long in seconds the local device is discoverable
	
    // Message types sent from the BluetoothConnectionHandler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothConnection Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
    // Bluetooth object declarations
    private StringBuffer mOutStringBuffer;    // String buffer for outgoing messages
	private BluetoothAdapter myBluetoothAdapter; // Local bluetooth adapter found in handset
	private BluetoothConnectionHandler myBTConnectionHandler = null; // Member object for handling connections
	
	// Device information containers
    private String mConnectedDeviceName = null; // Name of the connected device
	
	private ArrayList<BluetoothDevice> myListedDevices; // Complete list of devices found paired & unpaired
	private ArrayAdapter<String> myArrayAdapter; // Contains the total list of devices found used for deviceList
	
	// View widgets declarations
	private Switch checkBtEnabled;
	private ToggleButton discoverBtDevice;
	private Button testBtConnection;
	private ListView deviceList;
	private TextView usageInstructions;
	private TextView connectionMsgLogs;

	// Handler for that gets information back from the BluetoothConnectionHandler
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	super.handleMessage(msg);
            Log.d(TAG, "inside handleMessage - with case:" + msg.what);
 
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
            	switch (msg.arg1) {
            	case BluetoothConnectionHandler.STATE_CONNECTED:
            	case BluetoothConnectionHandler.STATE_CONNECTING:
            	case BluetoothConnectionHandler.STATE_LISTEN:
            	case BluetoothConnectionHandler.STATE_NONE:
            		break;
            	}
            	break;
            case MESSAGE_READ:
            	// construct a string from the valid bytes in the buffer
                byte[] readBuf = (byte[]) msg.obj;
                String readMsg = new String(readBuf, 0, msg.arg1);
                connectionMsgLogs.append("Message being read - " + readMsg + " \n");
                break;
            case MESSAGE_WRITE:	
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMsg = new String(writeBuf);
                connectionMsgLogs.append("Message being written - " + writeMsg + " \n");
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
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
	        	connectionMsgLogs.append("bluetooth action - " + action.toString() + "\n");
	        	Log.d(TAG, "bluetooth action - " + action.toString());
	            
	        	// Get the BluetoothDevice object from the Intent
	        	BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	Log.d(TAG, "Device found - " + device.getName());
	        	connectionMsgLogs.append("Device found - " + device.getName() + "\n");
	        	
	        	// If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
    	            // Add the name and address to an array adapter to show in a ListView
                	connectionMsgLogs.append("device is new add to list - " + device.getName() + "\n");
                	Log.d(TAG, "device is new add to list - " + device.getName());
    	            myArrayAdapter.add(device.getName() + "\n" + device.getAddress());
    	            myListedDevices.add(device);
                }
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
	        		// Performing this check in onResume() covers the case in which BT was
	                // not enabled during onStart(), so we were paused to enable it...
	                // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
	                if (myBTConnectionHandler != null) {
	                	Log.d(TAG, "onResume - valid btconnhandler");
	                    // Only if the state is STATE_NONE, do we know that we haven't started already
	                    if (myBTConnectionHandler.getState() == BluetoothConnectionHandler.STATE_NONE) {
	                      // Start the Bluetooth chat services
	                    	Log.d(TAG, "onResume - making btconnhandler start listening thread");	
	                      myBTConnectionHandler.start();
	                    }
	                }
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

	/******************************* Activity and View control methods **************************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set up the window layout
		setContentView(R.layout.activity_bluetooth_utility);
		
		Log.d(TAG, "Activity onCreate()");
		
		// Checks to see if current device is Bluetooth capable
		// Get local Bluetooth adapter
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (myBluetoothAdapter == null) {
		    // Device does not support Bluetooth - End program
			Log.d(TAG, "No bluetooth adapter found on device");
			Toast.makeText(getApplicationContext(), R.string.device_bt_support_no, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		Log.d(TAG, (myBluetoothAdapter != null)?"YES, found bt":"No, did not find bt");
		// Setup and initializer methods for the activity
		setupUtilityActivity();
		
		// Show the Up button in the action bar.
		setupActionBar();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected synchronized void onResume() {
		super.onResume();
		registerBroadcastReceivers();
	}
	
	@Override
	protected synchronized void onPause() {
		super.onPause();
		this.unregisterReceiver(myReceiver);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
        // Stop the Bluetooth chat services
        if (myBluetoothAdapter != null) myBluetoothAdapter.cancelDiscovery();
		if (myBTConnectionHandler != null) myBTConnectionHandler.stop();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
	        case REQUEST_CONNECT_DEVICE_SECURE:
	            // When DeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) {
	               Log.d(TAG, "request connect secure!");
	            	// myBTConnectionHandler.connect(data, true);
	            }
	            break;
	        case REQUEST_CONNECT_DEVICE_INSECURE:
	            // When DeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) {
	            	Log.d(TAG, "request connect insecure!");
	            	// connectDevice(data, false);
	            }
	            break;
	        case REQUEST_ENABLE_BT:
	            // When the request to enable Bluetooth returns
	            if (resultCode == Activity.RESULT_OK) {
	            	Log.d(TAG,"successfully requested enable bt");
	                // Bluetooth is now enabled, so set up a chat session
	                //setupChat();
	            } else if (requestCode == REQUEST_ENABLE_BT) {
	            	Log.d(TAG,"BT not enabled - user did not enable Bluetooth or error occured");  
	            	Toast.makeText(getApplicationContext(), R.string.need_bt_enabled, Toast.LENGTH_SHORT).show();
	                finish();
	            }
	            break;
	        case DISCOVERABLE_DURATION:
	        	Log.d(TAG, "device is discoverable now");
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

		case R.id.scan_bt_btn:
			if (((ToggleButton) v).isChecked()) {
				Log.d(TAG, "Activity performDeviceDiscovery() has been pressed");
				performDeviceDiscovery();
				
				// Performing this check in onResume() covers the case in which BT was
		        // not enabled during onStart(), so we were paused to enable it...
		        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		        if (myBTConnectionHandler != null) {
		            // Only if the state is STATE_NONE, do we know that we haven't started already
		            if (myBTConnectionHandler.getState() == BluetoothConnectionHandler.STATE_NONE) {
		              // Start the Bluetooth chat services
		            	myBTConnectionHandler.start();
		            }
		        }
		    } 
			break;
		
		case R.id.test_bt_btn:
			Log.d(TAG, "Activity testBT has been pressed");
			// Send a message using content of the edit text widget
            sendMessage("Testing message!");

			break;
		default:
			break;
		}	
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (((Switch) buttonView).isChecked()) {
			Log.d(TAG, "Activity checkForBtEnabled() has been turned on");
			doEnableBluetoothRadio();
		}
		// TODO Need to respond to situations in which the user has turned off the switch;
		// therefore, respond to turning off the bluetooth and present alerts to handle disconnections
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.d(TAG, "You clicked this item: " + view.toString() + " at " + position + " with " + id);
		connectionMsgLogs.append("You clicked this item: " + view.toString() + " at " + position + " with " + id);
		
		// cancel any discovery before attempting to create a connection
		if (myBluetoothAdapter.isDiscovering()) {
			connectionMsgLogs.append("canceling discovery");
			myBluetoothAdapter.cancelDiscovery();
			Log.d(TAG, "Stopped discovery process");
		}
		
		BluetoothDevice selectedDevice = myListedDevices.get(position);
		Log.d(TAG, "selectedDevice to connect: " + selectedDevice.getName() + selectedDevice.getAddress());
		connectionMsgLogs.append("selectedDevice to connect: " + selectedDevice.getName() + "\n");
		
		myBTConnectionHandler.connect(selectedDevice, true);
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
		checkBtEnabled.setOnCheckedChangeListener(this);
		
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
		

        // Initialize the BluetoothChatService to perform bluetooth connections
        myBTConnectionHandler = new BluetoothConnectionHandler(this, mHandler);
        Log.d(TAG, (myBTConnectionHandler != null)?"valid btconnhandler":"not valid");
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
		
//		registerBroadcastReceivers();
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
	
	private void doEnableBluetoothRadio() {
		try {
	        // setupChat() will then be called during onActivityResult
			if (myBluetoothAdapter != null) {
				// If BT is not on, request that it be enabled.
				if (!myBluetoothAdapter.isEnabled()) {
					connectionMsgLogs.append("Success - Bluetooth found. Send Enable request to system.\n");
					Log.d(TAG, "Since bluetooth is not enabled - send enable request");
					turnOnBluetooth();
				}
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
		String debugmsg;
		// See if the toggle button has been pressed
		if (checkBtEnabled.isChecked()) {
			try {
					myArrayAdapter.clear();
					myListedDevices.clear();
					
					Log.d(TAG, "Commencing device discovery");
					connectionMsgLogs.append("Commencing device discovery - \n");
					
					// Devices that have been already paired by system
					Set<BluetoothDevice> myPairedDevices = myBluetoothAdapter.getBondedDevices();
									
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
					        /*I can also try to do add devices to the list iteratively*///myListedDevices.add(device);
					    }
					    
					    myListedDevices.addAll(myPairedDevices);
					}
					
					debugmsg = (myBluetoothAdapter.cancelDiscovery()) ? "Cancel any outstanding discoveries" : "Did not cancel discoveries";
					Log.d(TAG, debugmsg);
					connectionMsgLogs.append(debugmsg +"\n");
				
					debugmsg = (myBluetoothAdapter.startDiscovery()) ? "Commence bluetooth device discovery" : "Failed to commmence discoveries";
					Log.d(TAG, debugmsg);
					connectionMsgLogs.append(debugmsg +"\n");
					
					// make my local device discoverable for the duration in seconds
					// asynchronous - immediately returns with a boolean if discovery has successfully started/canceled
					// be certain that you always cancel/stop discovery before attempting a connection
					// you should not perform discovery while connected
					// performing discovery can significantly reduce the bandwidth available for the connection
					Intent discoverableIntent = new	Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
					startActivity(discoverableIntent);
				
					Log.d(TAG, "Exit performDeviceDiscovery()");
	
			} catch (Exception e) {
				Log.e(TAG, "Failed device discovery: " + e);
			}
		}
		else {
			Log.d(TAG, "BLUETOOTH not enabled - make switch on!");
			discoverBtDevice.setChecked(false);
			connectionMsgLogs.append("BLUETOOTH not enabled - make switch on!\n");
		}
	}
	
	private void sendMessage(String message) {
		if (myBluetoothAdapter.getState() != BluetoothConnectionHandler.STATE_CONNECTED) {
			Log.d(TAG, "I am not connected");
		}
		
		//
		if (message.isEmpty()) {
			//
			myBTConnectionHandler.write(message.getBytes());
			
			// 
			mOutStringBuffer.setLength(0);
		}
	}

} // END OF CLASS 
