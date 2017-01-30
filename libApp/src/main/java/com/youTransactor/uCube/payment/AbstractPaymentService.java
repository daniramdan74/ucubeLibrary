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

import android.util.Log;

import com.youTransactor.uCube.AbstractService;
import com.youTransactor.uCube.ITask;
import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TLV;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.command.DisplayMessageCommand;
import com.youTransactor.uCube.rpc.command.ExitSecureSessionCommand;
import com.youTransactor.uCube.rpc.command.GetInfosCommand;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author gbillard on 5/19/16.
 */
abstract public class AbstractPaymentService extends AbstractService implements IPaymentTask {

	protected PaymentContext context;
	protected IApplicationSelectionTask applicationSelectionProcessor;
	protected IRiskManagementTask riskManagementTask;
	protected IAuthorizationTask authorizationProcessor;
	protected boolean authorizationDone;

	public AbstractPaymentService(PaymentContext context) {
		this.context = context;
	}

	protected void riskManagement() {
		LogManager.debug(this.getClass().getSimpleName(), "riskManagement");

		if (riskManagementTask == null) {
			onRiskManagementDone();
			return;
		}

		riskManagementTask.setContext(context);
		riskManagementTask.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = riskManagementTask;
					notifyMonitor(TaskEvent.FAILED);
					break;

				case SUCCESS:
					context.setTvr(riskManagementTask.getTVR());
					onRiskManagementDone();
					break;
				}
			}
		});
	}

	protected void onRiskManagementDone() {
		processTransaction();
	}

	public void onAuthorizationDone() {
		LogManager.debug(this.getClass().getSimpleName(), "onAuthorizationResponse");

		Map<Integer, byte[]> authResponse = TLV.parse(context.getAuthorizationResponse());

		if (authResponse != null) {
			byte[] tag8a = authResponse.get(0x8A);

			if (TLV.equalValue(tag8a, new byte[] {0x30, 0x30})) {
				end(PaymentState.APPROVED);
				return;
			}
		}

		end(PaymentState.DECLINED);
	}

	public PaymentContext getContext() {
		return context;
	}

	public void setContext(PaymentContext context) {
		this.context = context;
	}

	public void setApplicationSelectionProcessor(IApplicationSelectionTask applicationSelectionProcessor) {
		this.applicationSelectionProcessor = applicationSelectionProcessor;
	}

	public void setRiskManagementTask(IRiskManagementTask riskManagementTask) {
		this.riskManagementTask = riskManagementTask;
	}

	public void setAuthorizationProcessor(IAuthorizationTask authorizationProcessor) {
		this.authorizationProcessor = authorizationProcessor;
	}

	protected void processTransaction() {
		LogManager.debug(this.getClass().getSimpleName(), "processTransaction");

		doAuthorization();
	}

	protected void doAuthorization() {
		LogManager.debug(this.getClass().getSimpleName(), "doAuthorization");

		authorizationDone = true;

		if (authorizationProcessor == null) {
			onAuthorizationDone();
			return;
		}

		context.setPaymentStatus(PaymentState.AUTHORIZE);

		displayMessage(context.getString("LBL_authorization"), new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = (ITask) params[0];
					end(PaymentState.ERROR);
					break;

				case SUCCESS:
					performAuthorization();
					break;
				}
			}
		});
	}

	protected void performAuthorization() {
		authorizationProcessor.setContext(context);

		authorizationProcessor.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = authorizationProcessor;
					notifyMonitor(TaskEvent.FAILED);
					break;

				case SUCCESS:
					context.setAuthorizationResponse(authorizationProcessor.getAuthorizationResponse());

					displayMessage(context.getString("LBL_wait"), new ITaskMonitor() {
						@Override
						public void handleEvent(TaskEvent event, Object... params) {
							if (event == TaskEvent.PROGRESS) {
								return;
							}

							onAuthorizationDone();
						}
					});
					break;
				}
			}
		});
	}

	protected void exitSecureSession() {
		LogManager.debug(this.getClass().getSimpleName(), "exitSecureSession");

		new ExitSecureSessionCommand().execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				if (event == TaskEvent.PROGRESS) {
					return;
				}

				displayResult(null);
			}
		});
	}

	protected void displayResult(ITaskMonitor monitor) {
		LogManager.debug(this.getClass().getSimpleName(), "displayResult");

		String msgKey;

		switch (context.getPaymentStatus()) {
		case APPROVED:
			msgKey = "LBL_approved";
			break;

		case CHIP_REQUIRED:
			msgKey = "LBL_use_chip";
			break;

		case UNSUPPORTED_CARD:
			msgKey = "LBL_unsupported_card";
			break;

		case REFUSED_CARD:
			msgKey = "LBL_refused_card";
			break;

		case CARD_WAIT_FAILED:
			msgKey = "LBL_no_card_detected";
			break;

		case CANCELLED:
			msgKey = "LBL_cancelled";
			break;

		default:
			msgKey = "LBL_declined";
			break;
		}

		displayMessage(context.getString(msgKey), monitor);
	}

	protected void end(final PaymentState state) {
		LogManager.debug(this.getClass().getSimpleName(), "end: " + state.name());

		context.setPaymentStatus(state);

		notifyMonitor(context.getPaymentStatus() != PaymentState.ERROR ? TaskEvent.SUCCESS : TaskEvent.FAILED);

		exitSecureSession();
	}

	protected void displayMessage(String msg, ITaskMonitor callback) {
		new DisplayMessageCommand(msg).execute(callback);
	}

	@Override
	protected void notifyMonitor(TaskEvent event, Object... params) {
		super.notifyMonitor(event, this);
	}

}
