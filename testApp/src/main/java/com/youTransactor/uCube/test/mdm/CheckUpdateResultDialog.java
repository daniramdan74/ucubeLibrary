/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.mdm;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.TextView;

import com.youTransactor.uCube.mdm.service.BinaryUpdate;
import com.youTransactor.uCube.test.rpc.DeviceConfigAdapter;

import java.util.List;

/**
 * @author gbillard on 5/26/16.
 */
public class CheckUpdateResultDialog extends DialogFragment {

	private List<BinaryUpdate> updateList;
	private View view;

	public void init(List<BinaryUpdate> updateList) {
		this.updateList = updateList;
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

		return new AlertDialog.Builder(getActivity())
				.setView(view)
				.setTitle("Device configuration")
				.setPositiveButton("Ok", null)
				.create();
	}

	private void initView(LayoutInflater inflater, ViewGroup container) {
		if (updateList != null && updateList.size() > 0) {
			GridLayout gridView = new GridLayout(getContext());
			gridView.setColumnCount(2);
			gridView.setRowCount(updateList.size());

			for (BinaryUpdate update : updateList) {
				TextView txtFld = new TextView(getContext());
				txtFld.setPadding(10, 10, 10, 10);
				txtFld.setText(update.getCfg().getLabel());

				gridView.addView(txtFld);

				txtFld = new TextView(getContext());
				txtFld.setPadding(10, 10, 10, 10);
				txtFld.setText(update.getCfg().getCurrentVersion());

				if (update.isMandatory()) {
					txtFld.append(" (mandatory)");
				}

				gridView.addView(txtFld);
			}

			view = gridView;
		}
	}
}
