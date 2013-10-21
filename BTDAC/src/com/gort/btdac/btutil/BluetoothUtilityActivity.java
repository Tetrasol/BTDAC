package com.gort.btdac.btutil;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import com.gort.btdac.R;

public class BluetoothUtilityActivity extends Activity implements OnClickListener, OnItemClickListener {
	
	private static final String TAG = "com.gort.btdac.btutil.BluetoothUtilityActivity";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int DISCOVERABLE_DURATION = 200; // how long in seconds the local device is discoverable
	
	// Bluetooth object declarations
	private BluetoothAdapter myBluetoothAdapter;	// Local bluetooth adapter found in handset
	private Set<BluetoothDevice> myPairedDevices;	// Devices that have been already paired by system
	
	private ArrayAdapter<String> myArrayAdapter;
	
	// View widgets declarations
	private Button checkBtEnabled;
	private Button discoverBtDevice;
	private ListView deviceList;
	private TextView usageInstructions;
		
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            // Add the name and address to an array adapter to show in a ListView
	            myArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	        }
	        
	        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	        	switch (myBluetoothAdapter.getState()) {
	        	case BluetoothAdapter.STATE_TURNING_ON:
	        		break;
	        	case BluetoothAdapter.STATE_ON:
	        		break;
	        	case BluetoothAdapter.STATE_TURNING_OFF:
	        		break;
	        	case BluetoothAdapter.STATE_OFF:
	        		turnOnBluetooth();
	        		break;	        		
	        	}
	        }
	        
	        if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
	        	switch (myBluetoothAdapter.getScanMode()) {
	        	case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
	        		// not in discoverable mode but still able to receive connections
	        		break;
	        	case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
	        		// the device is either in discoverable mode
	        		break;
	        	case BluetoothAdapter.SCAN_MODE_NONE:
	        		// not in discoverable mode and unable to receive connections
	        		break;
	        	}
	        }
	        
	        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
	        	// TODO add some code
	        }
	        
	        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	// TODO add some code
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
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		registerBroadcastReceivers();
	}
	
	@Override
	protected void onPause() {
		//
		super.onPause();
		unregisterReceiver(myReceiver);
	}
	
	@Override
	protected void onDestroy() {
		// 
		super.onDestroy();
		unregisterReceiver(myReceiver);
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
		case R.id.check_bt_enabled_btn:
			Log.d(TAG, "Activity checkForBtEnabled() has been pressed");
			checkForBtEnabled();
			break;
		case R.id.conn_scan_btn:
			Log.d(TAG, "Activity performDeviceDiscovery() has been pressed");
			performDeviceDiscovery();
		default:
			break;
		}	
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.d(TAG, "You clicked this item: " + arg1.toString() + " " + arg2 + " " + arg3);
		
		// cancel any discovery before attempting to create a connection
		if (myBluetoothAdapter.isDiscovering()) {
			myBluetoothAdapter.cancelDiscovery();
			Log.d(TAG, "Stopped discovery process");
		}


	}
		
	/************************************ Private Activity methods used for internal setups ********************************************/

	/*** Set up the {@link android.app.ActionBar}. */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	private void setupUtilityActivity() {
		// attach view and onclick listeners to the view buttons
		checkBtEnabled = (Button) findViewById(R.id.check_bt_enabled_btn);
		checkBtEnabled.setOnClickListener(this);		
		
		discoverBtDevice = (Button) findViewById(R.id.conn_scan_btn);
		discoverBtDevice.setOnClickListener(this);
		
		usageInstructions = (TextView) findViewById(R.id.scan_for_device_msg);
		
		// prepare the adapter array that will hold the values of bluetooth devices - needed for listviews
		// creates individualized textviews for each bt devices added.
		myArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		
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
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(myReceiver, filter); // Don't forget to unregister during onDestroy
	}

	private void performDeviceDiscovery() {
		try {
			checkForBtEnabled();
			myPairedDevices = myBluetoothAdapter.getBondedDevices();
			
			// If there are paired devices
			if (!myPairedDevices.isEmpty()) {
				Log.d(TAG, "bluetooth adapter has some paired devices - obtaining list");
			    
				// Loop through paired devices
			    for (BluetoothDevice device : myPairedDevices) {
			        // Add the name and address to an array adapter to show in a ListView
			    	Log.d(TAG, "Found Device: " + device.getName() + " with MAC: " + device.getAddress());
			        myArrayAdapter.add(device.getName() + "\n" + device.getAddress());
			    }
			}
			
			startBluetoothDiscovery();
			
			Log.d(TAG, "Finished finding device discovery");
		} catch (Exception e) {
			Log.e(TAG, "Failed device discovery: " + e);
		}
	}
	
	private void checkForBtEnabled() {
		// checks device for a bluetooth adapter
		try {
			myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (myBluetoothAdapter == null) {
			    // Device does not support Bluetooth
				Log.d(TAG, "No bluetooth adapter found on device");
				Toast.makeText(getApplicationContext(), R.string.device_bt_support, Toast.LENGTH_SHORT).show();
			}
			else {
				// checks to see if the bluetooth is enabled
				if (!myBluetoothAdapter.isEnabled()) {
					Log.d(TAG, "Since bluetooth is not enabled - send enable request");
					turnOnBluetooth();
				}
			}
			
			Log.d(TAG, "Bluetooth has been enabled and is ready to go");
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

	private void startBluetoothDiscovery() {
		String debugmsg;
		// asynchronous - immediately returns with a boolean if discovery has successfully started
		// be certain that you always stop discovery before attempting a connection
		// you should not perform discovery while connected
		// performing discovery can significantly reduce the bandwidth available for the connection
		try {
			
			debugmsg = (myBluetoothAdapter.cancelDiscovery()) ? "Cancel any outstanding discoveries" : "did not cancel discoveries";
			Log.d(TAG, debugmsg);
		
			debugmsg = (myBluetoothAdapter.startDiscovery()) ? "Commence bluetooth device discovery" : "failed to commmence discoveries";
			Log.d(TAG, debugmsg);

			// make my local device discoverable for 5 minutes
			Intent discoverableIntent = new	Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
			startActivity(discoverableIntent);
		
		} catch (Exception e) {
			Log.e(TAG, "Device discovery failed: " + e);
		}
	}
	
} // END OF CLASS 
