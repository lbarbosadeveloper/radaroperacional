package com.leo.meucartv;

/**
 * Decide se o vídeo deve ficar bloqueado pela velocidade do carro.
 * Bloqueia a partir de 10 km/h e só libera depois de 5 segundos seguidos parado (até 3 km/h),
 * para não ficar piscando no trânsito anda-e-para. Sem dependência do Android, para ser testável.
 */
public final class SpeedGate {
	static final float BLOCK_AT_MS = 10f / 3.6f;
	static final float STOPPED_BELOW_MS = 3f / 3.6f;
	static final long UNBLOCK_AFTER_MS = 5_000;

	private boolean blocked;
	private long stoppedSince = -1;

	public boolean isBlocked() {
		return blocked;
	}

	/**
	 * @param speedMetersPerSecond velocidade atual em m/s
	 * @param nowMillis            relógio monotônico em ms
	 * @return true se o estado (bloqueado/liberado) mudou com esta leitura
	 */
	public boolean onSpeed(float speedMetersPerSecond, long nowMillis) {
		if (Float.isNaN(speedMetersPerSecond) || speedMetersPerSecond < 0) return false;
		boolean before = blocked;

		if (speedMetersPerSecond >= BLOCK_AT_MS) {
			blocked = true;
			stoppedSince = -1;
		} else if (blocked) {
			if (speedMetersPerSecond <= STOPPED_BELOW_MS) {
				if (stoppedSince < 0) stoppedSince = nowMillis;
				if (nowMillis - stoppedSince >= UNBLOCK_AFTER_MS) {
					blocked = false;
					stoppedSince = -1;
				}
			} else {
				stoppedSince = -1;
			}
		}

		return before != blocked;
	}
}
