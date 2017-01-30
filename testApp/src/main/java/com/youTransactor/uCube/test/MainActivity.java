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

import android.bluetooth.BluetoothAdapter;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.mdm.MDMManager;
import com.youTransactor.uCube.rpc.RPCManager;

import io.fabric.sdk.android.Fabric;

/**
 * @author gbillard on 3/11/16.
 */
public class MainActivity extends AppCompatActivity {

	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());

		setContentView(R.layout.activity_main);

		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		getBaseContext().registerReceiver(BluetoothConnexionManager.getInstance(), filter);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		LogManager.initialize(this);
		BluetoothConnexionManager.getInstance().initialize(settings);

		MDMManager.getInstance().initialize(this);

		RPCManager.getInstance().setConnexionManager(BluetoothConnexionManager.getInstance());

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		MainPagerAdapater pagerAdapter = new MainPagerAdapater(getSupportFragmentManager());

		tabLayout.addTab(tabLayout.newTab().setText(pagerAdapter.getTabTitle(0)));
		tabLayout.addTab(tabLayout.newTab().setText(pagerAdapter.getTabTitle(1)));
		tabLayout.addTab(tabLayout.newTab().setText(pagerAdapter.getTabTitle(2)));
		tabLayout.addTab(tabLayout.newTab().setText(pagerAdapter.getTabTitle(3)));

		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(pagerAdapter);

		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

		tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {}
		});

		if (BluetoothConnexionManager.getInstance().isInitialized()) {
			viewPager.setCurrentItem(3);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_exit:
			MainActivity.this.finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void finish() {
		RPCManager.getInstance().stop();
		BluetoothConnexionManager.getInstance().disconnect();
		getBaseContext().unregisterReceiver(BluetoothConnexionManager.getInstance());

		super.finish();
	}

	public static final String DEVICE_MAC_ADDR_SETTINGS_KEY = "BT_deviceMacAddr";
	public static final String DEVICE_NAME_SETTINGS_KEY = "BT_deviceName";

}
