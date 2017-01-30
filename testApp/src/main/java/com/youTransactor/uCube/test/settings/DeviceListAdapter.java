/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.settings;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.TextView;

import com.youTransactor.uCube.test.AbstractListViewAdapter;

import java.util.Collection;
import java.util.List;

/**
 * Created by gbillard on 3/22/16.
 */
public class DeviceListAdapter extends AbstractListViewAdapter<BluetoothDevice> {

	public DeviceListAdapter() {}

	public DeviceListAdapter(Collection<BluetoothDevice> deviceList) {
		super(deviceList);
	}

	protected void renderITem(View view, BluetoothDevice item) {
		String label = item.getName() == null ? "unnamed" : item.getName() + " - " + item.getAddress();
		((TextView) view).setText(label);
	}

}
