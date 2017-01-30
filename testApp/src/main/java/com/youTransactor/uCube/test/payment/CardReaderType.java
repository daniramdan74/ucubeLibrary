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

import com.youTransactor.uCube.rpc.Constants;

/**
 * @author gbillard on 5/19/16.
 */
public enum CardReaderType {
	MSR(Constants.MS_READER, "Magstripe"),
	ICC(Constants.ICC_READER, "Chip"),
	NFC(Constants.NFC_READER, "NFC");

	private byte code;
	private String label;

	CardReaderType(byte code, String label) {
		this.code = code;
		this.label = label;
	}

	public byte getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

}
