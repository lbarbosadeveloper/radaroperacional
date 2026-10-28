package com.leo.meucartv;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.gms.car.CarSensorManager;

/** Velocidade informada pelo próprio carro, via Android Auto. */
public final class CarSpeedSource implements SpeedSource, CarSensorManager.CarSensorEventListener {
	private static final String TAG = "MeuCarTV";
	private static final String SENSOR_SERVICE = "sensor";
	private static final int SENSOR_TYPE_CAR_SPEED = 2;
	private static final int SENSOR_RATE_UI = 2;

	private final CarActivity activity;
	private final Handler main = new Handler(Looper.getMainLooper());
	private CarSensorManager sensors;
	private Listener listener;

	public CarSpeedSource(CarActivity activity) {
		this.activity = activity;
	}

	@Override
	public boolean start(Listener l) {
		try {
			Object m = activity.a(SENSOR_SERVICE);
			if (!(m instanceof CarSensorManager)) return false;
			CarSensorManager s = (CarSensorManager) m;
			if (!s.isSensorSupported(SENSOR_TYPE_CAR_SPEED)) return false;
			listener = l;
			if (!s.registerListener(this, SENSOR_TYPE_CAR_SPEED, SENSOR_RATE_UI)) {
				listener = null;
				return false;
			}
			sensors = s;
			return true;
		} catch (Throwable t) {
			Log.w(TAG, "Sensor de velocidade do carro indisponível", t);
			listener = null;
			return false;
		}
	}

	@Override
	public void stop() {
		if (sensors != null) {
			try {
				sensors.unregisterListener(this);
			} catch (Throwable ignored) {
			}
		}
		sensors = null;
		listener = null;
	}

	@Override
	public void onSensorChanged(int type, long timestamp, float[] values, byte[] bytes) {
		if (type != SENSOR_TYPE_CAR_SPEED || values == null || values.length == 0) return;
		float speed = Math.abs(values[0]);
		main.post(() -> {
			Listener l = listener;
			if (l != null) l.onSpeed(speed);
		});
	}
}
