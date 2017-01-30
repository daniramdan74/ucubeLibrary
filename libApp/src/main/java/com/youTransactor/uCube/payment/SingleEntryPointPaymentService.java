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

import com.youTransactor.uCube.ITask;
import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.command.DisplayMessageCommand;
import com.youTransactor.uCube.rpc.command.EnterSecureSessionCommand;
import com.youTransactor.uCube.rpc.command.StartNFCTransactionCommand;

/**
 * @author gbillard on 5/31/16.
 */
public class SingleEntryPointPaymentService extends PaymentService {

	public SingleEntryPointPaymentService(PaymentContext paymentContext, byte[] enabledReaders) {
		super(paymentContext, enabledReaders);
	}

	@Override
	protected void onGetInfos() {
		final EnterSecureSessionCommand cmd = new EnterSecureSessionCommand();
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = (ITask) params[0];
					notifyMonitor(TaskEvent.FAILED);
					break;

				case SUCCESS:
					context.setKsn(cmd.getKsn());
					startTransaction();
					break;
				}
			}
		});
	}

	private void startTransaction() {
		final StartNFCTransactionCommand cmd = new StartNFCTransactionCommand(enabledReaders, context.getCurrency());

		cmd.setDate(context.getTransactionDate());
		cmd.setNoAmount(context.getAmount() < 0);
		cmd.setAmount(context.getAmount());
		cmd.setTimeout(cardWaitTimeout);
		cmd.setTransactionType(context.getTransactionType());

		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					switch (cmd.getResponseStatus()) {
					case Constants.CANCELLED_STATUS:
					case Constants.TIMEOUT_STATUS:
						end(PaymentState.CANCELLED);
						break;

					default:
						failedTask = cmd;
						notifyMonitor(TaskEvent.FAILED);
						break;
					}
					break;

				case CANCELLED:
					notifyMonitor(TaskEvent.CANCELLED);
					break;

				case SUCCESS:
					context.setActivatedReader(cmd.getActivatedReader());

					new DisplayMessageCommand(context.getString("LBL_wait")).execute(new ITaskMonitor() {
						@Override
						public void handleEvent(TaskEvent event, Object... params) {
							if (event == TaskEvent.PROGRESS) {
								return;
							}

							switch (context.getActivatedReader()) {
							case Constants.ICC_READER:
								paymentService = new ICCPaymentService(context);
								paymentService.setApplicationSelectionProcessor(applicationSelectionProcessor);
								break;

							case Constants.MS_READER:
								paymentService = new MSPaymentService(context);
								break;

							case Constants.NFC_READER:
								context.setNFCOutcome(cmd.getNFCOutcome());
								paymentService = new NFCPaymentService(context);
								break;

							default:
								failedTask = cmd;
								notifyMonitor(TaskEvent.FAILED);
								return;
							}

							if (context.getAmount() == -1) {
								context.setAmount(cmd.getAmount());
							}

							paymentService.setRiskManagementTask(riskManagementTask);
							paymentService.setAuthorizationProcessor(authorizationProcessor);

							context.setPaymentStatus(PaymentState.STARTED);
							notifyMonitor(TaskEvent.PROGRESS);

							paymentService.execute(monitor);
						}
					});
				}
			}
		});
	}

}
