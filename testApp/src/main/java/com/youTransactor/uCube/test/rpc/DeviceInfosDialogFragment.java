/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p/>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.rpc;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.youTransactor.uCube.test.R;
import com.youTransactor.uCube.rpc.DeviceInfos;

/**
 * @author gbillard on 3/31/16.
 */
public class DeviceInfosDialogFragment extends DialogFragment {

	private DeviceInfos deviceInfos;
	private View view;

	public void init(DeviceInfos deviceInfos) {
		this.deviceInfos = deviceInfos;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (view != null) {
			return super.onCreateView(inflater, container, savedInstanceState);
		}

		initView(inflater, container);

		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		initView(LayoutInflater.from(getContext()), null);

		return new AlertDialog.Builder(getContext())
				.setView(view)
				.setTitle("Device infos")
				.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								dialog.dismiss();
							}
						}
				)
				.create();
	}

	private void initView(LayoutInflater inflater, ViewGroup container) {
		view = inflater.inflate(R.layout.device_infos_dialog, container, false);

		((TextView) view.findViewById(R.id.serialFld)).setText(deviceInfos.getSerial());

		((TextView) view.findViewById(R.id.partNumberFld)).setText(deviceInfos.getPartNumber());

		((TextView) view.findViewById(R.id.svppVersionFld)).setText(deviceInfos.getSvppFirmware());

		((TextView) view.findViewById(R.id.emvL1VersionFld)).setText(deviceInfos.getEmvL1Version());

		((TextView) view.findViewById(R.id.emvL2VersionFld)).setText(deviceInfos.getEmvL2Version());

		((TextView) view.findViewById(R.id.iccEmvCfgFld)).setText(deviceInfos.getIccEmvConfigVersion());

		((TextView) view.findViewById(R.id.nfcFirmwareFld)).setText(deviceInfos.getNfcFirmware());

		((TextView) view.findViewById(R.id.nfcEmvL1Fld)).setText(deviceInfos.getNfcEmvL1Version());

		((TextView) view.findViewById(R.id.nfcEmvL2Fld)).setText(deviceInfos.getNfcEmvL2Version());

		((TextView) view.findViewById(R.id.nfcEmvCfgFld)).setText(deviceInfos.getNfcEmvConfigVersion());
	}

}
