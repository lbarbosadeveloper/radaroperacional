package com.leo.meucartv;

import android.app.Activity;
import android.os.Bundle;

/** Mostra no celular exatamente a mesma tela do carro, para testar sem estar no carro. */
public class PreviewActivity extends Activity {
	private TvView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tv = new TvView(this);
		setContentView(tv);
	}

	@Override
	protected void onResume() {
		super.onResume();
		tv.onResume();
	}

	@Override
	protected void onPause() {
		tv.onPause();
		super.onPause();
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onBackPressed() {
		if (!tv.handleBack()) super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		tv.destroy();
		super.onDestroy();
	}
}
