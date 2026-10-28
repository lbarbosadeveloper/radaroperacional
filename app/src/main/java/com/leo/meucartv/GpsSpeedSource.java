package com.leo.meucartv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

/** Velocidade pelo GPS do celular. Usada quando o carro não informa a velocidade. */
public final class GpsSpeedSource implements SpeedSource, LocationListener {
	private final Context context;
	private LocationManager manager;
	private Listener listener;

	public GpsSpeedSource(Context context) {
		this.context = context;
	}

	@Override
	@SuppressLint("MissingPermission")
	public boolean start(Listener l) {
		if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) return false;
		try {
			manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			if (manager == null || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return false;
			listener = l;
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
			return true;
		} catch (RuntimeException e) {
			manager = null;
			listener = null;
			return false;
		}
	}

	@Override
	public void stop() {
		if (manager != null) {
			try {
				manager.removeUpdates(this);
			} catch (RuntimeException ignored) {
			}
		}
		manager = null;
		listener = null;
	}

	@Override
	public void onLocationChanged(Location location) {
		Listener l = listener;
		if (l != null && location.hasSpeed()) l.onSpeed(location.getSpeed());
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}
}
