package com.leo.meucartv;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/** Transforma o que foi digitado em endereço: busca no YouTube, busca no Google ou o próprio site. */
public final class SearchUrls {
	// Algo como "g1.globo.com", "x.com/abc" ou "https://...": sem espaços e com um domínio.
	private static final Pattern ADDRESS = Pattern.compile(
			"^(https?://)?[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}(:\\d+)?(/\\S*)?$");

	private SearchUrls() {
	}

	public static String youtube(String query) {
		String q = trim(query);
		if (q.isEmpty()) return "";
		return "https://m.youtube.com/results?search_query=" + encode(q);
	}

	public static String web(String text) {
		String t = trim(text);
		if (t.isEmpty()) return "";
		if (ADDRESS.matcher(t).matches()) return ShortcutCodec.normalizeUrl(t);
		return "https://www.google.com/search?q=" + encode(t);
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
