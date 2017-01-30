/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.youTransactor.uCube.test.mdm.MDMFragment;
import com.youTransactor.uCube.test.payment.PaymentFragment;
import com.youTransactor.uCube.test.rpc.RPCFragment;
import com.youTransactor.uCube.test.settings.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gbillard on 3/29/16.
 */
public class MainPagerAdapater extends FragmentPagerAdapter {

	private List<Fragment> panelList;
	private List<String> titleList;

	public MainPagerAdapater(FragmentManager fm) {
		super(fm);

		panelList = new ArrayList<>();
		titleList = new ArrayList<>();

		panelList.add(new SettingsFragment());
		titleList.add("Settings");

		panelList.add(new RPCFragment());
		titleList.add("RPC");

		panelList.add(new MDMFragment());
		titleList.add("MDM");

		panelList.add(new PaymentFragment());
		titleList.add("Payment");
	}

	@Override
	public Fragment getItem(int position) {
		return panelList.get(position);
	}

	@Override
	public int getCount() {
		return panelList.size();
	}

	public String getTabTitle(int position) {
		return titleList.get(position);
	}

}
