/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.payment;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * @author gbillard on 5/19/16.
 */
public class TransactionTypeAdapter extends BaseAdapter {

	private TransactionType[] typeList = TransactionType.values();

	@Override
	public int getCount() {
		return typeList.length;
	}

	@Override
	public Object getItem(int position) {
		return typeList[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = new TextView(parent.getContext());
			convertView.setPadding(20, 20, 20, 20);
		}

		((TextView) convertView).setText(typeList[position].getLabel());

		return convertView;
	}

}
