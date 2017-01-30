/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p/>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.rpc.command;

import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.RPCCommand;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by gbillard on 3/22/16.
 */
public class DisplayMessageCommand extends RPCCommand {

	private byte timeout = 0x00;
	private byte abortKey = 0x00;
	private byte clearConfig = 0x00;
	private byte centered = 0x00;
	private String text;

	public DisplayMessageCommand() {
		super(Constants.DISPLAY_WITHOUT_KI_COMMAND);
	}

	public DisplayMessageCommand(String message) {
		this();

		setText(message);
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setCentered(byte centered) {
		this.centered = centered;
	}

	public void setClearConfig(byte clearConfig) {
		this.clearConfig = clearConfig;
	}

	public void setAbortKey(byte abortKey) {
		this.abortKey = abortKey;
	}

	public void setTimeout(byte timeout) {
		this.timeout = timeout;
	}

	@Override
	protected byte[] createPayload() {
		byte textBytes[] = Charset.forName("UTF-8").encode(CharBuffer.wrap(text)).array();
		/* text max length is 254 */
		byte textLen = (byte) Math.min(text.length(), 254);

		/**
		 * (textLen + 9) = timeout + abort_key + clear configuration  + number of lines + Font + Length
		 * 		+ x-Coordinate + y-Coordinate + lasting (last byt in the text ) 0x00
		 */
		byte[] buffer = new byte[textLen + 9];

		int offset = 0;

		buffer[offset++] = timeout;

		buffer[offset++] = abortKey;

		buffer[offset++] = clearConfig;

		/* Number of lines */
		buffer[offset++] = 0x01;

		/* Line description: Font */
		buffer[offset++] = 0x00;

		buffer[offset++] = (byte) (textLen + 1);

		System.arraycopy(textBytes, 0, buffer, offset, textLen);
		offset += textLen;

		/* Lasting 0x00 */
		buffer[offset++] = 0;

		/* Coordinates */
		buffer[offset++] = centered;
		buffer[offset++] = 0;

		return buffer;
	}

}
