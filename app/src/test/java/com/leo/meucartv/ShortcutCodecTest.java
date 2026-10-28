package com.leo.meucartv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ShortcutCodecTest {

	@Test
	public void encodeThenDecodeKeepsNamesUrlsAndOrder() {
		List<Shortcut> original = Arrays.asList(
				new Shortcut("YouTube", "https://m.youtube.com"),
				new Shortcut("Globoplay", "https://globoplay.globo.com"));

		List<Shortcut> decoded = ShortcutCodec.decode(ShortcutCodec.encode(original));

		assertEquals(original, decoded);
	}

	@Test
	public void namesWithTabsAndLineBreaksSurviveRoundTrip() {
		List<Shortcut> original = Arrays.asList(new Shortcut("Meu\tsite\nnovo", "https://a.com"));

		List<Shortcut> decoded = ShortcutCodec.decode(ShortcutCodec.encode(original));

		assertEquals("Meu site novo", decoded.get(0).name);
	}

	@Test
	public void decodeOfNullOrEmptyReturnsEmptyList() {
		assertTrue(ShortcutCodec.decode(null).isEmpty());
		assertTrue(ShortcutCodec.decode("").isEmpty());
	}

	@Test
	public void decodeSkipsBrokenLines() {
		List<Shortcut> decoded = ShortcutCodec.decode("sem-tab\nOk\thttps://ok.com\n\t\n");

		assertEquals(Arrays.asList(new Shortcut("Ok", "https://ok.com")), decoded);
	}

	@Test
	public void normalizeUrlAddsHttpsWhenMissing() {
		assertEquals("https://youtube.com", ShortcutCodec.normalizeUrl("  youtube.com "));
	}

	@Test
	public void normalizeUrlKeepsExistingScheme() {
		assertEquals("http://192.168.0.10:8080", ShortcutCodec.normalizeUrl("http://192.168.0.10:8080"));
		assertEquals("https://netflix.com", ShortcutCodec.normalizeUrl("HTTPS://netflix.com"));
	}

	@Test
	public void normalizeUrlOfBlankReturnsEmpty() {
		assertEquals("", ShortcutCodec.normalizeUrl("   "));
		assertEquals("", ShortcutCodec.normalizeUrl(null));
	}

	@Test
	public void moveUpSwapsWithPrevious() {
		List<Shortcut> list = Arrays.asList(new Shortcut("A", "a"), new Shortcut("B", "b"), new Shortcut("C", "c"));

		List<Shortcut> moved = ShortcutCodec.move(list, 2, -1);

		assertEquals(Arrays.asList(new Shortcut("A", "a"), new Shortcut("C", "c"), new Shortcut("B", "b")), moved);
		assertEquals("lista original não muda", "C", list.get(2).name);
	}

	@Test
	public void moveOutOfBoundsKeepsOrder() {
		List<Shortcut> list = Arrays.asList(new Shortcut("A", "a"), new Shortcut("B", "b"));

		assertEquals(list, ShortcutCodec.move(list, 0, -1));
		assertEquals(list, ShortcutCodec.move(list, 1, +1));
		assertEquals(list, ShortcutCodec.move(list, 5, -1));
	}

	@Test
	public void domainOfExtractsHost() {
		assertEquals("m.youtube.com", ShortcutCodec.domainOf("https://m.youtube.com/watch?v=1"));
		assertEquals("globoplay.globo.com", ShortcutCodec.domainOf("globoplay.globo.com"));
		assertEquals("192.168.0.10", ShortcutCodec.domainOf("http://192.168.0.10:8080/x"));
		assertEquals("", ShortcutCodec.domainOf(""));
	}

	@Test
	public void defaultsContainYouTubeFirst() {
		List<Shortcut> defaults = ShortcutCodec.defaults();

		assertEquals("YouTube", defaults.get(0).name);
		assertTrue(defaults.size() >= 4);
	}
}
