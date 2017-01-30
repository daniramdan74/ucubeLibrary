/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p/>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.rpc;

import com.youTransactor.uCube.TLV;
import com.youTransactor.uCube.Tools;

import java.util.Map;

/**
 * @author gbillard on 4/5/16.
 */
public class DeviceInfos {

	private byte[] tlv;
	private String serial;
	private String partNumber;
	private String svppFirmware;
	private String emvL1Version;
	private String emvL2Version;
	private String iccEmvConfigVersion;
	private String nfcFirmware;
	private String nfcEmvL1Version;
	private String nfcEmvL2Version;
	private String nfcEmvConfigVersion;
	private int nfcModuleState;

	public DeviceInfos() {}

	public DeviceInfos(String serial, String partNUmber) {
		this.serial = serial;
		this.partNumber = partNUmber;
	}

	public DeviceInfos(byte[] tlv) {
		init(tlv);
	}

	public byte[] getTlv() {
		return tlv;
	}

	public String getSerial() {
		return serial;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public String getSvppFirmware() {
		return svppFirmware;
	}

	public String getEmvL1Version() {
		return emvL1Version;
	}

	public String getEmvL2Version() {
		return emvL2Version;
	}

	public String getIccEmvConfigVersion() {
		return iccEmvConfigVersion;
	}

	public String getNfcFirmware() {
		return nfcFirmware;
	}

	public String getNfcEmvL1Version() {
		return nfcEmvL1Version;
	}

	public String getNfcEmvL2Version() {
		return nfcEmvL2Version;
	}

	public String getNfcEmvConfigVersion() {
		return nfcEmvConfigVersion;
	}

	public int getNfcModuleState() {
		return nfcModuleState;
	}

	private void init(byte[] tlv) {
		this.tlv = tlv;

		if (tlv == null || tlv.length == 0) {
			return;
		}

		Map<Integer, byte[]> valueByTag = TLV.parseYtBerMixedLen(tlv);

		serial = Tools.parseSerial(valueByTag.get(Integer.valueOf(Constants.TAG_TERMINAL_SN)));

		partNumber = Tools.parsePartNumber(valueByTag.get(Integer.valueOf(Constants.TAG_TERMINAL_PN)));

		svppFirmware = Tools.parseVersion(valueByTag.get(Integer.valueOf(Constants.TAG_FIRMWARE_VERSION)));

		emvL1Version = Tools.parseVersion(valueByTag.get(Integer.valueOf(Constants.TAG_EMV_L1_VERSION)));

		emvL2Version = Tools.parseVersion(valueByTag.get(Integer.valueOf(Constants.TAG_EMV_L2_VERSION)));

		iccEmvConfigVersion = Tools.parseVersion(valueByTag.get(Integer.valueOf(Integer.valueOf(Constants.TAG_EMV_ICC_CONFIG_VERSION))));

		if (valueByTag.containsKey(Integer.valueOf(Constants.TAG_MPOS_MODULE_STATE))) {
			nfcModuleState = valueByTag.get(Integer.valueOf(Constants.TAG_MPOS_MODULE_STATE))[0];
		}

		Map<Integer, byte[]> nfcInfos = TLV.parseYtBerMixedLen(valueByTag.get(Integer.valueOf(Constants.TAG_NFC_INFOS)));

		if (nfcInfos == null) {
			return;
		}

		nfcFirmware = Tools.parseVersion(nfcInfos.get(Integer.valueOf(Constants.TAG_FIRMWARE_VERSION)));

		byte[] v = nfcInfos.get(Integer.valueOf(Constants.TAG_EMV_L1_NFC_VERSION));
		if (v != null && v.length > 4) {
			byte[] v2 = new byte[4];
			System.arraycopy(v, 0, v2, 0, 4);

			nfcEmvL1Version = Tools.parseVersion(v2);
		}

		nfcEmvL2Version = Tools.parseVersion(nfcInfos.get(Integer.valueOf(Constants.TAG_EMV_L2_NFC_VERSION)));

		nfcEmvConfigVersion = Tools.parseVersion(valueByTag.get(Integer.valueOf(Constants.TAG_EMV_NFC_CONFIG_VERSION)));
	}

}
