/*
 * Copyright (C) 2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */
package com.youTransactor.uCube;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author gbillard on 6/21/16.
 */
public class LogManager {

	public static void debug(String tag, String message) {
		debug(tag, message, null);
	}

	public static boolean storeTransactionLog(byte[] logs1, byte[] logs2) {
		if (context == null || (logs1 == null && logs2 == null)) {
			return false;
		}

		File logDir = context.getDir(LOG_DIR, Context.MODE_PRIVATE);

		try {
			FileOutputStream out = new FileOutputStream(new File(logDir, timestampFormatter.format(new Date()).replace(':','-').replace(' ', '_')));
			if (logs1 != null) {
				IOUtils.copy(new ByteArrayInputStream(logs1), out);
			}

			if (logs2 != null) {
				IOUtils.copy(new ByteArrayInputStream(logs2), out);
			}

			return true;

		} catch (Exception e) {
			debug(LogManager.class.getSimpleName(), "unable to store transaction logs", e);
			return false;
		}
	}

	public static boolean hasLogs() {
		return context.getDir(LOG_DIR, Context.MODE_PRIVATE).listFiles().length > 0;
	}

	public static void getLogs(OutputStream out) throws IOException {
		File logDir = context.getDir(LOG_DIR, Context.MODE_PRIVATE);

		if (logDir.listFiles().length > 0) {
			ZipOutputStream zout = new ZipOutputStream(out);

			for (File file : logDir.listFiles()) {
				ZipEntry entry = new ZipEntry(file.getName());
				zout.putNextEntry(entry);
				IOUtils.copy(new FileInputStream(file), zout);
				zout.closeEntry();
			}

			zout.close();
		}
	}

	public static void deleteLogs() {
		File logDir = context.getDir(LOG_DIR, Context.MODE_PRIVATE);

		if (!logDir.exists()) {
			return;
		}

		boolean wasEnabled = false;

		if (out != null) {
			synchronized (mOut) {
				wasEnabled = true;
				out.close();
				out = null;
			}
		}

		for (File file : logDir.listFiles()) {
			file.delete();
		}

		setEnabled(wasEnabled);
	}

	public static void debug(String tag, String message, Exception e) {
		Log.d(tag, message, e);

		if (out != null) {
			synchronized (mOut) {
				if (out != null) {
					out.print(timestampFormatter.format(new Date()));
					out.print(" - ");
					out.print(tag);
					out.print(": ");
					out.print(message);
					out.print("\n");

					if (e != null) {
						e.printStackTrace(out);
					}

					out.flush();
				}
			}
		}
	}

	public static boolean isEnabled() {
		return out != null;
	}

	public static void setEnabled(boolean state) {
		synchronized (mOut) {
			if (state) {
				if (out == null) {
					if (context == null) {
						return;
					}

					File logDir = context.getDir(LOG_DIR, Context.MODE_PRIVATE);

					if (!logDir.exists()) {
						logDir.mkdirs();
					}

					try {
						out = new PrintStream(new File(logDir, LOG_FILE_NAME));
					} catch (Exception e) {
						Log.d(LogManager.class.getSimpleName(), "unable to open custom log file", e);
					}
				}

			} else {
				if (out != null) {
					out.close();
					out = null;
				}
			}
		}
	}

	public static void initialize(Context context) {
		LogManager.context = context;

		setEnabled(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(LOG_MANAGER_STATE_SETTINGS_KEY, false));
	}

	public static final String LOG_MANAGER_STATE_SETTINGS_KEY = "LogManager.state";

	private static Context context;
	private static final Object mOut = new Object();
	private static PrintStream out;
	private static final DateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String LOG_DIR = "logs";
	private static final String LOG_FILE_NAME = "logcat.txt";

}
