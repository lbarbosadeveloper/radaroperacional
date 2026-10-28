package com.leo.meucartv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Converte a lista de atalhos para texto (para salvar no celular) e de volta.
 * Formato: uma linha por atalho, "nome<TAB>url". Sem dependência do Android, para ser testável.
 */
public final class ShortcutCodec {
	private ShortcutCodec() {
	}

	public static List<Shortcut> defaults() {
		return Arrays.asList(
				new Shortcut("YouTube", "https://m.youtube.com"),
				new Shortcut("Netflix", "https://www.netflix.com"),
				new Shortcut("Globoplay", "https://globoplay.globo.com"),
				new Shortcut("Google", "https://www.google.com"));
	}

	public static String encode(List<Shortcut> shortcuts) {
		StringBuilder sb = new StringBuilder();
		for (Shortcut s : shortcuts) {
			sb.append(clean(s.name)).append('\t').append(clean(s.url)).append('\n');
		}
		return sb.toString();
	}

	public static List<Shortcut> decode(String text) {
		List<Shortcut> list = new ArrayList<>();
		if (text == null || text.isEmpty()) return list;
		for (String line : text.split("\n")) {
			int tab = line.indexOf('\t');
			if (tab <= 0) continue;
			String name = line.substring(0, tab).trim();
			String url = line.substring(tab + 1).trim();
			if (name.isEmpty() || url.isEmpty()) continue;
			list.add(new Shortcut(name, url));
		}
		return list;
	}

	/** Remove espaços e coloca https:// quando o endereço não tem protocolo. */
	public static String normalizeUrl(String url) {
		if (url == null) return "";
		String u = url.trim();
		if (u.isEmpty()) return "";
		int sep = u.indexOf("://");
		if (sep > 0) return u.substring(0, sep).toLowerCase(Locale.ROOT) + u.substring(sep);
		return "https://" + u;
	}

	/** Devolve uma cópia da lista com o item em {@code index} movido {@code delta} posições (−1 sobe, +1 desce). */
	public static List<Shortcut> move(List<Shortcut> list, int index, int delta) {
		List<Shortcut> copy = new ArrayList<>(list);
		int target = index + delta;
		if (index < 0 || index >= copy.size() || target < 0 || target >= copy.size()) return copy;
		Shortcut item = copy.remove(index);
		copy.add(target, item);
		return copy;
	}

	/** Só o nome do servidor de um endereço: "https://m.youtube.com/x" vira "m.youtube.com". */
	public static String domainOf(String url) {
		String u = normalizeUrl(url);
		if (u.isEmpty()) return "";
		int start = u.indexOf("://") + 3;
		int end = u.length();
		for (char c : new char[]{'/', '?', '#', ':'}) {
			int i = u.indexOf(c, start);
			if (i >= 0 && i < end) end = i;
		}
		return u.substring(start, end).toLowerCase(Locale.ROOT);
	}

	private static String clean(String s) {
		if (s == null) return "";
		return s.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
	}
}
