package com.leo.meucartv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SearchUrlsTest {

	@Test
	public void plainWordsSearchOnYouTube() {
		assertEquals("https://m.youtube.com/results?search_query=jornal+nacional",
				SearchUrls.youtube("  jornal nacional "));
	}

	@Test
	public void specialCharactersAreEncoded() {
		assertEquals("https://m.youtube.com/results?search_query=a%26b+%C3%A7%C3%A3o",
				SearchUrls.youtube("a&b ção"));
	}

	@Test
	public void addressLikeTextOpensTheSite() {
		assertEquals("https://g1.globo.com", SearchUrls.web("g1.globo.com"));
		assertEquals("https://x.com/abc", SearchUrls.web("https://x.com/abc"));
	}

	@Test
	public void sentenceWithDotIsStillASearch() {
		assertEquals("https://www.google.com/search?q=placar+do+jogo+ontem.",
				SearchUrls.web("placar do jogo ontem."));
	}

	@Test
	public void wordsSearchOnGoogle() {
		assertEquals("https://www.google.com/search?q=previs%C3%A3o+do+tempo",
				SearchUrls.web("previsão do tempo"));
	}

	@Test
	public void blankInputGivesEmpty() {
		assertEquals("", SearchUrls.web("  "));
		assertEquals("", SearchUrls.youtube(null));
	}
}
