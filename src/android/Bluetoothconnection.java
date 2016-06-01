  
package org.apache.cordova.Bluetoothconnection;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import java.util.UUID;


public class Bluetoothconnection extends CordovaPlugin {
	
	private static final String LIST = "list";
    private static final String CONNECT = "connect";
	private static final int REQUEST_ENABLE_BT = 2;
	
	private BluetoothAdapter mBluetoothAdapter;
	private Vector<BluetoothDevice> remoteDevices;
	private BroadcastReceiver searchFinish;
	private BroadcastReceiver searchStart;
	private BroadcastReceiver discoveryResult;
	private Thread hThread;
	private Context context;
	private connTask connectionTask;
	private BluetoothPort bluetoothPort;

	public Bluetoothconnection()
	{
		
	}
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
	 
	
	if (mBluetoothAdapter == null) {
           mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		    bluetoothSetup(callbackContext);
        }
	
	if (action.equals("list")) {

        listBondedDevices(callbackContext);
        return true;
    }else if (action.equals("isConnected")) {

        boolean secure = true;
        connect(args, secure, callbackContext);
        return true;
    }
		
	return false;	
	}
	
	public void bluetoothSetup(CallbackContext callbackContext)
	{
		try
		{
			bluetoothPort = BluetoothPort.getInstance();
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) 
			{
				errMsg = "No bluetooth adapter available";
				Log.e(LOG_TAG, errMsg);
				callbackContext.error(errMsg);
				return;
			}
			if (!mBluetoothAdapter.isEnabled()) 
			{
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			    
			}
		}
		catch (Exception e) {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
	}
		public void listBondedDevices(CallbackContext callbackContext)
		{
			try
			{
				bluetoothSetup(callbackContext);
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
				if (pairedDevices.size() > 0) {
					JSONArray json = new JSONArray();
					for (BluetoothDevice device : pairedDevices) {
						json.put(device.getName() +"\n["+device.getAddress()+"] [Paired]");
					}
					callbackContext.success(json);
				} else {
					callbackContext.error("No Bluetooth Device Found");
				}
				
			}
			catch (Exception e) {
				errMsg = e.getMessage();
				Log.e(LOG_TAG, errMsg);
				e.printStackTrace();
				callbackContext.error(errMsg);
			}
		}
		public void connect(CordovaArgs args, boolean secure, CallbackContext callbackContext) 
		{
			try
			{
		        String macAddress = args.getString(0);
		        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
	
		        if (device != null) {
		        	if((connectionTask != null) && (connectionTask.getStatus() == AsyncTask.Status.RUNNING))
		    		{
		    			connectionTask.cancel(true);
		    			if(!connectionTask.isCancelled())
		    				connectionTask.cancel(true);
		    			connectionTask = null;
		    		}
		    		connectionTask = new connTask();
		    		connectionTask.execute(btDev);

	
		            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
		            result.setKeepCallback(true);
		            callbackContext.sendPluginResult(result);
	
		        } else {
		            callbackContext.error("Could not connect to " + macAddress);
		        }
	        }
			catch(IllegalArgumentException e)
			{
				// Bluetooth Address Format [OO:OO:OO:OO:OO:OO]
				errMsg = e.getMessage();
				Log.e(LOG_TAG, errMsg);
				e.printStackTrace();
				callbackContext.error(errMsg);
			}
			catch (IOException e)
			{
				errMsg = e.getMessage();
				Log.e(LOG_TAG, errMsg);
				e.printStackTrace();
				callbackContext.error(errMsg);
			}

		}		
	    
		// Bluetooth Connection Task.
		class connTask extends AsyncTask<BluetoothDevice, Void, Integer>
		{
			private final ProgressDialog dialog = new ProgressDialog(BluetoothConnectMenu.this);
			
			@Override
			protected void onPreExecute()
			{
				dialog.setTitle(getResources().getString(R.string.bt_tab));
				dialog.setMessage(getResources().getString(R.string.connecting_msg));
				dialog.show();
				super.onPreExecute();
			}
			
			@Override
			protected Integer doInBackground(BluetoothDevice... params)
			{
				Integer retVal = null;
				try
				{
					bluetoothPort.connect(params[0]);
					lastConnAddr = params[0].getAddress();
					retVal = Integer.valueOf(0);
				}
				catch (IOException e)
				{
					retVal = Integer.valueOf(-1);
				}
				return retVal;
			}
			
			@Override
			protected void onPostExecute(Integer result)
			{
				if(result.intValue() == 0)	// Connection success.
				{
					RequestHandler rh = new RequestHandler();				
					hThread = new Thread(rh);
					hThread.start();
					// UI
					connectButton.setText(getResources().getString(R.string.dev_disconn_btn));
					list.setEnabled(false);
					btAddrBox.setEnabled(false);
					searchButton.setEnabled(false);
					if(dialog.isShowing())
						dialog.dismiss();				
					Toast toast = Toast.makeText(context, getResources().getString(R.string.bt_conn_msg), Toast.LENGTH_SHORT);
					toast.show();
				}
				else	// Connection failed.
				{
					if(dialog.isShowing())
						dialog.dismiss();				
					AlertView.showAlert(getResources().getString(R.string.bt_conn_fail_msg),
										getResources().getString(R.string.dev_check_msg), context);
				}
				super.onPostExecute(result);
			}
		}
	}
	
	
	
	