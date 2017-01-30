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

import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.Tools;
import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.EMVApplicationDescriptor;
import com.youTransactor.uCube.rpc.command.BuildCandidateListCommand;
import com.youTransactor.uCube.rpc.command.DisplayChoiceCommand;
import com.youTransactor.uCube.rpc.command.DisplayMessageCommand;
import com.youTransactor.uCube.rpc.command.GetInfosCommand;
import com.youTransactor.uCube.rpc.command.InitTransactionCommand;
import com.youTransactor.uCube.rpc.command.TransactionFinalizationCommand;
import com.youTransactor.uCube.rpc.command.TransactionProcessCommand;
import com.youTransactor.uCube.rpc.command.WaitCardRemovalCommand;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author gbillard on 5/18/16.
 */
public class ICCPaymentService extends AbstractPaymentService {

	private boolean running;
	private List<EMVApplicationDescriptor> candidateList;

	public ICCPaymentService(PaymentContext context) {
		super(context);
	}

	@Override
	protected void start() {
		LogManager.debug(this.getClass().getSimpleName(), "start");

		running = true;

		buildCandidateList();
	}

	private void buildCandidateList() {
		LogManager.debug(this.getClass().getSimpleName(), "buildCandidateList");

		final BuildCandidateListCommand cmd = new BuildCandidateListCommand();
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					if (cmd.getResponseStatus() == Constants.EMV_NOT_SUPPORT) {
						end(PaymentState.UNSUPPORTED_CARD);
					} else {
						end(PaymentState.ERROR);
					}
					break;

				case SUCCESS:
					candidateList = cmd.getCandidateList();
					selectApplication();
					break;
				}
			}
		});
	}

	private void selectApplication() {
		LogManager.debug(this.getClass().getSimpleName(), "selectApplication");

		if (applicationSelectionProcessor == null) {
			applicationSelectionProcessor = new EMVApplicationSelectionTask();
		}

		applicationSelectionProcessor.setContext(context);
		applicationSelectionProcessor.setAvailableApplication(candidateList);

		applicationSelectionProcessor.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					failedTask = applicationSelectionProcessor;
					notifyMonitor(TaskEvent.FAILED);
					break;

				case SUCCESS:
					List<EMVApplicationDescriptor> appList = applicationSelectionProcessor.getSelection();

					if (appList.size() == 0) {
						end(PaymentState.UNSUPPORTED_CARD);

					} else if (appList.size() == 1) {
						initTransaction(appList.get(0));

					} else {
						userApplicationSelection(appList);
					}
				}
			}
		});
	}

	private void userApplicationSelection(final List<EMVApplicationDescriptor> appList) {
		LogManager.debug(this.getClass().getSimpleName(), "userApplicationSelection");

		List<String> labelList = new ArrayList<>();

		for (EMVApplicationDescriptor app : appList) {
			labelList.add(app.getLabel());
		}

		new DisplayChoiceCommand(labelList).execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					end(PaymentState.ERROR);
					break;

				case SUCCESS:
					int index = ((DisplayChoiceCommand) params[0]).getSelectedIndex();

					EMVApplicationDescriptor selected = appList.get(index);
					initTransaction(selected);
					break;
				}
			}
		});
	}

	private void initTransaction(final EMVApplicationDescriptor app) {
		LogManager.debug(this.getClass().getSimpleName(), "initTransaction using " + Tools.bytesToHex(app.getAid()));

		InitTransactionCommand cmd = new InitTransactionCommand(context.getAmount(), context.getCurrency(), context.getTransactionType(), app);
		cmd.setPreferredLanguageList(context.getPreferredLanguageList());
		cmd.setRequestedTagList(context.getRequestedAuthorizationTagList());
		cmd.setDate(context.getTransactionDate());
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					candidateList.remove(app);
					selectApplication();
					break;

				case SUCCESS:
					context.setSelectedApplication(app);
					riskManagement();
					break;
				}
			}
		});
	}

	@Override
	protected void processTransaction() {
		LogManager.debug(this.getClass().getSimpleName(), "processTransaction");

		final TransactionProcessCommand cmd = new TransactionProcessCommand(context.getTvr());
		if (context.getTransactionDate() == null) {
			/* if context.transaction != null, date has been send at initTransaction */
			cmd.setDate(new Date());
		}
		cmd.setApplicationVersion(context.getApplicationVersion());
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					end(cmd.getResponseStatus() == Constants.EMV_NOT_ACCEPT ? PaymentState.REFUSED_CARD : PaymentState.ERROR);
					break;

				case SUCCESS:
					doAuthorization();
					break;
				}
			}
		});
	}

	@Override
	public void onAuthorizationDone() {
		finalizeTransaction();
	}

	private void finalizeTransaction() {
		LogManager.debug(this.getClass().getSimpleName(), "finalizeTransaction");

		final TransactionFinalizationCommand cmd = new TransactionFinalizationCommand();
		cmd.setAuthResponse(context.getAuthorizationResponse());
		cmd.execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case FAILED:
					end(PaymentState.ERROR);
					break;

				case SUCCESS:
					context.setTransactionData(cmd.getResponseData());
					end(cmd.getResponseStatus() == 0x07 ? PaymentState.APPROVED : PaymentState.DECLINED);
					break;
				}
			}
		});
	}

	@Override
	protected void displayResult(final ITaskMonitor monitor) {
		LogManager.debug(this.getClass().getSimpleName(), "displayResult");

		final ITaskMonitor logMonitor = new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				if (event == TaskEvent.PROGRESS) {
					return;
				}

				logTransaction(monitor);
			}
		};

		if (!running) {
			context.setPaymentStatus(PaymentState.CARD_REMOVED);
			super.displayResult(logMonitor);
			return;
		}

		new DisplayMessageCommand(context.getString("LBL_remove_card")).execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				if (event == TaskEvent.PROGRESS) {
					return;
				}

				new WaitCardRemovalCommand().execute(new ITaskMonitor() {
					@Override
					public void handleEvent(TaskEvent event, Object... params) {
						if (event == TaskEvent.PROGRESS) {
							return;
						}

						ICCPaymentService.super.displayResult(logMonitor);
					}
				});
			}
		});
	}

	private void logTransaction(final ITaskMonitor monitor) {
		if (!LogManager.isEnabled()) {
			return;
		}

		final byte[][] logs = new byte[2][];

		new GetInfosCommand(Constants.TAG_SYSTEM_FAILURE_LOG_RECORD_1).execute(new ITaskMonitor() {
			@Override
			public void handleEvent(TaskEvent event, Object... params) {
				switch (event) {
				case PROGRESS:
					return;

				case FAILED:
					LogManager.debug(AbstractPaymentService.class.getSimpleName(), "retrieve transaction logs 1 errors");
					break;

				case SUCCESS:
					logs[0] = ((GetInfosCommand) params[0]).getResponseData();
					break;
				}

				new GetInfosCommand(Constants.TAG_SYSTEM_FAILURE_LOG_RECORD_2).execute(new ITaskMonitor() {
					@Override
					public void handleEvent(final TaskEvent event, final Object... params) {
						switch (event) {
						case PROGRESS:
							return;

						case FAILED:
							LogManager.debug(AbstractPaymentService.class.getSimpleName(), "retrieve transaction logs 2 errors");
							break;

						case SUCCESS:
							logs[1] =((GetInfosCommand) params[0]).getResponseData();
							break;
						}

						LogManager.storeTransactionLog(logs[0], logs[1]);

						if (monitor != null) {
							monitor.handleEvent(TaskEvent.SUCCESS);
						}
					}
				});
			}
		});
	}

}
