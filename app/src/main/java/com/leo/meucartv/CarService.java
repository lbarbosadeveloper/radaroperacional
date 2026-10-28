package com.leo.meucartv;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

/** Ponto de entrada que o Android Auto chama para mostrar o app na tela do carro. */
public class CarService extends CarActivityService {
	@Override
	public Class<? extends CarActivity> getCarActivity() {
		return MainCarActivity.class;
	}
}
