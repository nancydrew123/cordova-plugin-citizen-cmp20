package org.apache.cordova.bluetooth;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.BitmapFactory;
import android.widget.EditText;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Vector;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.app.ProgressDialog;

import com.citizen.*;
import com.citizen.port.android.BluetoothPort;
import com.citizen.request.android.RequestHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.os.AsyncTask.Status;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import android.graphics.Typeface;
import com.citizen.jpos.command.CPCLConst;
import com.citizen.jpos.printer.CPCLPrinter;
import com.citizen.jpos.command.ESCPOSConst;


import com.citizen.jpos.command.CPCLConst;
import com.citizen.jpos.command.ESCPOS;
import com.citizen.jpos.command.ESCPOSConst;
import com.citizen.jpos.printer.ESCPOSPrinter;
import com.citizen.jpos.printer.CMPPrint;

import io.cordova.myapp648ca0.R;

public class Bluetoothconnection extends CordovaPlugin {

	private static final String LIST = "list";
	private static final String CONNECT = "connect";
	private static final int REQUEST_ENABLE_BT = 2;
	private static final String LOG_TAG = "BluetoothPrinter";
	private static final String TAG = "Bluetoothconnection";
	public static final int LENGTH_SHORT = 0;

	private BluetoothAdapter mBluetoothAdapter;
	private Vector<BluetoothDevice> remoteDevices;
	private BroadcastReceiver searchFinish;
	private BroadcastReceiver searchStart;
	private BroadcastReceiver discoveryResult;
	private Thread hThread;
	private Context context;
	private Bluetoothconnection.connTask connectionTask;
	private BluetoothPort bluetoothPort;
	private String lastConnAddr;
	byte FONT_TYPE;
	private static OutputStream btoutputstream;
	private static InputStream btinputtstream;
	private  BluetoothDevice mmDevice;


	//	private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//	private ConnectThread mConnectThread;
	private CPCLPrinter cpclPrinter;
	private ESCPOSPrinter posPtr;
	private final char ESC = ESCPOS.ESC;
	private final char LF = ESCPOS.LF;


	//	private final char ESC = ESCPOS.ESC;
	private  BluetoothSocket mmSocket;
	String macAddress;
	String devicename;

	private static final String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "//temp";
	private static final String fileName;

	private String strCount;

	static {
		fileName = dir + "//BTPrinter";
	}


	public Bluetoothconnection()
	{
		cpclPrinter = new CPCLPrinter();
		posPtr = new ESCPOSPrinter("Shift_JIS");
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {


		clearBtDevData();
		if (mBluetoothAdapter == null) {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			bluetoothSetup(callbackContext);
		}

		if (action.equals("print")) {
			String errMsg = null;
			boolean secure = true;
			if(listBondedDevices(callbackContext)) //getting paired device
			{
				Toast.makeText(this.cordova.getActivity(), "Devices Listed Successfully", Toast.LENGTH_LONG).show();
				try
				{
					if(connect(callbackContext)) //connecting to the paired device
					{
						try
						{
							int sts;


							sts = printerStatus();
							if(sts == ESCPOSConst.CMP_SUCCESS) {
//								sts = posPtr.status();
//								if (sts == ESCPOSConst.CMP_SUCCESS) {


//							if(mmSocket != null) {
									Log.e(LOG_TAG, "Getting to printing function");
									print_bt(args, callbackContext); //Taking the prin of the text

//							}

//								}
							}

						}
						catch(Exception e)
						{
							// Bluetooth Address Format [OO:OO:OO:OO:OO:OO]
							errMsg = e.getMessage();
							Log.e(LOG_TAG, errMsg);
							e.printStackTrace();
							callbackContext.error("Error message" + errMsg);
						}

					}
					else
					{
						callbackContext.error("Could not connect to " + devicename);
						return true;
					}
				}
				catch (Exception e) {
					errMsg = e.getMessage();
					Log.e(LOG_TAG, errMsg);
					e.printStackTrace();
					callbackContext.error(errMsg);
				}
			}
			else
			{
				callbackContext.error("No Bluetooth Device Found");
				return true;
			}

			return true;
		}
		return false;
	}

	public int printerStatus() throws UnsupportedEncodingException
	{
		int sts;

		sts = posPtr.printerCheck();
		if(sts != ESCPOSConst.CMP_SUCCESS) return ESCPOSConst.CMP_FAIL;

		sts = posPtr.status();
		if(sts != ESCPOSConst.CMP_SUCCESS) return sts;

		return sts;
	}
	private void clearBtDevData()
	{
		remoteDevices = new Vector<BluetoothDevice>();
	}

	public void bluetoothSetup(CallbackContext callbackContext)
	{
		String errMsg = null;
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
				this.cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

			}
		}
		catch (Exception e) {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
	}
	boolean listBondedDevices(CallbackContext callbackContext)
	{
		String errMsg = null;
		try
		{
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				JSONArray json = new JSONArray();
				for (BluetoothDevice device : pairedDevices) {
					json.put(device.getName() +","+device.getAddress()+" ,[Paired]");
					macAddress = device.getAddress();
					devicename = device.getName();
				}
				Log.e(LOG_TAG, "Address is " + macAddress);
				return true;
			}
		}
		catch (Exception e) {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
		return false;
	}
	boolean connect(CallbackContext callbackContext)
	{
		String errMsg = null;
		try
		{
			macAddress=macAddress.replace(" ", "");
			Log.e(LOG_TAG,"macaddress in connect" + macAddress );
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);

			if(this.connectionTask != null && this.connectionTask.getStatus() == Status.RUNNING) {
				this.connectionTask.cancel(true);
				if(!this.connectionTask.isCancelled()) {
					this.connectionTask.cancel(true);
				}

				this.connectionTask = null;
			}

			this.connectionTask = new Bluetoothconnection.connTask();
			this.connectionTask.execute(new BluetoothDevice[]{device});
			Toast.makeText(this.cordova.getActivity(), "Connected To A Device Successfully", Toast.LENGTH_LONG).show();

			return true;
		}
		catch(Exception e)
		{
			// Bluetooth Address Format [OO:OO:OO:OO:OO:OO]
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error("Error message" + errMsg);
		}

		return false;
	}

	boolean print_bt(JSONArray  args, CallbackContext callbackContext) {
		try {

			// TODO Auto-generated method stub

			Toast.makeText(this.cordova.getActivity(), "Enter To Print Function", Toast.LENGTH_LONG).show();

//
//			int count = 1;

			//btoutputstream = mmSocket.getOutputStream();

			String str = args.getString(0);

			String newline = "\n";

			String msg = str.toString();

			msg += "\r\n";

			posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "Receipt" + LF + LF);
/*
			posPtr.printBitmap(BitmapFactory.decodeResource(cordova.getActivity().getApplicationContext().getResources(), R.drawable.logo_m),CMPPrint.CMP_ALIGNMENT_CENTER);
*/
			posPtr.printBitmap(BitmapFactory.decodeResource(cordova.getActivity().getApplicationContext().getResources(), R.drawable.img_logo),CMPPrint.CMP_ALIGNMENT_CENTER,200);
			posPtr.lineFeed(2);
			posPtr.printString(msg);
			posPtr.printNormal(ESC + "|cA" + ESC + "|4C" + ESC + "|bC" + "Thank you" + LF);
			//posPtr.lineFeed(2);
			Log.e(LOG_TAG, "Printing success");

			Toast.makeText(this.cordova.getActivity(), "Successfully printed", Toast.LENGTH_LONG).show();
			callbackContext.success("Printed Successfuly : ");
/*
			callbackContext.success("Printed Successfuly : " + msg.getBytes());
*/
			/*mmSocket.close();*/

			return true;


		} catch (Exception e) {
			Log.e(LOG_TAG,"Printing error" + e.getMessage());
			e.printStackTrace();
			callbackContext.error("Some error occured new " + e.getMessage());
		}

		return false;
	}


	// Bluetooth Connection Task.
	class connTask extends AsyncTask<BluetoothDevice, Void, Integer> {
//		private final ProgressDialog dialog = new ProgressDialog(Bluetoothconnection.this);

		private final ProgressDialog dialog = new ProgressDialog( cordova.getActivity().getApplicationContext());

		connTask() {
		}

		protected void onPreExecute() {
			dialog.setTitle("Bluetooth");
			dialog.setMessage("Connecting");
//			this.dialog.show();
			super.onPreExecute();
		}

		protected Integer doInBackground(BluetoothDevice... params) {
			Integer retVal = 1;

			try {
				Bluetoothconnection.this.bluetoothPort.connect(params[0]);
				Bluetoothconnection.this.macAddress = params[0].getAddress();
				retVal = Integer.valueOf(0);
			} catch (IOException var4) {
				retVal = Integer.valueOf(-1);
			}

			return retVal;
		}

		protected void onPostExecute(Integer result) {
			if(result.intValue() == 0) {
				RequestHandler rh = new RequestHandler();
				Bluetoothconnection.this.hThread = new Thread(rh);
				Bluetoothconnection.this.hThread.start();

				if(this.dialog.isShowing()) {
					this.dialog.dismiss();
				}

			} else {
				if(this.dialog.isShowing()) {
					this.dialog.dismiss();
				}

//				AlertView.showAlert(Bluetoothconnection.this.getResources().getString(2131034118), Bluetoothconnection.this.getResources().getString(2131034123), Bluetoothconnection.this.context);
			}

			super.onPostExecute(result);
		}
	}

}




