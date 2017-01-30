/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.rpc;

import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.Tools;
import com.youTransactor.uCube.rpc.command.EnterSecureSessionCommand;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gbillard on 3/11/16.
 */
public class RPCManager {

	private IConnexionManager connexionManager;
	private OutputStream out;
	private boolean secureSession = false;
	private byte sequenceNumber = 0;
	private Map<Short, IRPCMessageHandler> messageHandlerByCommandId = new HashMap<>();

	private RPCManager() {}

	public synchronized void sendCommand(RPCCommand command) {
		if (out == null) {
			if (connexionManager == null || !connexionManager.connect()) {
				LogManager.debug(RPCManager.class.getSimpleName(), "unable to connect to device");

				command.setState(RPCCommandStatus.CONNECT_ERROR);

				return;
			}
		}

		LogManager.debug(RPCManager.class.getSimpleName(), "send command ID: 0x" + Integer.toHexString(command.getCommandId()));
		LogManager.debug(RPCManager.class.getSimpleName(), "send command data: 0x" + Tools.bytesToHex(command.getPayload()));

		messageHandlerByCommandId.put(command.getCommandId(), command);

		command.setState(RPCCommandStatus.SENDING);

		try {
            /* reset sequence number before entering in secure session */
            if(command.getCommandId() == Constants.ENTER_SECURED_SESSION) {
                sequenceNumber = 0;
            }

			if (secureSession && (command.getCommandId() != Constants.EXIT_SECURED_SESSION)) {
				sendSecureCommand(command);
			} else {
				sendInsecureCommand(command);
			}

			out.flush();

			LogManager.debug(RPCManager.class.getSimpleName(), "sent command ID: 0x" + Integer.toHexString(command.getCommandId()));

			command.setState(RPCCommandStatus.SENT);

		} catch (Exception e) {
			LogManager.debug(RPCManager.class.getName(), "send command error for Id: " + Integer.toHexString(command.getCommandId()), e);

			messageHandlerByCommandId.remove(command.getCommandId());

			command.setState(RPCCommandStatus.FAILED);
		}
	}

	public void start(InputStream in, OutputStream out) {
		this.out = out;

		DAEMON = new RPCDaemon(in);

		new Thread(DAEMON).start();

		if (secureSession) {
			new EnterSecureSessionCommand().execute(null);
		}
	}

	public void stop() {
		if (DAEMON != null) {
			DAEMON.stop();
		}

		out = null;

		for (IRPCMessageHandler handler : messageHandlerByCommandId.values()) {
			handler.processMessage(null);
		}

		messageHandlerByCommandId.clear();
	}

	public boolean isReady() {
		return out != null;
	}

	public boolean connect() {
		return connexionManager != null &&  connexionManager.connect();
	}

	public void setConnexionManager(IConnexionManager connexionManager) {
		this.connexionManager = connexionManager;
	}

	private void sendSecureCommand(RPCCommand command) throws IOException {
		byte[] payload = command.getPayload();

		if (payload.length == 0) {
			sendInsecureCommand(command);
			return;
		}

        byte[] message = new byte[payload.length + Constants.RPC_SECURED_HEADER_LEN + Constants.RPC_SRED_MAC_SIZE];
		int securedLen = payload.length + Constants.RPC_SECURED_HEADER_CRYPTO_RND_LEN + Constants.RPC_SRED_MAC_SIZE;

        int offset = 0;

        message[offset++] = (byte) (securedLen / 0x100);
        message[offset++] = (byte) (securedLen % 0x100);
        message[offset++] = sequenceNumber++;
        message[offset++] = (byte) (command.getCommandId() / 0x100);
        message[offset++] = (byte) (command.getCommandId() % 0x100);

        message[offset++] = (byte) 0x7F; // TODO should be random

        System.arraycopy(payload, 0, message, offset, payload.length);
        offset += payload.length;

		/* Padding the last 4 bytes with 0x00 (SRED OPT) */
		for (int i = 0; i < Constants.RPC_SRED_MAC_SIZE; i++){
            message[offset++] = 0x00;
		}

        int crc = computeChecksumCRC16(message);

        out.write(Constants.STX);
        IOUtils.write(message, out);
        out.write((byte) (crc / 0x100));
        out.write((byte) (crc % 0x100));
        out.write(Constants.ETX);

        LogManager.debug(RPCManager.class.getSimpleName(), "sent secure message: 0x" + Tools.bytesToHex(message));
	}

	private void sendInsecureCommand(RPCCommand command) throws IOException {
		byte[] payload = command.getPayload();

		byte[] message = new byte[payload.length + Constants.RPC_HEADER_LEN];
		int offset = 0;

		message[offset++] = (byte) (payload.length / 0x100);
		message[offset++] = (byte) (payload.length % 0x100);
		message[offset++] = sequenceNumber++;
		message[offset++] = (byte) (command.getCommandId() / 0x100);
		message[offset++] = (byte) (command.getCommandId() % 0x100);

		System.arraycopy(payload, 0, message, offset, payload.length);

		int crc = computeChecksumCRC16(message);

		out.write(Constants.STX);
		IOUtils.write(message, out);
		out.write((byte) (crc / 0x100));
		out.write((byte) (crc % 0x100));
		out.write(Constants.ETX);

		LogManager.debug(RPCManager.class.getSimpleName(), "sent message: 0x" + Tools.bytesToHex(message));
	}

	private void processMessage(RPCMessage message) {
		LogManager.debug(RPCManager.class.getSimpleName(), "received command ID: 0x" + Integer.toHexString(message.getCommandId()));
		LogManager.debug(RPCManager.class.getSimpleName(), "received command Status: 0x" + Integer.toHexString(message.getStatus()));
		LogManager.debug(RPCManager.class.getSimpleName(), "received command data: 0x" + Tools.bytesToHex(message.getData()));

		if (message.getStatus() == Constants.SUCCESS_STATUS) {
			if (message.getCommandId() == Constants.ENTER_SECURED_SESSION) {
                /* Set Secure Session State*/
				secureSession = true;
			} else if (message.getCommandId() == Constants.EXIT_SECURED_SESSION) {
				secureSession = false;
			}
		}

		IRPCMessageHandler handler = messageHandlerByCommandId.remove(message.getCommandId());

		if (handler == null && messageHandlerByCommandId.size() == 1) {
			/* command ID is 0x8000 in case of error => unable to recognize command */
			handler = messageHandlerByCommandId.values().iterator().next();
			messageHandlerByCommandId.clear();
		}

		if (handler != null) {
			try {
				handler.processMessage(message);
			} catch (Exception e) {
				LogManager.debug(RPCManager.class.getSimpleName(), "RPC command response process error", e);
			}
		}
	}

	public static RPCManager getInstance() {
		return INSTANCE;
	}

	/**
	 * this function to calculate the checksum 16bit
	 *
	 * @param bytes the payload data
	 * @return the calculated CRC16
	 */
	private static int computeChecksumCRC16(byte bytes[]) {
		int crc = 0x0000;
		int temp;
		int crc_byte;

		for (int byte_index = 0; byte_index < bytes.length; byte_index++) {

			crc_byte = bytes[byte_index];

			if (crc_byte < 0)
				crc_byte += 256;

			for (int bit_index = 0; bit_index < 8; bit_index++) {

				temp = (crc >> 15) ^ (crc_byte >> 7);

				crc <<= 1;
				crc &= 0xFFFF;

				if (temp > 0) {
					crc ^= 0x1021;
					crc &= 0xFFFF;
				}

				crc_byte <<= 1;
				crc_byte &= 0xFF;

			}
		}

		return crc;
	}

	/**
	 * The size of the biggest packet is the LOAD command and its 2052
	 * 2040(block) + 2(length) + 1(isLastBlock) + 2(command_ID) + 7(RPC Headers ETX,STX,SEQ,CRC,PLL) = 2052
	 */
	public static final int MAX_RPC_PACKET_SIZE = 2068;

	private static final RPCManager INSTANCE = new RPCManager();
	private static RPCDaemon DAEMON;

	private static class RPCDaemon implements Runnable {

		private InputStream in;
		private boolean interrupted = false;

		private RPCDaemon(InputStream in) {
			this.in = in;
		}

		private void stop() {
			interrupted = true;
		}

		@Override
		public void run() {
			byte[] bufferFromStream = new byte[MAX_RPC_PACKET_SIZE];
			byte[] bufferToDeliver = new byte[MAX_RPC_PACKET_SIZE];

			int nb_bytes;
			boolean isPacketComplete = false;
			boolean waiting_for_first_packet = true;
			short expected_length = 0;
			short bufferToDeliverOffset = 0;

			while (!interrupted) {
				try {
					nb_bytes = in.read(bufferFromStream);

					if (nb_bytes > 0) {
						if (waiting_for_first_packet) {
							if (bufferFromStream[0] != Constants.STX) {
								isPacketComplete = false;
								waiting_for_first_packet = true;

								continue;
							}

							expected_length = Tools.makeShort(bufferFromStream[1], bufferFromStream[2]);
							expected_length += 1 + 2 + 1 + 2 + 3; /* ETX,CRC,STX,CMDID,LENGTH */

							if (expected_length > MAX_RPC_PACKET_SIZE) {
								isPacketComplete = false;
								waiting_for_first_packet = true;

							} else {
								waiting_for_first_packet = false;
							}

							bufferToDeliverOffset = 0;
						}

						System.arraycopy(bufferFromStream, 0, bufferToDeliver, bufferToDeliverOffset, nb_bytes);
						bufferToDeliverOffset += nb_bytes;
					}

					if (bufferToDeliverOffset == expected_length) {
						if (bufferToDeliver[expected_length - 1] != Constants.ETX) {
							continue;
						}

						isPacketComplete = true;
						bufferFromStream = new byte[MAX_RPC_PACKET_SIZE];
					}

					if (isPacketComplete) {
						waiting_for_first_packet = true;
						isPacketComplete = false;

						LogManager.debug(RPCManager.class.getSimpleName(), "received: " + Tools.bytesToHex(bufferToDeliver));

						RPCMessage response = new RPCMessage(bufferToDeliver, INSTANCE.secureSession);

						/* Reset the buffer to the next message to avoid overwriting problems */
						Arrays.fill(bufferToDeliver, (byte) 0);

						INSTANCE.processMessage(response);
					}

				} catch (Exception e) {
					if (!interrupted) {
						if (e instanceof IOException) {
							LogManager.debug(RPCManager.class.getName(), "socket closed");
						} else {
							LogManager.debug(RPCManager.class.getName(), "RPC socket read  error", e);
						}

						INSTANCE.stop();
					}
				}
			}

			DAEMON = null;
		}
	}

}
