/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 * <p>
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube.mdm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.youTransactor.uCube.LogManager;
import com.youTransactor.uCube.R;
import com.youTransactor.uCube.rpc.DeviceInfos;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author gbillard on 4/3/16.
 */
public class MDMManager implements SharedPreferences.OnSharedPreferenceChangeListener {

	private String serverURL = DEFAULT_URL;
	private SSLContext sslContext;
	private boolean ready;
	private DeviceInfos deviceinfos;

	private MDMManager() {}

	public boolean setSSLCertificat(Context context, KeyStore sslKeystore) {
		try {
			FileOutputStream out = context.openFileOutput(KEYSTORE_CLIENT_FILENAME, Context.MODE_PRIVATE);
			sslKeystore.store(out, PWD);
			out.close();

			initialize(context);

			return ready;

		} catch (Exception e) {
			return false;
		}
	}

	public HttpURLConnection initRequest(String service, String method) throws IOException {
		URL url = new URL(serverURL + WS_URL_PREFIX + service);

		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		if(urlConnection instanceof HttpsURLConnection) {
			((HttpsURLConnection)urlConnection).setSSLSocketFactory(sslContext.getSocketFactory());
		}

		urlConnection.setRequestMethod(method);
		urlConnection.setConnectTimeout(20000);
		urlConnection.setReadTimeout(30000);


		LogManager.debug(MDMManager.class.getSimpleName(), "init request: " + url.getPath() + " (" + method + ")");

		return urlConnection;
	}

	public boolean isReady() {
		return ready;
	}

	public DeviceInfos getDeviceinfos() {
		return deviceinfos;
	}

	public void setDeviceinfos(DeviceInfos deviceinfos) {
		this.deviceinfos = deviceinfos;
	}

	/**
	 *
	 * @param settings
	 *
	 * @deprecated use initialize(Context context) instead
	 */
	public void initialize(SharedPreferences settings) {
		serverURL = settings.getString(MDM_SERVER_URL_SETTINGS_KEY, DEFAULT_URL);
	}

	/**
	 *
	 * @param context
	 *
	 * @deprecated use initialize(Context context) instead
	 */
	public void initSSLContext(Context context) {
		initialize(context);
	}

	public void initialize(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

		onSharedPreferenceChanged(settings, null);

		settings.registerOnSharedPreferenceChangeListener(this);

		try {
			KeyStore keystoreCA = KeyStore.getInstance(KEYSTORE_TYPE);
			keystoreCA.load(context.getResources().openRawResource(R.raw.keystore), PWD);

			KeyStore keystoreClient = null;

			File file = context.getFileStreamPath(KEYSTORE_CLIENT_FILENAME);

			if (file.exists()) {
				keystoreClient = KeyStore.getInstance(KEYSTORE_TYPE);
				InputStream in = new FileInputStream(file);
				keystoreClient.load(in, PWD);
			}

			ready = keystoreClient != null && keystoreClient.getKey(MDM_CLIENT_CERT_ALIAS, PWD) != null;

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keystoreCA);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
			kmf.init(keystoreClient, PWD);

			sslContext = SSLContext.getInstance("TLS");

			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		} catch (Exception e) {
			LogManager.debug(MDMManager.class.getSimpleName(), "load keystore error", e);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
		serverURL = settings.getString(MDM_SERVER_URL_SETTINGS_KEY, DEFAULT_URL);

		String serial = settings.getString(MDM_DEVICE_SERIAL_SETTINGS_KEY, "");
		String pn = settings.getString(MDM_DEVICE_PART_NUMBER_SETTINGS_KEY, "");

		if (StringUtils.isNotBlank(serial) && StringUtils.isNotBlank(pn)) {
			deviceinfos = new DeviceInfos(serial, pn);
		}
	}

	public static MDMManager getInstance() {
		return INSTANCE;
	}

	public static final String GET_METHOD = "GET";
	public static final String POST_METHOD = "POST";

	public static final String MDM_DEVICE_SERIAL_SETTINGS_KEY = "MDM.deviceSerial";
	public static final String MDM_DEVICE_PART_NUMBER_SETTINGS_KEY = "MDM.devicePartNUmber";
	public static final String MDM_SERVER_URL_SETTINGS_KEY = "MDM.serverUrl";
	/**
	 * @deprecated use MDM_SERVER_URL_SETTINGS_KEY instead
	 */
	public static final String SERVER_URL_KEY = MDM_SERVER_URL_SETTINGS_KEY;

	private static MDMManager INSTANCE = new MDMManager();

	public static final String DEFAULT_URL = "https://dev.youtransactor.com";

	private static final char[] PWD = new char[] {'g', 'm', 'x', 's', 'a', 's'};
	private static final String WS_URL_PREFIX = "/MDM/jaxrs";
	private static final String KEYSTORE_CLIENT_FILENAME = "keystore_client.jks";
	private static final String MDM_CLIENT_CERT_ALIAS = "MDM-client";
	private static final String KEYSTORE_TYPE = "PKCS12";

}
