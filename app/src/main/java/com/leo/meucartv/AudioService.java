package com.leo.meucartv;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.service.media.MediaBrowserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de mídia mínimo. Apps de mídia passam pelo filtro "Fontes desconhecidas" do Android Auto;
 * declará-lo faz o Android Auto aceitar o pacote (mesma estratégia do Fermata Auto).
 * Ele só mostra uma dica apontando para o "Meu CarTV Tela".
 */
public class AudioService extends MediaBrowserService {
	private static final String ROOT = "raiz";
	private MediaSession session;

	@Override
	public void onCreate() {
		super.onCreate();
		session = new MediaSession(this, "MeuCarTV");
		setSessionToken(session.getSessionToken());
	}

	@Override
	public void onDestroy() {
		if (session != null) {
			session.release();
			session = null;
		}
		super.onDestroy();
	}

	@Override
	public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
		return new BrowserRoot(ROOT, null);
	}

	@Override
	public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
		List<MediaBrowser.MediaItem> items = new ArrayList<>();
		if (ROOT.equals(parentId)) {
			MediaDescription hint = new MediaDescription.Builder()
					.setMediaId("dica")
					.setTitle("Para assistir, abra \"Meu CarTV Tela\"")
					.setSubtitle("Fica na lista de apps do Android Auto")
					.build();
			items.add(new MediaBrowser.MediaItem(hint, MediaBrowser.MediaItem.FLAG_PLAYABLE));
		}
		result.sendResult(items);
	}
}
