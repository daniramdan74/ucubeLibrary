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
public enum TransactionType {
	DEBIT("Debit", Constants.DEBIT),
	WITHDRAWAL("Withdrawal", Constants.WITHDRAWAL),
	REFUND("Refund", Constants.REFUND),
	PURCHASE_CASHBACK("Purchase cashback", Constants.PURCHASE_CASHBACK),
	CANCELLATION("Cancellation", Constants.CANCELLATION),
	MANUAL_CASH("Manual cash", Constants.MANUAL_CASH),
	CREDIT("Credit", Constants.CREDIT),
	EMV_CVM_CONDITION_CASH("EMV CVM cash", Constants.EMV_CVM_CONDITION_CASH),
	INQUIRY("Inquiry", Constants.INQUIRY);

	private byte code;
	private String label;

	TransactionType(String label, byte code) {
		this.label = label;
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public byte getCode() {
		return code;
	}
}
