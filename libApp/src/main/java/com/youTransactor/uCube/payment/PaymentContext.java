/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */

package com.youTransactor.uCube.payment;

import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.EMVApplicationDescriptor;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author gbillard on 5/19/16.
 */
public class PaymentContext {

	private PaymentState paymentStatus;
	private EMVApplicationDescriptor selectedApplication;
	private boolean allowFallback;
	private int retryBeforeFallback = 3;
	private double amount = -1;
	private Currency currency;
	private byte transactionType;
	private int applicationVersion;
	private List<String> preferredLanguageList;
	private byte[] uCubeInfos;
	private byte[] ksn;
	private byte activatedReader;
	private boolean forceOnlinePIN;
	private boolean forceAuthorization;
	private byte onlinePinBlockFormat = Constants.PIN_BLOCK_ISO9564_FORMAT_0;
	private int[] requestedPlainTagList;
	private int[] requestedSecuredTagList;
	private List<byte[]> requestedAuthorizationTagList;
	private byte[] securedTagBlock;
	private byte[] onlinePinBlock;
	private Map<Integer, byte[]> plainTagTLV;
	private byte[] authorizationResponse;
	private byte[] tvr = new byte[] {0, 0, 0, 0, 0};
	private Date transactionDate;
	private byte[] NFCOutcome;
	private byte[] transactionData;
	private ResourceBundle msgBundle;

	public PaymentContext() {}

	public PaymentContext(double amount, Currency currency, byte transactionType) {
		setAmount(amount);
		setCurrency(currency);
		setTransactionType(transactionType);
	}

	protected String getFormatedAmount() {
		if (currency.getExponent() == 0) {
			return Integer.toString((int) amount);
		}

		StringBuilder pat = new StringBuilder(10);
		pat.append("{0,number,#0.0");
		for (int i = 1; i < currency.getExponent(); i++) {
			pat.append('0');
		}
		pat.append('}');

		return MessageFormat.format(pat.toString(), amount);
	}

	public String getString(String key) {
		if (msgBundle != null && msgBundle.containsKey(key)) {
			return msgBundle.getString(key);
		}

		return key;
	}

	public EMVApplicationDescriptor getSelectedApplication() {
		return selectedApplication;
	}

	public void setSelectedApplication(EMVApplicationDescriptor selectedApplication) {
		this.selectedApplication = selectedApplication;
	}

	public int getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(int applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public boolean isAllowFallback() {
		return allowFallback;
	}

	public void setAllowFallback(boolean allowFallback) {
		this.allowFallback = allowFallback;
	}

	public int getRetryBeforeFallback() {
		return retryBeforeFallback;
	}

	public void setRetryBeforeFallback(int retryBeforeFallback) {
		this.retryBeforeFallback = retryBeforeFallback;
	}

	public byte getActivatedReader() {
		return activatedReader;
	}

	public void setActivatedReader(byte activatedReader) {
		this.activatedReader = activatedReader;
	}

	public PaymentState getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentState paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		if (amount >= 0) {
			this.amount = amount;
		}
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public byte getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(byte transactionType) {
		this.transactionType = transactionType;
	}

	public byte[] getuCubeInfos() {
		return uCubeInfos;
	}

	public void setuCubeInfos(byte[] uCubeInfos) {
		this.uCubeInfos = uCubeInfos;
	}

	public byte[] getKsn() {
		return ksn;
	}

	public void setKsn(byte[] ksn) {
		this.ksn = ksn;
	}

	public boolean isForceOnlinePIN() {
		return forceOnlinePIN;
	}

	public void setForceOnlinePIN(boolean forceOnlinePIN) {
		this.forceOnlinePIN = forceOnlinePIN;
	}

	public byte getOnlinePinBlockFormat() {
		return onlinePinBlockFormat;
	}

	public void setOnlinePinBlockFormat(byte onlinePinBlockFormat) {
		this.onlinePinBlockFormat = onlinePinBlockFormat;
	}

	public int[] getRequestedPlainTagList() {
		return requestedPlainTagList;
	}

	public void setRequestedPlainTagList(int[] requestedPlainTagList) {
		this.requestedPlainTagList = requestedPlainTagList;
	}

	public int[] getRequestedSecuredTagList() {
		return requestedSecuredTagList;
	}

	public void setRequestedSecuredTagList(int[] requestedSecuredTagList) {
		this.requestedSecuredTagList = requestedSecuredTagList;
	}

	public byte[] getSecuredTagBlock() {
		return securedTagBlock;
	}

	public void setSecuredTagBlock(byte[] securedTagBlock) {
		this.securedTagBlock = securedTagBlock;
	}

	public byte[] getOnlinePinBlock() {
		return onlinePinBlock;
	}

	public void setOnlinePinBlock(byte[] onlinePinBlock) {
		this.onlinePinBlock = onlinePinBlock;
	}

	public Map<Integer, byte[]> getPlainTagTLV() {
		return plainTagTLV;
	}

	public void setPlainTagTLV(Map<Integer, byte[]> plainTagTLV) {
		this.plainTagTLV = plainTagTLV;
	}

	public byte[] getAuthorizationResponse() {
		return authorizationResponse;
	}

	public void setAuthorizationResponse(byte[] response) {
		authorizationResponse = response;
	}

	public List<String> getPreferredLanguageList() {
		return preferredLanguageList;
	}

	public void setPreferredLanguageList(List<String> preferredLanguageList) {
		this.preferredLanguageList = preferredLanguageList;
	}

	public List<byte[]> getRequestedAuthorizationTagList() {
		return requestedAuthorizationTagList;
	}

	public void setRequestedAuthorizationTagList(List<byte[]> requestedAuthorizationTagList) {
		this.requestedAuthorizationTagList = requestedAuthorizationTagList;
	}

	public boolean isForceAuthorization() {
		return forceAuthorization;
	}

	public void setForceAuthorization(boolean forceAuthorization) {
		this.forceAuthorization = forceAuthorization;

		if (forceAuthorization) {
			tvr[3] |= 0b1000;
		} else {
			tvr[3] &= 0b11110111;
		}
	}

	public ResourceBundle getMsgBundle() {
		return msgBundle;
	}

	public void setMsgBundle(ResourceBundle msgBundle) {
		this.msgBundle = msgBundle;
	}

	public byte[] getTvr() {
		return tvr;
	}

	public void setTvr(byte[] tvr) {
		if (tvr instanceof byte[] && tvr.length == 5) {
			for (int i = 0; i < 5; i++) {
				this.tvr[i] = tvr[i];
			}
		}

		setForceAuthorization(forceAuthorization);
	}

	public byte[] getTransactionData() {
		return transactionData;
	}

	public void setTransactionData(byte[] transactionData) {
		this.transactionData = transactionData;
	}

	public byte[] getNFCOutcome() {
		return NFCOutcome;
	}

	public void setNFCOutcome(byte[] NFCOutcome) {
		this.NFCOutcome = NFCOutcome;
	}

}
