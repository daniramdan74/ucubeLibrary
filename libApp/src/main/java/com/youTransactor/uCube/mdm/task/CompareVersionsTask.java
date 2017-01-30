/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.mdm.task;

import android.support.annotation.NonNull;

import com.youTransactor.uCube.AbstractTask;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.mdm.Config;
import com.youTransactor.uCube.mdm.service.BinaryUpdate;
import com.youTransactor.uCube.rpc.DeviceInfos;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gbillard on 4/5/16.
 */
public class CompareVersionsTask extends AbstractTask {

	private DeviceInfos deviceInfos;
	private List<Config> configList;
	private List<BinaryUpdate> updateList;
	private boolean updateSameVersion;

	public CompareVersionsTask(@NonNull DeviceInfos deviceInfos, @NonNull List<Config> configList) {
		init(deviceInfos, configList);
	}

	public void setUpdateSameVersion(boolean updateSameVersion) {
		this.updateSameVersion = updateSameVersion;
	}

	@Override
	protected void start() {
		notifyMonitor(TaskEvent.SUCCESS, getUpdateList());
	}

	public void init(@NonNull DeviceInfos deviceInfos, @NonNull List<Config> configList) {
		this.deviceInfos = deviceInfos;
		this.configList = configList;
	}

	public List<BinaryUpdate> getUpdateList() {
		if (updateList == null) {
			updateList = new ArrayList<>();

			if (configList != null && deviceInfos != null) {
				for (Config cfg : configList) {
					compareVersion(cfg);
				}
			}
		}

		return updateList;
	}

	private void compareVersion(Config cfg) {
		int[] current = getComparableVersion(cfg.getType());

		if (current == null) {
			updateList.add(new BinaryUpdate(cfg, true));
			return;
		}

		int[] expected = convertToComparable(cfg.getCurrentVersion());

		int res = compare(current, expected);

		if (res > 0 || (res == 0 && !updateSameVersion)) {
			return;
		}

		expected = convertToComparable(cfg.getMinVersion());

		updateList.add(new BinaryUpdate(cfg, compare(current, expected) < 0));
	}

	private int compare(int[] current, int[] expected) {
		for (int i = 0; i < 4; i++) {
			if (current[i] < expected[i]) {
				return -1;
			}

			if (current[i] > expected[i]) {
				return 1;
			}
		}

		return 0;
	}

	private int[] getComparableVersion(int type) {
		switch (type) {
		case 0:
			return convertToComparable(deviceInfos.getSvppFirmware());

		case 3:
			return convertToComparable(deviceInfos.getNfcFirmware());

		case 4:
			return convertToComparable(deviceInfos.getIccEmvConfigVersion());

		case 5:
			return convertToComparable(deviceInfos.getNfcEmvConfigVersion());

		default:
			return null;
		}
	}

	private int[] convertToComparable(String version) {
		if (!StringUtils.isBlank(version)) {
			try {
				int index = 0;
				int[] res = new int[4];

				for (String v : version.split("\\.")) {
					res[index++] = Integer.valueOf(v).intValue();
				}

				return res;

			} catch (Exception e) {
				LogManager.debug(CompareVersionsTask.class.getSimpleName(), "invalid binary version: '" + version + "'", e);
			}
		}

		return null;
	}

}
