package com.leo.meucartv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * A tela do "Meu CarTV": grade de atalhos e navegador embutido.
 * A mesma tela é usada no carro (MainCarActivity) e na prévia do celular (PreviewActivity).
 */
@SuppressLint("ViewConstructor")
public class TvView extends FrameLayout {
	private static final int BG = Color.parseColor("#0E1116");
	private static final int CARD = Color.parseColor("#1C222B");
	private static final int[] TILE_COLORS = {
			Color.parseColor("#C4302B"), Color.parseColor("#B20710"),
			Color.parseColor("#F26522"), Color.parseColor("#1A73E8"),
			Color.parseColor("#7B1FA2"), Color.parseColor("#00897B"),
			Color.parseColor("#5D4037"), Color.parseColor("#455A64")};

	private final ShortcutStore store;
	private final FrameLayout homeLayer;
	private final LinearLayout browserLayer;
	private final FrameLayout fullscreenLayer;
	private final TextView pageTitle;
	private WebView webView;
	private View customView;
	private WebChromeClient.CustomViewCallback customViewCallback;

	public TvView(Context context) {
		super(context);
		store = new ShortcutStore(context);
		setBackgroundColor(BG);

		homeLayer = new FrameLayout(context);
		addView(homeLayer, match());

		browserLayer = new LinearLayout(context);
		browserLayer.setOrientation(LinearLayout.VERTICAL);
		browserLayer.setVisibility(GONE);
		addView(browserLayer, match());

		LinearLayout bar = new LinearLayout(context);
		bar.setOrientation(LinearLayout.HORIZONTAL);
		bar.setGravity(Gravity.CENTER_VERTICAL);
		bar.setBackgroundColor(CARD);
		int pad = dp(6);
		bar.setPadding(pad, pad, pad, pad);
		bar.addView(barButton("⌂  Início", v -> showHome()));
		bar.addView(barButton("←  Voltar", v -> goBack()));
		bar.addView(barButton("⟳", v -> {
			if (webView != null) webView.reload();
		}));
		pageTitle = new TextView(context);
		pageTitle.setTextColor(Color.LTGRAY);
		pageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		pageTitle.setSingleLine(true);
		pageTitle.setPadding(dp(12), 0, dp(8), 0);
		bar.addView(pageTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		browserLayer.addView(bar, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		fullscreenLayer = new FrameLayout(context);
		fullscreenLayer.setBackgroundColor(Color.BLACK);
		fullscreenLayer.setVisibility(GONE);
		addView(fullscreenLayer, match());

		buildHome();
	}

	/** Recarrega os atalhos (por exemplo, depois de editar no celular) e mostra a tela inicial. */
	public void showHome() {
		exitFullscreen();
		if (webView != null) {
			webView.loadUrl("about:blank");
			webView.onPause();
		}
		browserLayer.setVisibility(GONE);
		buildHome();
		homeLayer.setVisibility(VISIBLE);
	}

	/** Botão voltar: sai da tela cheia, volta no site ou retorna à tela inicial. Devolve false se já está no início. */
	public boolean handleBack() {
		if (customView != null) {
			exitFullscreen();
			return true;
		}
		if (browserLayer.getVisibility() == VISIBLE) {
			goBack();
			return true;
		}
		return false;
	}

	public void onPause() {
		if (webView != null) webView.onPause();
	}

	public void onResume() {
		if (webView != null && browserLayer.getVisibility() == VISIBLE) webView.onResume();
		if (homeLayer.getVisibility() == VISIBLE) buildHome();
	}

	public void destroy() {
		exitFullscreen();
		if (webView != null) {
			webView.stopLoading();
			webView.destroy();
			webView = null;
		}
		CookieManager.getInstance().flush();
	}

	private void goBack() {
		if (customView != null) {
			exitFullscreen();
		} else if (webView != null && webView.canGoBack()) {
			webView.goBack();
		} else {
			showHome();
		}
	}

	private void open(Shortcut s) {
		WebView w = ensureWebView();
		homeLayer.setVisibility(GONE);
		browserLayer.setVisibility(VISIBLE);
		pageTitle.setText(s.name);
		w.onResume();
		w.clearHistory();
		w.loadUrl(ShortcutCodec.normalizeUrl(s.url));
	}

	private void buildHome() {
		homeLayer.removeAllViews();
		Context ctx = getContext();

		LinearLayout column = new LinearLayout(ctx);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setPadding(dp(24), dp(20), dp(24), dp(20));

		TextView title = new TextView(ctx);
		title.setText("Meu CarTV");
		title.setTextColor(Color.WHITE);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
		column.addView(title);

		TextView hint = new TextView(ctx);
		hint.setText("Assista só com o carro parado. Edite os atalhos no app do celular.");
		hint.setTextColor(Color.GRAY);
		hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		hint.setPadding(0, dp(4), 0, dp(16));
		column.addView(hint);

		List<Shortcut> shortcuts = store.load();
		GridLayout grid = new GridLayout(ctx);
		grid.setColumnCount(columnsFor(getResources().getDisplayMetrics().widthPixels));
		for (int i = 0; i < shortcuts.size(); i++) {
			Shortcut s = shortcuts.get(i);
			grid.addView(tile(s, TILE_COLORS[i % TILE_COLORS.length]));
		}
		if (shortcuts.isEmpty()) {
			TextView empty = new TextView(ctx);
			empty.setText("Nenhum atalho. Adicione pelo app no celular.");
			empty.setTextColor(Color.LTGRAY);
			empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			column.addView(empty);
		}
		column.addView(grid);

		ScrollView scroll = new ScrollView(ctx);
		scroll.addView(column);
		homeLayer.addView(scroll, match());
	}

	private int columnsFor(int widthPx) {
		int tileWithMargin = dp(200);
		return Math.max(2, Math.min(5, widthPx / tileWithMargin));
	}

	private View tile(Shortcut s, int color) {
		TextView t = new TextView(getContext());
		t.setText(s.name);
		t.setTextColor(Color.WHITE);
		t.setTypeface(Typeface.DEFAULT_BOLD);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		t.setGravity(Gravity.CENTER);
		t.setMaxLines(2);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(color);
		bg.setCornerRadius(dp(16));
		t.setBackground(bg);
		t.setClickable(true);
		t.setFocusable(true);
		t.setOnClickListener(v -> open(s));
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
		lp.width = dp(180);
		lp.height = dp(110);
		lp.setMargins(dp(8), dp(8), dp(8), dp(8));
		t.setLayoutParams(lp);
		return t;
	}

	private TextView barButton(String label, OnClickListener click) {
		TextView b = new TextView(getContext());
		b.setText(label);
		b.setTextColor(Color.WHITE);
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		b.setTypeface(Typeface.DEFAULT_BOLD);
		b.setGravity(Gravity.CENTER);
		b.setMinWidth(dp(64));
		b.setPadding(dp(16), dp(10), dp(16), dp(10));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#2A323D"));
		bg.setCornerRadius(dp(10));
		b.setBackground(bg);
		b.setClickable(true);
		b.setOnClickListener(click);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.setMargins(dp(4), 0, dp(4), 0);
		b.setLayoutParams(lp);
		return b;
	}

	@SuppressLint("SetJavaScriptEnabled")
	private WebView ensureWebView() {
		if (webView != null) return webView;
		WebView w = new WebView(getContext());
		w.setBackgroundColor(Color.BLACK);
		WebSettings st = w.getSettings();
		st.setJavaScriptEnabled(true);
		st.setDomStorageEnabled(true);
		st.setMediaPlaybackRequiresUserGesture(false);
		st.setLoadWithOverviewMode(true);
		st.setUseWideViewPort(true);
		st.setBuiltInZoomControls(false);
		st.setSupportMultipleWindows(false);
		// Sem o marcador "wv", alguns sites (como o login do Google) tratam como navegador normal.
		st.setUserAgentString(st.getUserAgentString().replace("; wv", ""));
		CookieManager cookies = CookieManager.getInstance();
		cookies.setAcceptCookie(true);
		cookies.setAcceptThirdPartyCookies(w, true);

		w.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				String scheme = request.getUrl().getScheme();
				// Links de app (intent://, vnd.youtube:// etc.) não abrem na tela do carro: ignora.
				return !"http".equals(scheme) && !"https".equals(scheme);
			}
		});
		w.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
				enterFullscreen(view, callback);
			}

			@Override
			public void onHideCustomView() {
				exitFullscreen();
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				if (title != null && !title.startsWith("http")) pageTitle.setText(title);
			}
		});
		browserLayer.addView(w, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		webView = w;
		return w;
	}

	private void enterFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
		if (customView != null) {
			callback.onCustomViewHidden();
			return;
		}
		customView = view;
		customViewCallback = callback;
		fullscreenLayer.addView(view, match());
		fullscreenLayer.setVisibility(VISIBLE);
	}

	private void exitFullscreen() {
		if (customView == null) return;
		fullscreenLayer.removeView(customView);
		fullscreenLayer.setVisibility(GONE);
		customView = null;
		WebChromeClient.CustomViewCallback cb = customViewCallback;
		customViewCallback = null;
		if (cb != null) cb.onCustomViewHidden();
	}

	private static LayoutParams match() {
		return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	}

	private int dp(int v) {
		return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
	}
}
