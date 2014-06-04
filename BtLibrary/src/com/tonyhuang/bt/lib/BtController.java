package com.tonyhuang.bt.lib;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Tony Huang (starlightslo@gmail.com)
 * @version Creation time: 2014/6/3 下午4:11:17
 */
public class BtController {
	private static final String TAG = "BtController";

	private Context mContext;
	private BtListener mBtListener;

	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothA2dp mBluetoothA2dp = null;
	private IBluetoothA2dp mIBluetoothA2dp = null;
	private boolean isNewAPI = false;

	public BtController(Context context, BtListener listener) {
		mContext = context;
		mBtListener = listener;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device != null)
					mBtListener.onFoundBtDevice(device);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
				mBtListener.onFinishDiscovery();
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
				final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
				final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

				if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
					mBtListener.onPaired();
				} else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
					mBtListener.onUnpaired();
				}
			} else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
				final int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothDevice.ERROR);
				
				if (state == BluetoothA2dp.STATE_CONNECTED) {
					mBtListener.onConnected();
				} else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
					mBtListener.onDisconnect();
				}
			}
		}
	};

	private boolean initBluetoothA2dp() {
		try {
			Class<?> c2 = Class.forName("android.os.ServiceManager");
			Method m2 = c2.getDeclaredMethod("getService", String.class);
			IBinder b = (IBinder) m2.invoke(c2.newInstance(), "bluetooth_a2dp");
			if (b == null) {
				// For Android 4.2 Above Devices
				isNewAPI = true;
				if (!BluetoothAdapter.getDefaultAdapter().getProfileProxy(mContext, new ServiceListener() {
					@Override
					public void onServiceConnected(int profile, BluetoothProfile proxy) {
						if (BluetoothProfile.A2DP == profile) {
							mBluetoothA2dp = (BluetoothA2dp) proxy;
						}
					}
					
					@Override
					public void onServiceDisconnected(int profile) {
						if (BluetoothProfile.A2DP == profile) {
							mBluetoothA2dp = null;
						}
					}
				}, BluetoothProfile.A2DP)) {
					return false;
				}
			} else {
				// For Android below 4.2 devices
				isNewAPI = false;
				Class<?> c3 = Class.forName("android.bluetooth.IBluetoothA2dp");
				Class<?>[] s2 = c3.getDeclaredClasses();
				Class<?> c = s2[0];
				Method m = c.getDeclaredMethod("asInterface", IBinder.class);
				m.setAccessible(true);
				mIBluetoothA2dp = (IBluetoothA2dp) m.invoke(null, b);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean enableBluetooth() {
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.enable()) {
				return false;
			}
		}
		return true;
	}

	public boolean disableBluetooth() {
		if (mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.disable()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean start() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			return false;
		}

		if (!enableBluetooth()) {
			return false;
		}

		if (!initBluetoothA2dp()) {
			Log.e(TAG, "Init bluetooth a2dp failed.");
			return false;
		}

		if (mContext != null) {
			mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
			mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
			mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
		}

		return true;
	}

	public void stop() {
		if (mContext != null)
			mContext.unregisterReceiver(mReceiver);
	}

	public void startDiscovery() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) stopDiscovery();
			mBluetoothAdapter.startDiscovery();
		}
	}
	
	public void stopDiscovery() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering())
				mBluetoothAdapter.cancelDiscovery();
		}
	}

	public void enableDiscoverability(int sec) {
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, sec);
			mContext.startActivity(discoverableIntent);
		}
	}

	public void pairDevice(BluetoothDevice device) {
		stopDiscovery();
		try {
			Method method = device.getClass().getMethod("createBond", (Class[]) null);
			method.invoke(device, (Object[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void unpairDevice(BluetoothDevice device) {
		stopDiscovery();
		try {
			Method method = device.getClass().getMethod("removeBond", (Class[]) null);
			method.invoke(device, (Object[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<BluetoothDevice> getPairedDevices() {
		if (mBluetoothAdapter == null) return null;
		Set<BluetoothDevice> setPairedDevices = mBluetoothAdapter.getBondedDevices();
		List<BluetoothDevice> pariedDevices = new ArrayList<BluetoothDevice>();
		
		for (BluetoothDevice device : setPairedDevices) {
			pariedDevices.add(device);
		}
		
		return pariedDevices;
	}
	
	public void connectDevice(BluetoothDevice device) {
		stopDiscovery();
		try {
			if (device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED) {
				if (isNewAPI) {
					mBluetoothA2dp.getClass().getMethod("connect", BluetoothDevice.class).invoke(mBluetoothA2dp, device);
				} else {
					mIBluetoothA2dp.connect(device);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getConnectStatus(BluetoothDevice device) {
		int status = -1;
		try {
			if (device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED) {
				if (isNewAPI) {
					if (mBluetoothA2dp != null)
						return mBluetoothA2dp.getConnectionState(device);
				} else {
					if (mIBluetoothA2dp != null)
						return mIBluetoothA2dp.getConnectionState(device);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return status;
	}

}
