package com.leo.meucartv;

import android.content.Context;
import android.content.SharedPreferences;

/** Configurações do app e a última página vista (para o "Continuar"). */
public final class AppPrefs {
	private static final String PREFS = "config";
	private static final String MOTION_LOCK = "bloqueio_movimento";
	private static final String LAST_URL = "ultima_url";
	private static final String LAST_TITLE = "ultimo_titulo";

	private final SharedPreferences prefs;

	public AppPrefs(Context context) {
		prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	public boolean isMotionLockEnabled() {
		return prefs.getBoolean(MOTION_LOCK, true);
	}

	public void setMotionLockEnabled(boolean enabled) {
		prefs.edit().putBoolean(MOTION_LOCK, enabled).apply();
	}

	public String getLastUrl() {
		return prefs.getString(LAST_URL, "");
	}

	public String getLastTitle() {
		return prefs.getString(LAST_TITLE, "");
	}

	public void setLastPage(String url, String title) {
		prefs.edit().putString(LAST_URL, url == null ? "" : url)
				.putString(LAST_TITLE, title == null ? "" : title).apply();
	}
}
