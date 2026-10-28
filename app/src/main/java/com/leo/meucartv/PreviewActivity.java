package com.leo.meucartv;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/** Mostra no celular exatamente a mesma tela do carro, para testar sem estar no carro. */
public class PreviewActivity extends Activity {
	private TvView tv;
	private SpeedSource speed;
	private final SpeedGate gate = new SpeedGate();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tv = new TvView(this, new PhoneKeyboard());
		setContentView(tv);
	}

	@Override
	protected void onResume() {
		super.onResume();
		tv.onResume();
		if (speed == null && new AppPrefs(this).isMotionLockEnabled()) {
			SpeedSource gps = new GpsSpeedSource(this);
			if (gps.start(v -> {
				if (gate.onSpeed(v, SystemClock.elapsedRealtime())) tv.setBlocked(gate.isBlocked());
			})) speed = gps;
		}
	}

	@Override
	protected void onPause() {
		if (speed != null) {
			speed.stop();
			speed = null;
		}
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

	/** Teclado normal do celular. */
	private final class PhoneKeyboard implements TvView.Input {
		@Override
		public EditText create(Context context, Runnable onSubmit) {
			EditText field = new EditText(context);
			field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
			field.setOnEditorActionListener((v, actionId, event) -> {
				end(field);
				onSubmit.run();
				return true;
			});
			return field;
		}

		@Override
		public void begin(EditText field) {
			field.requestFocus();
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			if (imm != null) imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
		}

		@Override
		public void end(EditText field) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
		}
	}
}
