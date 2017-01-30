/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.test.payment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.youTransactor.uCube.ITaskMonitor;
import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.TaskEvent;
import com.youTransactor.uCube.payment.AbstractPaymentService;
import com.youTransactor.uCube.payment.PaymentService;
import com.youTransactor.uCube.payment.Currency;
import com.youTransactor.uCube.payment.PaymentContext;
import com.youTransactor.uCube.payment.SingleEntryPointPaymentService;
import com.youTransactor.uCube.rpc.Constants;
import com.youTransactor.uCube.test.R;
import com.youTransactor.uCube.test.UIUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * @author gbillard on 5/12/16.
 */
public class PaymentFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private EditText cardWaitTimeoutFld;
	private Spinner trxTypeChoice;
	private EditText amountFld;
	private Spinner currencyChooser;
	private CheckBox MSRCheck;
	private CheckBox ICCCheck;
	private CheckBox NFCCheck;
	private Switch forceOnlinePINBtn;
	private EditText trxDateFld;
	private Switch amountSrcSwitch;
	private Switch useSEPSwitch;
	private TextView trxResultFld;
	private ResourceBundle msgBundle;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH'h'mm:ss");

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.payment_fragment, container, false);

		cardWaitTimeoutFld = (EditText) view.findViewById(R.id.cardWaitTimeoutFld);

		useSEPSwitch = (Switch) view.findViewById(R.id.useSEPSwitch);

		trxDateFld = (EditText) view.findViewById(R.id.trxDateFld);
		trxDateFld.setText(dateFormat.format(new Date()));
		trxDateFld.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Calendar cal = Calendar.getInstance();

				try {
					cal.setTime(dateFormat.parse(trxDateFld.getText().toString()));
				} catch (Exception ignored) {}

				new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						cal.set(Calendar.YEAR, year);
						cal.set(Calendar.MONTH, monthOfYear);
						cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

						trxDateFld.setText(dateFormat.format(cal.getTime()));

						new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
							@Override
							public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
								cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
								cal.set(Calendar.MINUTE, minute);

								trxDateFld.setText(dateFormat.format(cal.getTime()));
							}
						}, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();

					}
				}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		trxTypeChoice = (Spinner) view.findViewById(R.id.trxTypeChoice);
		trxTypeChoice.setAdapter(new TransactionTypeAdapter());

		amountSrcSwitch = (Switch) view.findViewById(R.id.amountSrcBtn);
		amountSrcSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				amountFld.setEnabled(!amountSrcSwitch.isChecked());
			}
		});

		amountFld = (EditText) view.findViewById(R.id.amountFld);
		amountFld.setText("1.00");

		final CurrencyAdapter adapter = new CurrencyAdapter();
		adapter.add(new Currency(978, 2, "EUR"));
		adapter.add(new Currency(840, 2, "USD"));

		currencyChooser = (Spinner) view.findViewById(R.id.currencyChooser);
		currencyChooser.setAdapter(adapter);
		currencyChooser.setSelection(0);

		MSRCheck = (CheckBox) view.findViewById(R.id.MSRCheck);

		ICCCheck = (CheckBox) view.findViewById(R.id.ICCCheck);

		NFCCheck = (CheckBox) view.findViewById(R.id.NFCCheck);

		forceOnlinePINBtn = (Switch) view.findViewById(R.id.forceOnlinePINBtn);

		Button doPaymentBtn = (Button) view.findViewById(R.id.doPaymentBtn);
		doPaymentBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startPayment();
			}
		});

		trxResultFld = (TextView) view.findViewById(R.id.trxResultFld);

		init(PreferenceManager.getDefaultSharedPreferences(getContext()));

		try {
			msgBundle = new PropertyResourceBundle(getContext().getResources().openRawResource(R.raw.ucube_strings));
		} catch (Exception e) {
			LogManager.debug(PaymentFragment.class.getSimpleName(), "Unable to load uCube message bundle", e);
		}

		PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);

		return view;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		init(sharedPreferences);
	}

	private void init(SharedPreferences settings) {
		boolean nfcEnabled = settings.getBoolean(NFC_ENABLE_DEVICE_SETTINGS_KEY, false);

		if (!nfcEnabled) {
			NFCCheck.setChecked(false);
			amountSrcSwitch.setChecked(false);
			useSEPSwitch.setChecked(false);
		}

		useSEPSwitch.setEnabled(nfcEnabled);
		NFCCheck.setEnabled(nfcEnabled);
		amountSrcSwitch.setEnabled(nfcEnabled);
	}

	private void startPayment() {
		trxResultFld.setText("");

		double amount = -1;

		try {
			amount = Double.parseDouble(amountFld.getText().toString());
		} catch (Exception e) {
			amount = -1;
			amountSrcSwitch.setChecked(true);
		}

		Currency currency = (Currency) currencyChooser.getSelectedItem();
		TransactionType trxType = (TransactionType) trxTypeChoice.getSelectedItem();

		final PaymentContext paymentContext = new PaymentContext();

		String msg;

		if (amountSrcSwitch.isChecked()) {
			msg = "Make payment in " + currency.getLabel();
		} else {
			paymentContext.setAmount(amount);
			msg = String.format("Make payment of %.2f %s", amount, currency.getLabel());
		}

		paymentContext.setCurrency(currency);
		paymentContext.setTransactionType(trxType.getCode());
		paymentContext.setPreferredLanguageList(Arrays.asList("en"));
		try {
			paymentContext.setTransactionDate(dateFormat.parse(trxDateFld.getText().toString()));
		} catch (Exception ignored) {}

		paymentContext.setMsgBundle(msgBundle);

		final List<CardReaderType> readerList = new ArrayList<>();

		if (MSRCheck.isChecked()) {
			readerList.add(CardReaderType.MSR);

			paymentContext.setRequestedSecuredTagList(new int[] {Constants.TAG_CARD_DATA_BLOCK});
			paymentContext.setRequestedPlainTagList(new int[] {Constants.TAG_MSR_BIN});
			paymentContext.setForceOnlinePIN(forceOnlinePINBtn.isChecked());
		}

		if (ICCCheck.isChecked()) {
			readerList.add(CardReaderType.ICC);

			List<byte[]> tagList = new ArrayList<>();
			tagList.add(new byte[] {(byte) 0x95}); /* TVR */
			tagList.add(new byte[] {(byte) 0x9B}); /* TSI */

			paymentContext.setRequestedAuthorizationTagList(tagList);
		}

		if (NFCCheck.isChecked()) {
			readerList.add(CardReaderType.NFC);
		}

		if (readerList.isEmpty()) {
			Toast.makeText(getContext(), "No reader activated", Toast.LENGTH_LONG).show();
			return;
		}

		UIUtils.showProgress(getContext(), msg);

		new Thread(new Runnable() {
			@Override
			public void run() {
				byte[] activatedReaders = new byte[readerList.size()];
				for (int i = 0; i < activatedReaders.length; i++) {
					activatedReaders[i] = readerList.get(i).getCode();
				}

				PaymentService svc;

				if (NFCCheck.isChecked() || useSEPSwitch.isChecked()) {
					svc = new SingleEntryPointPaymentService(paymentContext, activatedReaders);
				} else {
					svc = new PaymentService(paymentContext, activatedReaders);
				}

				svc.setCardWaitTimeout(Integer.valueOf(cardWaitTimeoutFld.getText().toString()));
				svc.setRiskManagementTask(new RiskManagementTask(getActivity()));
				svc.setAuthorizationProcessor(new AuthorizationTask(getActivity()));
				svc.execute(new ITaskMonitor() {
					@Override
					public void handleEvent(final TaskEvent event, final Object... params) {
						final AbstractPaymentService svc = (AbstractPaymentService) params[0];

						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								switch (event) {
								case PROGRESS:
									switch (paymentContext.getPaymentStatus()) {
									case STARTED:
										UIUtils.setProgressMessage(String.format("Make payment of %.2f %s", paymentContext.getAmount(), paymentContext.getCurrency().getLabel()));
										break;
									}
									return;

								case SUCCESS:
									switch (paymentContext.getPaymentStatus()) {
									case APPROVED:
										trxResultFld.setText("Transaction approved");
										break;

									case DECLINED:
										trxResultFld.setText("Transaction declined");
										break;

									case CHIP_REQUIRED:
										trxResultFld.setText("MSR use forbidden.\nUse chip");
										break;

									case CANCELLED:
										trxResultFld.setText("Transaction cancelled");
										break;

									case UNSUPPORTED_CARD:
										trxResultFld.setText("Card not supported");
										break;

									case REFUSED_CARD:
										trxResultFld.setText("Card refused");
										break;

									default:
										trxResultFld.setText("Error occured");
										break;
									}
									break;

								case FAILED:
									trxResultFld.setText("Error occured");
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

	public final static String NFC_ENABLE_DEVICE_SETTINGS_KEY = "NFC_enabled_device";

}
