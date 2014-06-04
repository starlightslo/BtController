package com.tonyhuang.bt.lib;

import android.bluetooth.BluetoothDevice;

/**
 * @author Tony Huang (starlightslo@gmail.com)
 * @version Creation time: 2014/6/3 下午4:11:52
 */
public interface BtListener {
	public void onFoundBtDevice(BluetoothDevice device);
	public void onFinishDiscovery();
	public void onPaired();
	public void onUnpaired();
	public void onConnected();
	public void onDisconnect();
}
