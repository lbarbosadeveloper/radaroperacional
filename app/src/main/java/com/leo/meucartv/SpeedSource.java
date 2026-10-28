package com.leo.meucartv;

/** Fonte de velocidade (sensor do carro ou GPS do celular). */
public interface SpeedSource {
	interface Listener {
		/** Velocidade em m/s. */
		void onSpeed(float metersPerSecond);
	}

	/** Começa a ouvir. Devolve false se esta fonte não está disponível. */
	boolean start(Listener listener);

	void stop();
}
