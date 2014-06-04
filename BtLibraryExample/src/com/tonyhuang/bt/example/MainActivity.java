package com.tonyhuang.bt.example;

import java.util.ArrayList;
import java.util.List;

import com.tonyhuang.bt.lib.BtController;
import com.tonyhuang.bt.lib.BtListener;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity implements BtListener {
	private static final String TAG = "BtLibraryExample";
	
	private static Button mDiscovery;
	private static Button mPaired;
	private static Button mEnableDiscoverability;
	private static ListView mDevicesList;
	
	private static BtController mBtController;
	private static BluetoothDevice mDevice;
	
	private static String[] mDevicesStr = new String[0];
	private static String[] mStatusStr = new String[0];
	private static List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	private static DeviceAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mBtController = new BtController(this, this);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!mBtController.start()) {
			Log.e(TAG, "Start BtController failed.");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mBtController.stop();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			
			mDiscovery = (Button) rootView.findViewById(R.id.discovery);
			mPaired = (Button) rootView.findViewById(R.id.paired);
			mEnableDiscoverability = (Button) rootView.findViewById(R.id.enableDiscoverability);
			mDevicesList = (ListView) rootView.findViewById(R.id.listView);
			
			mAdapter = new DeviceAdapter(getActivity().getApplicationContext(), mDevicesStr, mStatusStr);
			mDevicesList.setAdapter(mAdapter);
			
			mDiscovery.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mBtController.startDiscovery();
					mDevices.clear();
					update();
					mAdapter.notifyDataSetChanged();
				}
			});
			mPaired.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mDevices = mBtController.getPairedDevices();
					update();
					mAdapter.notifyDataSetChanged();
				}
			});
			mEnableDiscoverability.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mBtController.enableDiscoverability(300);
				}
			});
			mDevicesList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					mDevice = mDevices.get(position);
					if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
						if (mBtController.getConnectStatus(mDevice) == BluetoothA2dp.STATE_CONNECTED) {
							mBtController.unpairDevice(mDevice);
						} else {
							mBtController.connectDevice(mDevice);
						}
					} else if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
						mBtController.pairDevice(mDevice);
					}
				}
			});
			
			return rootView;
		}
	}

	@Override
	public void onFoundBtDevice(BluetoothDevice device) {
		Log.d(TAG, "discovery: " + device.getName());
		mDevices.add(device);
		update();
		notifyUpdate();
	}

	@Override
	public void onFinishDiscovery() {
		Log.d(TAG, "onFinishDiscovery.");
	}

	@Override
	public void onPaired() {
		Log.d(TAG, "onPaired.");
		update();
		notifyUpdate();
	}

	@Override
	public void onUnpaired() {
		Log.d(TAG, "onUnpaired.");
		update();
		notifyUpdate();
	}

	@Override
	public void onConnected() {
		Log.d(TAG, "onConnected.");
		update();
		notifyUpdate();
	}

	@Override
	public void onDisconnect() {
		Log.d(TAG, "onDisconnect.");
		update();
		notifyUpdate();
	}

	private void notifyUpdate() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	
	private static void update() {
		mDevicesStr = new String[mDevices.size()];
		mStatusStr = new String[mDevices.size()];
		for (int i = 0 ; i < mDevicesStr.length ; i++) {
			mDevicesStr[i] = mDevices.get(i).getName();
			if (mDevices.get(i).getBondState() == BluetoothDevice.BOND_NONE) {
				mStatusStr[i] = "";
			} else if (mDevices.get(i).getBondState() == BluetoothDevice.BOND_BONDED) {
				if (mBtController.getConnectStatus(mDevices.get(i)) == BluetoothA2dp.STATE_CONNECTED) {
					mStatusStr[i] = "Conntected";
				} else {
					mStatusStr[i] = "Paired";
				}
			}
		}
		mAdapter.update(mDevicesStr, mStatusStr);
	}
}
