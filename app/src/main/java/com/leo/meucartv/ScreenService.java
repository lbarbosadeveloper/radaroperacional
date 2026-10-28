package com.leo.meucartv;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;

/**
 * "Modo navegação": o Android Auto entrega uma superfície de desenho (como faz com apps de mapa)
 * e nós desenhamos a tela do Meu CarTV nela. Mesma técnica do "FS Mirror" do Fermata Auto.
 */
public class ScreenService extends CarAppService {

	@NonNull
	@Override
	public HostValidator createHostValidator() {
		return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
	}

	@NonNull
	@Override
	public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
		return new Session() {
			@NonNull
			@Override
			public Screen onCreateScreen(@NonNull Intent intent) {
				return new SurfaceTvScreen(getCarContext());
			}
		};
	}
}
