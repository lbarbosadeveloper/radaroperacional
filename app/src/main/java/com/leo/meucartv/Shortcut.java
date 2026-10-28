package com.leo.meucartv;

import java.util.Objects;

/** Um atalho da tela inicial: nome que aparece no botão e o site que ele abre. */
public final class Shortcut {
	public final String name;
	public final String url;

	public Shortcut(String name, String url) {
		this.name = name;
		this.url = url;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Shortcut)) return false;
		Shortcut s = (Shortcut) o;
		return Objects.equals(name, s.name) && Objects.equals(url, s.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, url);
	}

	@Override
	public String toString() {
		return name + " -> " + url;
	}
}
