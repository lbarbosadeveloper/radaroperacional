package com.leo.meucartv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Guarda os atalhos no próprio celular. Celular e tela do carro leem do mesmo lugar. */
public final class ShortcutStore {
	private static final String PREFS = "atalhos";
	private static final String KEY = "lista";

	private final SharedPreferences prefs;

	public ShortcutStore(Context context) {
		prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	public List<Shortcut> load() {
		if (!prefs.contains(KEY)) return new ArrayList<>(ShortcutCodec.defaults());
		return ShortcutCodec.decode(prefs.getString(KEY, ""));
	}

	public void save(List<Shortcut> shortcuts) {
		prefs.edit().putString(KEY, ShortcutCodec.encode(shortcuts)).apply();
	}

	public void resetToDefaults() {
		save(ShortcutCodec.defaults());
	}
}
