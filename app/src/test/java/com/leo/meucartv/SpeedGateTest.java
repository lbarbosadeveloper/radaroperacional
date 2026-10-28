package com.leo.meucartv;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpeedGateTest {

	private static float kmh(float v) {
		return v / 3.6f;
	}

	@Test
	public void startsUnblocked() {
		assertFalse(new SpeedGate().isBlocked());
	}

	@Test
	public void blocksWhenSpeedReachesTenKmh() {
		SpeedGate g = new SpeedGate();

		g.onSpeed(kmh(9), 0);
		assertFalse(g.isBlocked());

		g.onSpeed(kmh(10), 1_000);
		assertTrue(g.isBlocked());
	}

	@Test
	public void staysBlockedWhileSlowingDownButNotStopped() {
		SpeedGate g = new SpeedGate();
		g.onSpeed(kmh(40), 0);

		g.onSpeed(kmh(6), 1_000);

		assertTrue(g.isBlocked());
	}

	@Test
	public void unblocksOnlyAfterFiveSecondsStopped() {
		SpeedGate g = new SpeedGate();
		g.onSpeed(kmh(40), 0);

		g.onSpeed(kmh(2), 10_000);
		g.onSpeed(kmh(1), 14_000);
		assertTrue(g.isBlocked());

		g.onSpeed(0, 15_000);
		assertFalse(g.isBlocked());
	}

	@Test
	public void briefStopAtLightThenMovingAgainStaysBlocked() {
		SpeedGate g = new SpeedGate();
		g.onSpeed(kmh(40), 0);
		g.onSpeed(0, 10_000);
		g.onSpeed(kmh(5), 12_000);
		g.onSpeed(0, 13_000);

		g.onSpeed(0, 16_000);

		assertTrue("parada que recomeçou deve reiniciar a contagem", g.isBlocked());
	}

	@Test
	public void negativeOrNaNSpeedIsIgnored() {
		SpeedGate g = new SpeedGate();
		g.onSpeed(kmh(40), 0);

		g.onSpeed(Float.NaN, 20_000);
		g.onSpeed(-1, 30_000);

		assertTrue(g.isBlocked());
	}

	@Test
	public void onSpeedReportsWhetherStateChanged() {
		SpeedGate g = new SpeedGate();

		assertFalse(g.onSpeed(kmh(5), 0));
		assertTrue(g.onSpeed(kmh(30), 1_000));
		assertFalse(g.onSpeed(kmh(50), 2_000));
	}
}
