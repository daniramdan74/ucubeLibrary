/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p/>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.mdm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.mdm.MDMManager;
import com.youTransactor.uCube.mdm.service.SendLogsService;
import com.youTransactor.uCube.rpc.DeviceInfos;
import com.youTransactor.uCube.mdm.service.GetConfigService;
import com.youTransactor.uCube.mdm.service.RegisterService;
import com.youTransactor.uCube.mdm.service.ServiceState;
import com.youTransactor.uCube.mdm.service.BinaryUpdate;
import com.youTransactor.uCube.mdm.service.CheckUpdateService;
import com.youTransactor.uCube.mdm.service.UpdateService;
import com.youTransactor.uCube.test.R;
import com.youTransactor.uCube.mdm.Config;
import com.youTransactor.uCube.test.UIUtils;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gbillard on 3/29/16.
 */
public class MDMFragment extends Fragment {

	private DeviceInfos deviceInfos;
	private List<BinaryUpdate> updateList;
	private Switch updateSameVersionBtn;
	private Button getCfgBtn;
	private Button doUpdateBtn;
	private Button checkUpdateBtn;

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.mdm_fragment, container, false);

		updateSameVersionBtn = (Switch) view.findViewById(R.id.updateSameVersionBtn);

		Button registerBtn = (Button) view.findViewById(R.id.registerBtn);
		registerBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Register device");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new RegisterService(getContext()).execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Device registration failed");
											break;

										case SUCCESS:
											KeyStore sslKey = (KeyStore) params[0];
											MDMManager.getInstance().setSSLCertificat(getContext(), sslKey);
											getCfgBtn.setEnabled(MDMManager.getInstance().isReady());
											checkUpdateBtn.setEnabled(MDMManager.getInstance().isReady());
											Toast.makeText(getContext(), "Device registration succeed", Toast.LENGTH_LONG).show();
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

		getCfgBtn = (Button) view.findViewById(R.id.getCfgBtn);
		getCfgBtn.setEnabled(MDMManager.getInstance().isReady());
		getCfgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Get device config from server");

				new Thread(new Runnable() {
					@Override
					public void run() {
						new GetConfigService().execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Get device config from server error");
											break;

										case SUCCESS:
											DeviceConfigDialogFragment dlg = new DeviceConfigDialogFragment();
											dlg.init((List<Config>) params[0]);
											dlg.show(MDMFragment.this.getFragmentManager(), DeviceConfigDialogFragment.class.getSimpleName());
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

		checkUpdateBtn = (Button) view.findViewById(R.id.checkUpdateBtn);
		checkUpdateBtn.setEnabled(MDMManager.getInstance().isReady());
		checkUpdateBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progressDlg = UIUtils.showProgress(getContext(), "Check update", false);

				new Thread(new Runnable() {
					@Override
					public void run() {
						final CheckUpdateService svc = new CheckUpdateService();
						svc.setUpdateSameVersion(updateSameVersionBtn.isChecked());

						svc.execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case PROGRESS:
											switch ((ServiceState) params[0]) {
											case RETRIEVE_DEVICE_INFOS:
												progressDlg.setMessage("Get device infos");
												break;

											case RETRIEVE_DEVICE_CONFIG:
												progressDlg.setMessage("Get device configuration from server");
												break;

											case CHECK_UPDATE:
												progressDlg.setMessage("Compare installed and available versions");
												break;
											}
											return;

										case CANCELLED:
											Toast.makeText(getContext(), "Check update cancelled", Toast.LENGTH_LONG).show();
											break;

										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Check update failed");
											break;

										case SUCCESS:
											updateList = (List<BinaryUpdate>) params[0];

											if (updateList.size() == 0) {
												Toast.makeText(getContext(), "Device is up to date", Toast.LENGTH_SHORT).show();
											} else {
												doUpdateBtn.setEnabled(true);

												CheckUpdateResultDialog dlg = new CheckUpdateResultDialog();
												dlg.init(updateList);
												dlg.show(MDMFragment.this.getFragmentManager(), CheckUpdateResultDialog.class.getSimpleName());
											}
											break;

										default:
											return;
										}

										deviceInfos = svc.getDeviceInfos();

										progressDlg.dismiss();
									}
								});
							}
						});
					}
				}).start();
			}
		});

		doUpdateBtn = (Button) view.findViewById(R.id.doUpdateBtn);
		doUpdateBtn.setEnabled(updateList != null && updateList.size() > 0 && MDMManager.getInstance().isReady());
		doUpdateBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showBinUpdateListDialog();
			}
		});

		Button sendLogsBtn = (Button) view.findViewById(R.id.sendLogsBtn);
		sendLogsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!LogManager.hasLogs()) {
					UIUtils.showMessageDialog(getContext(), "No logs to send");
					return;
				}

				UIUtils.showProgress(getContext(), "Send logs to server", true);

				new Thread(new Runnable() {
					@Override
					public void run() {
						new SendLogsService().execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								if (event == TaskEvent.PROGRESS) {
									return;
								}

								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Send logs error");
											break;

										case SUCCESS:
											Toast.makeText(getContext(), "Logs sent successfuly", Toast.LENGTH_LONG).show();
											break;
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

		return view;
	}

	private void showBinUpdateListDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

		builder.setTitle("Select update");

		boolean[] selectedItems = new boolean[updateList.size()];
		String[] items = new String[updateList.size()];

		for (int i = 0; i < updateList.size(); i++) {
			selectedItems[i] = true;
			items[i] = updateList.get(i).getCfg().getLabel();
		}

		builder.setMultiChoiceItems(items, selectedItems, null);

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final SparseBooleanArray selectedItems = ((AlertDialog) dialog).getListView().getCheckedItemPositions();

				dialog.dismiss();

				UIUtils.showProgress(getContext(), "Start update", false);

				new Thread(new Runnable() {
					@Override
					public void run() {
						List<BinaryUpdate> selectedUpdateList = new ArrayList<BinaryUpdate>(selectedItems.size());

						for (int i = 0; i < selectedItems.size(); i++) {
							if (selectedItems.get(i)) {
								selectedUpdateList.add(updateList.get(i));
							}
						}
						final UpdateService svc = new UpdateService(deviceInfos, selectedUpdateList);

						svc.execute(new ITaskMonitor() {
							@Override
							public void handleEvent(final TaskEvent event, final Object... params) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										switch (event) {
										case PROGRESS:
											switch ((ServiceState) params[0]) {
											case RETRIEVE_DEVICE_CERTIFICAT:
												UIUtils.setProgressMessage("Retrieve device certificate");
												break;

											case DOWNLOAD_BINARY:
												UIUtils.setProgressMessage("Download  " + ((Config) params[1]).getLabel() +  " (" + (updateList.size() - (int) params[2]) + "/" + updateList.size() + ")");
												break;

											case UPDATE_DEVICE:
												UIUtils.setProgressMessage("Update " + ((Config) params[1]).getLabel() + " (" + (updateList.size() - (int) params[2]) + "/" + updateList.size() + ")");
												break;

											case RECONNECT:
												UIUtils.setProgressMessage("Reconnect");
												break;
											}
											return;

										case CANCELLED:
											Toast.makeText(getContext(), "Device update cancelled", Toast.LENGTH_LONG).show();
											break;

										case FAILED:
											UIUtils.showMessageDialog(getContext(), "Device update failed");
											break;

										case SUCCESS:
											updateList = null;
											doUpdateBtn.setEnabled(false);
											Toast.makeText(getContext(), "Device updated successfully", Toast.LENGTH_SHORT).show();
											break;
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

		builder.create().show();
	}

}
