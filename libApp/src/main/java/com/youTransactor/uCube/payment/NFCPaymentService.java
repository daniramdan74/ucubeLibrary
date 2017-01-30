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
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.rpc.command.CompleteNFCTransactionCommand;
import com.youTransactor.uCube.rpc.command.GetPlainTagCommand;
import com.youTransactor.uCube.rpc.command.GetSecuredTagCommand;

/**
 * @author gbillard on 5/31/16.
 */
public class NFCPaymentService extends AbstractPaymentService {

	public NFCPaymentService(PaymentContext context) {
		super(context);
	}

	@Override
	protected void start() {
		switch (context.getNFCOutcome()[1]) {
		case 0x36:
			end(PaymentState.APPROVED);
			break;

		case 0x3E:
			getSecuredTag();
			break;

		case 0x31:
			//TODO what to do for fallback
			break;

		case 0x3A:
			end(PaymentState.CANCELLED);
			break;

		case 0x37:
			end(PaymentState.DECLINED);
			break;

		case 0x3F:
		case 0x38:
		default:
			end(PaymentState.ERROR);
			break;
		}
	}


	private void getSecuredTag() {
		LogManager.debug(this.getClass().getSimpleName(), "getSecuredTag");

		final GetSecuredTagCommand cmd = new GetSecuredTagCommand(context.getRequestedSecuredTagList());
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					end(PaymentState.ERROR);
					break;

				case SUCCESS:
					context.setSecuredTagBlock(cmd.getResponseData());
					getPlainTag();
					break;
				}
			}
		});
	}

	private void getPlainTag() {
		LogManager.debug(this.getClass().getSimpleName(), "getPlainTag");

		final GetPlainTagCommand cmd = new GetPlainTagCommand(context.getRequestedPlainTagList());
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					end(PaymentState.ERROR);
					break;

				case SUCCESS:
					context.setPlainTagTLV(cmd.getResult());
					doAuthorization();
					break;
				}
			}
		});
	}

	@Override
	public void onAuthorizationDone() {
		CompleteNFCTransactionCommand cmd = new CompleteNFCTransactionCommand(context.getAuthorizationResponse());
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = (ITask) params[0];
					notifyMonitor(TaskEvent.FAILED);
					break;

				case SUCCESS:
					context.setNFCOutcome(((CompleteNFCTransactionCommand) params[0]).getNFCOutcome());

					switch (context.getNFCOutcome()[1]) {
					case 0x36:
						end(PaymentState.APPROVED);
						break;

					case 0x3A:
						end(PaymentState.CANCELLED);
						break;

					case 0x37:
						end(PaymentState.DECLINED);
						break;

					case 0x38:
					default:
						end(PaymentState.ERROR);
						break;
					}

					break;
				}
			}
		});
	}


	protected void displayMessage(String msg, ITaskMonitor callback) {
		if (callback != null) {
			callback.handleEvent(TaskEvent.SUCCESS);
		}
	}

}
