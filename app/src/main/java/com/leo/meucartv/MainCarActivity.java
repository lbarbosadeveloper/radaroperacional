package com.leo.meucartv;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.EditText;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.gms.car.input.InputManager;

/** Tela que aparece no Android Auto. */
public class MainCarActivity extends CarActivity {
	private static final String TAG = "MeuCarTV";

	private TvView tv;
	private SpeedSource speed;
	private final SpeedGate gate = new SpeedGate();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Evita que a tela seja recriada (e o vídeo pare) quando o carro muda dia/noite ou orientação.
		setIgnoreConfigChanges(0xFFFFFFFF);
		CarUiController ui = getCarUiController();
		ui.getStatusBarController().hideAppHeader();
		ui.getMenuController().hideMenuButton();

		tv = new TvView(this, new CarKeyboard());
		setContentView(tv);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (tv != null) tv.onResume();
		startSpeedWatch();
	}

	@Override
	public void onPause() {
		stopSpeedWatch();
		if (tv != null) tv.onPause();
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		if (tv == null || !tv.handleBack()) super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		stopSpeedWatch();
		if (tv != null) {
			tv.destroy();
			tv = null;
		}
		super.onDestroy();
	}

	/** Usa a velocidade do carro; se o carro não informar, tenta o GPS do celular. */
	private void startSpeedWatch() {
		if (speed != null || tv == null) return;
		if (!new AppPrefs(this).isMotionLockEnabled()) {
			tv.setBlocked(false);
			return;
		}
		SpeedSource.Listener l = v -> {
			if (gate.onSpeed(v, SystemClock.elapsedRealtime()) && tv != null) tv.setBlocked(gate.isBlocked());
		};
		SpeedSource car = new CarSpeedSource(this);
		if (car.start(l)) {
			speed = car;
			Log.i(TAG, "Bloqueio em movimento: usando a velocidade do carro");
			return;
		}
		SpeedSource gps = new GpsSpeedSource(this);
		if (gps.start(l)) {
			speed = gps;
			Log.i(TAG, "Bloqueio em movimento: usando o GPS do celular");
		} else {
			Log.w(TAG, "Bloqueio em movimento: nenhuma fonte de velocidade disponível");
		}
	}

	private void stopSpeedWatch() {
		if (speed != null) {
			speed.stop();
			speed = null;
		}
	}

	/** Teclado da tela do carro, via Android Auto. */
	private final class CarKeyboard implements TvView.Input {
		@Override
		public EditText create(Context context, Runnable onSubmit) {
			CarEditText field = new CarEditText(context);
			field.setOnSubmit(() -> {
				end(field);
				onSubmit.run();
			});
			return field;
		}

		@Override
		public void begin(EditText field) {
			if (!(field instanceof CarEditText)) return;
			try {
				InputManager im = a();
				if (im != null && !im.isCurrentCarEditable((CarEditText) field)) im.startInput((CarEditText) field);
			} catch (Throwable t) {
				Log.w(TAG, "Teclado do carro indisponível", t);
			}
		}

		@Override
		public void end(EditText field) {
			try {
				InputManager im = a();
				if (im != null && im.isInputActive()) im.stopInput();
			} catch (Throwable t) {
				Log.w(TAG, "Falha ao fechar o teclado do carro", t);
			}
		}
	}
}
