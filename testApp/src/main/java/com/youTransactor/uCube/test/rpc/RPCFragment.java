/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p/>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.rpc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.youTransactor.uCube.rpc.RPCCommand;
import com.youTransactor.uCube.rpc.command.DisplayChoiceCommand;
import com.youTransactor.uCube.rpc.command.EnterSecureSessionCommand;
import com.youTransactor.uCube.rpc.command.ExitSecureSessionCommand;
import com.youTransactor.uCube.rpc.command.SetInfoFieldCommand;
import com.youTransactor.uCube.test.R;
import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.rpc.DeviceInfos;
import com.youTransactor.uCube.test.UIUtils;
import com.youTransactor.uCube.rpc.command.DisplayMessageCommand;
import com.youTransactor.uCube.rpc.command.GetInfosCommand;
import com.youTransactor.uCube.rpc.command.InstallForLoadKeyCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gbillard on 3/29/16.
 */
public class RPCFragment extends Fragment {

	private EditText displayMsgField;
	private ArrayAdapter<String> choiceListAdapater;
	private Switch secureSessionSwitch;
	private EditText powerTimeoutFld;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.rpc_fragment, container, false);

		Button displayMsgBtn = (Button) view.findViewById(R.id.displayMsgBtn);
		displayMsgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Display message");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new DisplayMessageCommand(displayMsgField.getText().toString()).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Message display failure");
											break;

										case SUCCESS:
											Toast.makeText(getContext(), "Message displayed successfully", Toast.LENGTH_LONG).show();
											break;

										default:
											return;
										}

										progressDlg.dismiss();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		displayMsgField = (EditText) view.findViewById(R.id.displayMsgField);

		powerTimeoutFld = (EditText) view.findViewById(R.id.powerTimeoutFld);

		Button getInfosBtn = (Button) view.findViewById(R.id.getInfosBtn);
		getInfosBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				UIUtils.showProgress(getContext(), "Get current device infos");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new GetInfosCommand(
								Constants.TAG_FIRMWARE_VERSION, Constants.TAG_TERMINAL_PN, Constants.TAG_TERMINAL_SN,
								Constants.TAG_EMV_L1_VERSION, Constants.TAG_EMV_L2_VERSION, Constants.TAG_BOOT_LOADER_VERSION,
								Constants.TAG_EMV_ICC_CONFIG_VERSION, Constants.TAG_EMV_NFC_CONFIG_VERSION, Constants.TAG_MPOS_MODULE_STATE
						).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								switch (event) {
								case PROGRESS:
									return;

								case FAILED:
									showStateDialog(null);
									break;

								case SUCCESS:
									final DeviceInfos infos = new DeviceInfos(((GetInfosCommand) params[0]).getResponseData());

									switch (infos.getNfcModuleState()) {
									case 3:/* ready */
										new GetInfosCommand(Constants.TAG_FIRMWARE_VERSION, Constants.TAG_TERMINAL_PN, Constants.TAG_TERMINAL_SN,
												Constants.TAG_EMV_L1_VERSION, Constants.TAG_EMV_L2_VERSION, Constants.TAG_BOOT_LOADER_VERSION,
												Constants.TAG_EMV_ICC_CONFIG_VERSION, Constants.TAG_EMV_NFC_CONFIG_VERSION,
												Constants.TAG_MPOS_MODULE_STATE, Constants.TAG_NFC_INFOS).execute(new ITaskMonitor() {
											@Override
											public void handleEvent(TaskEvent event, Object... params) {
												switch (event) {
												case SUCCESS:
													showStateDialog(new DeviceInfos(((GetInfosCommand) params[0]).getResponseData()));
													break;

												case FAILED:
													showStateDialog(infos);
												}
											}
										});
										break;

									default:
										showStateDialog(infos);
									}
									break;
								}
							};
						});
					}
				}).start();
			}
		});

		Button powerTimeoutBtn = (Button) view.findViewById(R.id.powerTimeoutBtn);
		powerTimeoutBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final int timeout = Integer.parseInt(powerTimeoutFld.getText().toString());

				if ((timeout < 32 && timeout != 0) || timeout > 255) {
					Toast.makeText(getContext(), "timeout must be > 32 and < 255 or equal to 0 to disable poweroff", Toast.LENGTH_SHORT).show();
					return;
				}

				UIUtils.showProgress(getContext(), "Set power timeout");

				new Thread(new Runnable() {
					@Override
					public void run() {
						SetInfoFieldCommand cmd = new SetInfoFieldCommand();
						cmd.setPowerTimeout(timeout);
						cmd.execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Set power timeout failed");
											break;

										case SUCCESS:
											Toast.makeText(getContext(), "Set power timeout success", Toast.LENGTH_SHORT).show();
											break;

										default:
											return;
										}

										UIUtils.hideProgressDialog();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		choiceListAdapater = new ArrayAdapter<>(getContext(), R.layout.list_item);
		choiceListAdapater.add("item 1");
		choiceListAdapater.add("item 2");

		final ListView choiceListView = (ListView) view.findViewById(R.id.choiceListView);
		choiceListView.setAdapter(choiceListAdapater);
		choiceListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

		Button choiceBtn = (Button) view.findViewById(R.id.choiceBtn);
		choiceBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Show list choice");

				new Thread(new Runnable() {
					@Override
					public void run() {
						List<String> choiceList = new ArrayList<>(choiceListAdapater.getCount());
						for (int i = 0; i < choiceListAdapater.getCount(); i++) {
							choiceList.add(choiceListAdapater.getItem(i));
						}

						new DisplayChoiceCommand(choiceList).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								if (event == TaskEvent.PROGRESS) {
									return;
								}

								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										progressDlg.dismiss();

										switch (event) {
										case CANCELLED:
											UIUtils.showMessageDialog(getContext(), "Cancelled by user");
											break;

										case SUCCESS:
											UIUtils.showMessageDialog(getContext(), "User choice: " + ((DisplayChoiceCommand) params[0]).getSelectedItem());
											break;

										case FAILED:
											UIUtils.showMessageDialog(getContext(), "RPC command failed");
											break;
										}
									}
								});
							}
						});
					}
				}).start();
			}
		});

		Button addChoiceBtn = (Button) view.findViewById(R.id.addChoiceBtn);
		addChoiceBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				UIUtils.showUserInputDialog(getContext(), "New choice item", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String filter = ((EditText) ((AlertDialog) dialog).findViewById(UIUtils.USER_INPUT_FIELD_ID)).getText().toString();
						choiceListAdapater.add(filter);
					}
				});
			}
		});

		Button delChoiceBtn = (Button) view.findViewById(R.id.delChoiceBtn);
		delChoiceBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				choiceListAdapater.remove((String) choiceListView.getSelectedItem());
			}
		});

		Button getCertBtn = (Button) view.findViewById(R.id.getCertBtn);
		getCertBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Get device certificate");

				new Thread(new Runnable() {
					@Override
					public void run() {
						final InstallForLoadKeyCommand cmd = new InstallForLoadKeyCommand();

						cmd.execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case SUCCESS:
											//TODO display first few bytes of certificates
											Toast.makeText(getContext(), "Retrieve certificats success", Toast.LENGTH_SHORT).show();
											break;

										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Retrieve certificats failure");
											break;

										default:
											return;
										}

										progressDlg.hide();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		secureSessionSwitch = (Switch) view.findViewById(R.id.secureSessionSwitch);
		secureSessionSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final RPCCommand cmd;
				final String msg;

				if (secureSessionSwitch.isChecked()) {
					cmd = new EnterSecureSessionCommand();
					msg = "Enter secure session";

				} else {
					cmd = new ExitSecureSessionCommand();
					msg = "Exit secure session";
				}

				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), msg);

				new Thread(new Runnable() {
					@Override
					public void run() {
						cmd.execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case SUCCESS:
											Toast.makeText(getContext(), msg + " success", Toast.LENGTH_SHORT).show();
											break;

										case FAILED:
											UIUtils.showMessageDialog(getContext(), msg + " failed");
											secureSessionSwitch.setChecked(false);
											break;

										default:
											return;
										}

										progressDlg.hide();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		Button getLog1Btn = (Button) view.findViewById(R.id.getLog1Btn);
		getLog1Btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Retrieve system failure log 1");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new GetInfosCommand(Constants.TAG_SYSTEM_FAILURE_LOG_RECORD_1).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Retrieve system log 1 failure");
											break;

										case SUCCESS:
											Toast.makeText(getContext(), "Retrieve system log 1 success", Toast.LENGTH_LONG).show();
											break;

										default:
											return;
										}
										progressDlg.dismiss();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		Button getLog2Btn = (Button) view.findViewById(R.id.getLog2Btn);
		getLog2Btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Retrieve system failure log 2");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new GetInfosCommand(Constants.TAG_SYSTEM_FAILURE_LOG_RECORD_2).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Retrieve system log 2 failure");
											break;

										case SUCCESS:
											Toast.makeText(getContext(), "Retrieve system log 2 success", Toast.LENGTH_LONG).show();
											break;

										default:
											return;
										}
										progressDlg.dismiss();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		return view;
	}

	private void showStateDialog(final DeviceInfos infos) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (infos == null) {
					UIUtils.showMessageDialog(getContext(), "get device infos failure");
				} else{
					DeviceInfosDialogFragment dlg = new DeviceInfosDialogFragment();
					dlg.init(infos);
					dlg.show(getFragmentManager(), "dialog");
				}

UIUtils.hideProgressDialog();
		}

		});
		}

		}
