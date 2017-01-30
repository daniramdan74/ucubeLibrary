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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.mdm.MDMManager;
import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.DeviceInfos;
import com.youTransactor.uCube.rpc.command.GetInfosCommand;
import com.youTransactor.uCube.test.BluetoothConnexionManager;
import com.youTransactor.uCube.test.MainActivity;
import com.youTransactor.uCube.test.R;
import com.youTransactor.uCube.test.UIUtils;
import com.youTransactor.uCube.test.payment.PaymentFragment;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author gbillard on 5/25/16.
 */
public class SettingsFragment extends Fragment {

	private TextView deviceFld;
	private BluetoothDevice selectedDevice;
	private TextView urlFld;
	private boolean nfcEnabled;
	private String deviceSerial;
	private String devicePartNUMber;
	private Switch logSwitch;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.settings_fragment, container, false);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences (getContext());

		nfcEnabled = settings.getBoolean(PaymentFragment.NFC_ENABLE_DEVICE_SETTINGS_KEY, false);

		deviceSerial = settings.getString(MDMManager.MDM_DEVICE_SERIAL_SETTINGS_KEY, null);
		devicePartNUMber = settings.getString(MDMManager.MDM_DEVICE_PART_NUMBER_SETTINGS_KEY, null);

		deviceFld = (TextView) view.findViewById(R.id.deviceFld);
		deviceFld.setText(settings.getString(MainActivity.DEVICE_NAME_SETTINGS_KEY, ""));
		deviceFld.setInputType(InputType.TYPE_NULL);
		deviceFld.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDeviceSelectDialog();
			}
		});

		urlFld = (TextView) view.findViewById(R.id.urlFld);
		urlFld.setText(settings.getString(MDMManager.MDM_SERVER_URL_SETTINGS_KEY, MDMManager.DEFAULT_URL));

		Button saveSettingsBtn = (Button) view.findViewById(R.id.saveSettingsBtn);
		saveSettingsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveSettings();
			}
		});

		logSwitch = (Switch) view.findViewById(R.id.logSwitch);
		logSwitch.setChecked(settings.getBoolean(LogManager.LOG_MANAGER_STATE_SETTINGS_KEY, false));

		return view;
	}

	private void saveSettings() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
		SharedPreferences.Editor editor = settings.edit();

		if (selectedDevice != null) {
			editor.putString(MainActivity.DEVICE_MAC_ADDR_SETTINGS_KEY, selectedDevice.getAddress());

			if (StringUtils.isBlank(selectedDevice.getName())) {
				editor.putString(MainActivity.DEVICE_NAME_SETTINGS_KEY, selectedDevice.getAddress());
			} else {
				editor.putString(MainActivity.DEVICE_NAME_SETTINGS_KEY, selectedDevice.getName());
			}
		}

		editor.putString(MDMManager.MDM_SERVER_URL_SETTINGS_KEY, urlFld.getText().toString());

		editor.putBoolean(PaymentFragment.NFC_ENABLE_DEVICE_SETTINGS_KEY, nfcEnabled);

		editor.putString(MDMManager.MDM_DEVICE_SERIAL_SETTINGS_KEY, deviceSerial);
		editor.putString(MDMManager.MDM_DEVICE_PART_NUMBER_SETTINGS_KEY, devicePartNUMber);

		editor.putBoolean(LogManager.LOG_MANAGER_STATE_SETTINGS_KEY, logSwitch.isChecked());

		editor.apply();

		BluetoothConnexionManager.getInstance().initialize(settings);
		LogManager.setEnabled(logSwitch.isChecked());

		Toast.makeText(getContext(), "Settings stored successfuly", Toast.LENGTH_SHORT).show();
	}

	private void showDeviceSelectDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

		builder.setTitle("Select uCube device");

		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		final List<BluetoothDevice> deviceList = new ArrayList<>(adapter.getBondedDevices());
		Collections.sort(deviceList, new Comparator<BluetoothDevice>() {
			@Override
			public int compare(BluetoothDevice lhs, BluetoothDevice rhs) {
				String v1 = lhs.getName();
				if (StringUtils.isBlank(v1)) {
					v1 = lhs.getAddress();
				}

				String v2 = rhs.getName();
				if (StringUtils.isBlank(v1)) {
					v2 = rhs.getAddress();
				}

				return v1.compareToIgnoreCase(v2);
			}
		});

		final String[] labels = new String[deviceList.size()];
		for (int i = 0; i < deviceList.size(); i++) {
			labels[i] = deviceList.get(i).getName();

			if (StringUtils.isEmpty(labels[i])) {
				labels[i] = deviceList.get(i).getAddress();
			}
		}

		builder.setItems(labels, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				selectedDevice = deviceList.get(which);

				BluetoothConnexionManager.getInstance().setDeviceAddr(selectedDevice.getAddress());

				UIUtils.showProgress(getContext(), "Check device model");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new GetInfosCommand(Constants.TAG_TERMINAL_SN, Constants.TAG_TERMINAL_PN, Constants.TAG_MPOS_MODULE_STATE).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								if (event == TaskEvent.PROGRESS) {
									return;
								}

								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											nfcEnabled = false;
											deviceSerial = null;
											devicePartNUMber = null;
											UIUtils.showMessageDialog(getContext(), "Unable to connect to device !\nNFC will be disabled.");
											break;

										case SUCCESS:
											DeviceInfos deviceInfos = new DeviceInfos(((GetInfosCommand) params[0]).getResponseData());
											nfcEnabled = deviceInfos != null && deviceInfos.getNfcModuleState() != 0;
											PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(PaymentFragment.NFC_ENABLE_DEVICE_SETTINGS_KEY, nfcEnabled).apply();
											deviceSerial = deviceInfos.getSerial();
											devicePartNUMber = deviceInfos.getPartNumber();

											break;
										}

										UIUtils.hideProgressDialog();
									}
								});
							}
						});
					}
				}).start();

				deviceFld.setText(labels[which]);
			}
		});

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.create().show();
	}

}
