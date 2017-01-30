/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.rpc.IConnexionManager;
import com.youTransactor.uCube.rpc.RPCManager;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author gbillard on 5/25/16.
 */
public class BluetoothConnexionManager extends BroadcastReceiver implements IConnexionManager {

	private String deviceAddr;
	private BluetoothSocket socket;

	private BluetoothConnexionManager() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		if (!adapter.isEnabled()) {
			adapter.enable();
		}
	}

	public boolean connect() {
		if (deviceAddr == null) {
			return false;
		}

		LogManager.debug(BluetoothConnexionManager.class.getSimpleName(), "connect to " + deviceAddr);

		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException ignored) {}
		}

		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddr);

		try {
//			socket = device.createRfcommSocketToServiceRecord(BT_UUID);
			Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
			socket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));

			socket.connect();

			RPCManager.getInstance().start(socket.getInputStream(), socket.getOutputStream());

			return true;

		} catch (Exception e) {
			LogManager.debug(BluetoothConnexionManager.class.getName(), "connect device error", e);

			if (socket != null && socket.isConnected()) {
				try {
					socket.close();
				} catch (IOException ignored) {}
			}

			return false;
		}
	}

	public void disconnect() {
		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
				socket = null;
			} catch (IOException ignored) {}
		}
	}

	public boolean isInitialized() {
		return StringUtils.isNoneBlank(deviceAddr);
	}

	public void initialize(SharedPreferences settings) {
		setDeviceAddr(settings.getString(MainActivity.DEVICE_MAC_ADDR_SETTINGS_KEY, null));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		switch (intent.getAction()) {
		case BluetoothAdapter.ACTION_STATE_CHANGED:
			if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
				connect();
			}
			break;
		}
	}

	public String getDeviceAddr() {
		return deviceAddr;
	}

	public void setDeviceAddr(String deviceAddr) {
		this.deviceAddr = deviceAddr;
	}

	public static BluetoothConnexionManager getInstance() {
		return INSTANCE;
	}

	private static final BluetoothConnexionManager INSTANCE = new BluetoothConnexionManager();

}
