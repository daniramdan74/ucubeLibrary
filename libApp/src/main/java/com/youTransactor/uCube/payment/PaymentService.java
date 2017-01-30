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
import com.youTransactor.uCube.rpc.command.EnterSecureSessionCommand;
import com.youTransactor.uCube.rpc.command.ExitSecureSessionCommand;
import com.youTransactor.uCube.rpc.command.GetInfosCommand;
import com.youTransactor.uCube.rpc.command.WaitCardCommand;

import java.text.MessageFormat;

/**
 * @author gbillard on 5/18/16.
 *
 * this class allow only ICC and MS card use.
 * Nor does it allow amount to be entered on uCube.
 * Use SingleEntryPointPaymentService if you need NFC or amount input on uCube device.
 */
public class PaymentService extends AbstractPaymentService {

	protected int cardWaitTimeout = 60;
	protected byte[] enabledReaders;
	protected AbstractPaymentService paymentService;

	public PaymentService(PaymentContext paymentContext, byte[] enabledReaders) {
		super(paymentContext);
		this.enabledReaders = enabledReaders;
	}

	public void setCardWaitTimeout(int cardWaitTimeout) {
		this.cardWaitTimeout = cardWaitTimeout;
	}

	@Override
	protected void start() {
		new ExitSecureSessionCommand().execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				if (event == TaskEvent.PROGRESS) {
					return;
				}

				displayMessage(context.getString("LBL_wait"), new ITaskMonitor() {
					@Override
					public void handleEvent(TaskEvent event, Object... params) {
						switch (event) {
						case FAILED:
							failedTask = (ITask) params[0];
							end(PaymentState.ERROR);
							break;

						case SUCCESS:
							getInfos();
							break;
						}
					}
				});
			}
		});
	}

	protected void getInfos() {
		new GetInfosCommand(Constants.TAG_FIRMWARE_VERSION).execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = (ITask) params[0];
					end(PaymentState.ERROR);
					break;

				case SUCCESS:
					context.setuCubeInfos(((GetInfosCommand) params[0]).getResponseData());

					onGetInfos();
					break;
				}
			}
		});
	}

	protected void onGetInfos() {
		waitCard();
	}

	protected void waitCard() {
		displayMessage(MessageFormat.format(context.getString("MSG_wait_card"), context.getCurrency().getLabel(), context.getFormatedAmount()), new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				if (event == TaskEvent.PROGRESS) {
					return;
				}

				new WaitCardCommand(enabledReaders, cardWaitTimeout).execute(new ITaskMonitor() {
					@Override
					public void handleEvent(TaskEvent event, Object... params) {
						WaitCardCommand cmd = (WaitCardCommand) params[0];

						switch (event) {
						case FAILED:
							if (cmd.getResponseStatus() != null && cmd.getResponseStatus().shortValue() == Constants.CANCELLED_STATUS) {
								end(PaymentState.CANCELLED);
							} else {
								end(PaymentState.CARD_WAIT_FAILED);
							}
							break;

						case SUCCESS:
							context.setActivatedReader(cmd.getActivatedReader());

							displayMessage(context.getString("LBL_wait"), new ITaskMonitor() {
								@Override
								public void handleEvent(TaskEvent event, Object... params) {
									if (event == TaskEvent.PROGRESS) {
										return;
									}

									enterSecureMode();
								}
							});
							break;
						}
					}
				});
			}
		});
	}

	private void enterSecureMode() {
		final EnterSecureSessionCommand cmd = new EnterSecureSessionCommand();
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = (ITask) params[0];
					notifyMonitor(TaskEvent.FAILED, paymentService);
					break;

				case SUCCESS:
					context.setKsn(cmd.getKsn());

					switch (context.getActivatedReader()) {
					case Constants.MS_READER:
						paymentService = new MSPaymentService(context);
						break;

					case Constants.ICC_READER:
						paymentService = new ICCPaymentService(context);
						paymentService.setApplicationSelectionProcessor(applicationSelectionProcessor);
						break;

					case Constants.NFC_READER:
						// TODO implement NFC payment

					default:
						end(PaymentState.ERROR);
						return;
					}

					paymentService.setRiskManagementTask(riskManagementTask);
					paymentService.setAuthorizationProcessor(authorizationProcessor);

					paymentService.execute(monitor);
					break;
				}
			}
		});
	}

}
