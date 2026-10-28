package com.leo.meucartv;

import android.os.Bundle;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;

/** Tela que aparece no Android Auto. */
public class MainCarActivity extends CarActivity {
	private TvView tv;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Evita que a tela seja recriada (e o vídeo pare) quando o carro muda dia/noite ou orientação.
		setIgnoreConfigChanges(0xFFFFFFFF);
		CarUiController ui = getCarUiController();
		ui.getStatusBarController().hideAppHeader();
		ui.getMenuController().hideMenuButton();

		tv = new TvView(this);
		setContentView(tv);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (tv != null) tv.onResume();
	}

	@Override
	public void onPause() {
		if (tv != null) tv.onPause();
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		if (tv == null || !tv.handleBack()) super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		if (tv != null) {
			tv.destroy();
			tv = null;
		}
		super.onDestroy();
	}
}
