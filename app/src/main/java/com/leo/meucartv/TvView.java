package com.leo.meucartv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A tela do "Meu CarTV": busca, "continuar", grade de atalhos e navegador embutido.
 * A mesma tela é usada no carro (MainCarActivity) e na prévia do celular (PreviewActivity).
 */
@SuppressLint("ViewConstructor")
public class TvView extends FrameLayout {

	/** Como criar e abrir o teclado: no carro usa o teclado do Android Auto, no celular o normal. */
	public interface Input {
		EditText create(Context context, Runnable onSubmit);

		void begin(EditText field);

		void end(EditText field);
	}

	private static final int BG = Color.parseColor("#0E1116");
	private static final int CARD = Color.parseColor("#1C222B");
	private static final int BUTTON = Color.parseColor("#2A323D");
	private static final int[] TILE_COLORS = {
			Color.parseColor("#C4302B"), Color.parseColor("#B20710"),
			Color.parseColor("#F26522"), Color.parseColor("#1A73E8"),
			Color.parseColor("#7B1FA2"), Color.parseColor("#00897B"),
			Color.parseColor("#5D4037"), Color.parseColor("#455A64")};
	private static final String PAUSE_MEDIA_JS =
			"document.querySelectorAll('video,audio').forEach(function(m){try{m.pause()}catch(e){}});";

	private static final Map<String, Bitmap> ICONS = new ConcurrentHashMap<>();
	private static final ExecutorService ICON_LOADER = Executors.newFixedThreadPool(3);

	private final Handler main = new Handler(Looper.getMainLooper());
	private final ShortcutStore store;
	private final AppPrefs prefs;
	private final Input input;
	private final FrameLayout homeLayer;
	private final LinearLayout browserLayer;
	private final FrameLayout fullscreenLayer;
	private final TextView blockedLayer;
	private final TextView pageTitle;
	private EditText searchField;
	private WebView webView;
	private View customView;
	private WebChromeClient.CustomViewCallback customViewCallback;
	private boolean blocked;

	public TvView(Context context, Input input) {
		super(context);
		this.input = input;
		store = new ShortcutStore(context);
		prefs = new AppPrefs(context);
		setBackgroundColor(BG);
		setKeepScreenOn(true);

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
		bar.addView(button("⌂  Início", v -> showHome()));
		bar.addView(button("←  Voltar", v -> goBack()));
		bar.addView(button("⟳", v -> {
			if (webView != null) webView.reload();
		}));
		bar.addView(button("⛶", v -> requestVideoFullscreen()));
		pageTitle = new TextView(context);
		pageTitle.setTextColor(Color.LTGRAY);
		pageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		pageTitle.setSingleLine(true);
		pageTitle.setEllipsize(TextUtils.TruncateAt.END);
		pageTitle.setPadding(dp(12), 0, dp(8), 0);
		bar.addView(pageTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		browserLayer.addView(bar, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		fullscreenLayer = new FrameLayout(context);
		fullscreenLayer.setBackgroundColor(Color.BLACK);
		fullscreenLayer.setVisibility(GONE);
		addView(fullscreenLayer, match());

		blockedLayer = new TextView(context);
		blockedLayer.setText("🚗  Carro em movimento\n\nO vídeo foi pausado por segurança.\nA tela volta sozinha quando o carro parar.");
		blockedLayer.setTextColor(Color.WHITE);
		blockedLayer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		blockedLayer.setGravity(Gravity.CENTER);
		blockedLayer.setBackgroundColor(Color.parseColor("#F20E1116"));
		blockedLayer.setClickable(true); // impede toques na página por baixo
		blockedLayer.setVisibility(GONE);
		addView(blockedLayer, match());

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

	/** Liga/desliga o bloqueio de carro em movimento. */
	public void setBlocked(boolean b) {
		if (blocked == b) return;
		blocked = b;
		blockedLayer.setVisibility(b ? VISIBLE : GONE);
		if (webView == null) return;
		if (b) {
			webView.evaluateJavascript(PAUSE_MEDIA_JS, null);
			webView.onPause();
		} else if (browserLayer.getVisibility() == VISIBLE) {
			webView.onResume();
		}
	}

	public void onPause() {
		if (searchField != null) input.end(searchField);
		if (webView != null) webView.onPause();
	}

	public void onResume() {
		if (webView != null && browserLayer.getVisibility() == VISIBLE && !blocked) webView.onResume();
		if (homeLayer.getVisibility() == VISIBLE) buildHome();
	}

	public void destroy() {
		exitFullscreen();
		main.removeCallbacksAndMessages(null);
		if (webView != null) {
			webView.stopLoading();
			webView.destroy();
			webView = null;
		}
		CookieManager.getInstance().flush();
	}

	// ---------- navegação ----------

	private void goBack() {
		if (customView != null) {
			exitFullscreen();
		} else if (webView != null && webView.canGoBack()) {
			webView.goBack();
		} else {
			showHome();
		}
	}

	private void open(String title, String url) {
		if (url == null || url.isEmpty()) return;
		if (searchField != null) input.end(searchField);
		WebView w = ensureWebView();
		homeLayer.setVisibility(GONE);
		browserLayer.setVisibility(VISIBLE);
		pageTitle.setText(title);
		if (!blocked) w.onResume();
		w.clearHistory();
		w.loadUrl(url);
	}

	private void search(boolean onYouTube) {
		if (searchField == null) return;
		String text = searchField.getText().toString();
		String url = onYouTube ? SearchUrls.youtube(text) : SearchUrls.web(text);
		if (url.isEmpty()) {
			input.begin(searchField);
			return;
		}
		open(text.trim(), url);
	}

	/** Pede ao vídeo da página para entrar em tela cheia (útil quando o botão do site é pequeno demais). */
	private void requestVideoFullscreen() {
		if (webView == null) return;
		webView.evaluateJavascript("(function(){var v=document.querySelector('video');"
				+ "if(!v)return;var f=v.requestFullscreen||v.webkitRequestFullscreen||v.webkitEnterFullscreen;"
				+ "if(f)f.call(v);})();", null);
	}

	// ---------- tela inicial ----------

	private void buildHome() {
		homeLayer.removeAllViews();
		Context ctx = getContext();

		LinearLayout column = new LinearLayout(ctx);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setPadding(dp(24), dp(16), dp(24), dp(20));

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
		hint.setPadding(0, dp(2), 0, dp(12));
		column.addView(hint);

		column.addView(searchRow());

		String lastUrl = prefs.getLastUrl();
		if (!lastUrl.isEmpty()) {
			String lastTitle = prefs.getLastTitle().isEmpty() ? lastUrl : prefs.getLastTitle();
			TextView cont = button("▶  Continuar: " + lastTitle, v -> open(lastTitle, lastUrl));
			cont.setSingleLine(true);
			cont.setEllipsize(TextUtils.TruncateAt.END);
			cont.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(dp(8), dp(12), dp(8), dp(4));
			column.addView(cont, lp);
		}

		List<Shortcut> shortcuts = store.load();
		if (shortcuts.isEmpty()) {
			TextView empty = new TextView(ctx);
			empty.setText("Nenhum atalho. Adicione pelo app no celular.");
			empty.setTextColor(Color.LTGRAY);
			empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			empty.setPadding(dp(8), dp(16), 0, 0);
			column.addView(empty);
		}
		GridLayout grid = new GridLayout(ctx);
		grid.setColumnCount(columnsFor(getResources().getDisplayMetrics().widthPixels));
		for (int i = 0; i < shortcuts.size(); i++) {
			grid.addView(tile(shortcuts.get(i), TILE_COLORS[i % TILE_COLORS.length]));
		}
		column.addView(grid);

		ScrollView scroll = new ScrollView(ctx);
		scroll.addView(column);
		homeLayer.addView(scroll, match());
	}

	private View searchRow() {
		Context ctx = getContext();
		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);

		if (searchField != null) input.end(searchField);
		searchField = input.create(ctx, () -> search(true));
		searchField.setHint("Pesquisar no YouTube ou digitar um site");
		searchField.setHintTextColor(Color.GRAY);
		searchField.setTextColor(Color.WHITE);
		searchField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		searchField.setSingleLine(true);
		searchField.setPadding(dp(16), dp(12), dp(16), dp(12));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(CARD);
		bg.setCornerRadius(dp(12));
		searchField.setBackground(bg);
		EditText field = searchField;
		field.setOnClickListener(v -> input.begin(field));
		LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		flp.setMargins(dp(8), 0, dp(4), 0);
		row.addView(field, flp);

		row.addView(button("▶ YouTube", v -> search(true)));
		row.addView(button("🌐 Web", v -> search(false)));
		return row;
	}

	private int columnsFor(int widthPx) {
		return Math.max(2, Math.min(5, widthPx / dp(200)));
	}

	private View tile(Shortcut s, int color) {
		TextView t = new TextView(getContext());
		t.setText(s.name);
		t.setTextColor(Color.WHITE);
		t.setTypeface(Typeface.DEFAULT_BOLD);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		t.setGravity(Gravity.CENTER);
		t.setMaxLines(2);
		t.setEllipsize(TextUtils.TruncateAt.END);
		t.setPadding(dp(8), dp(8), dp(8), dp(8));
		t.setCompoundDrawablePadding(dp(6));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(color);
		bg.setCornerRadius(dp(16));
		t.setBackground(bg);
		t.setClickable(true);
		t.setFocusable(true);
		String url = ShortcutCodec.normalizeUrl(s.url);
		t.setOnClickListener(v -> open(s.name, url));
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
		lp.width = dp(180);
		lp.height = dp(120);
		lp.setMargins(dp(8), dp(8), dp(8), dp(8));
		t.setLayoutParams(lp);
		loadIcon(ShortcutCodec.domainOf(url), t);
		return t;
	}

	/** Busca o ícone do site (favicon) em segundo plano e coloca acima do nome do atalho. */
	private void loadIcon(String domain, TextView target) {
		if (domain.isEmpty()) return;
		Bitmap cached = ICONS.get(domain);
		if (cached != null) {
			setIcon(target, cached);
			return;
		}
		ICON_LOADER.execute(() -> {
			Bitmap bmp = null;
			HttpURLConnection c = null;
			try {
				c = (HttpURLConnection) new URL(
						"https://www.google.com/s2/favicons?sz=128&domain=" + domain).openConnection();
				c.setConnectTimeout(5000);
				c.setReadTimeout(5000);
				try (InputStream in = c.getInputStream()) {
					bmp = BitmapFactory.decodeStream(in);
				}
			} catch (Exception ignored) {
				// Sem internet ou sem ícone: o atalho fica só com o nome.
			} finally {
				if (c != null) c.disconnect();
			}
			if (bmp == null || bmp.getWidth() < 24) return; // ícone genérico minúsculo: ignora
			ICONS.put(domain, bmp);
			Bitmap result = bmp;
			main.post(() -> {
				if (target.isAttachedToWindow()) setIcon(target, result);
			});
		});
	}

	private void setIcon(TextView t, Bitmap bmp) {
		BitmapDrawable d = new BitmapDrawable(getResources(), bmp);
		int size = dp(44);
		d.setBounds(0, 0, size, size);
		t.setCompoundDrawables(null, d, null, null);
	}

	private TextView button(String label, OnClickListener click) {
		TextView b = new TextView(getContext());
		b.setText(label);
		b.setTextColor(Color.WHITE);
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		b.setTypeface(Typeface.DEFAULT_BOLD);
		b.setGravity(Gravity.CENTER);
		b.setMinWidth(dp(56));
		b.setPadding(dp(16), dp(12), dp(16), dp(12));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(BUTTON);
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

	// ---------- navegador ----------

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
		st.setBuiltInZoomControls(true);
		st.setDisplayZoomControls(false);
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

			@Override
			public void onPageFinished(WebView view, String url) {
				if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
					prefs.setLastPage(url, view.getTitle());
				}
			}

			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
				if (!request.isForMainFrame()) return;
				String failed = request.getUrl().toString();
				String html = "<html><body style='background:#0E1116;color:#fff;font-family:sans-serif;"
						+ "text-align:center;padding-top:15%'><h2>Não deu para abrir a página</h2>"
						+ "<p style='color:#aaa'>Confira a internet do celular e tente de novo.</p>"
						+ "<p><a style='color:#fff;background:#2A323D;padding:16px 28px;border-radius:10px;"
						+ "text-decoration:none;font-size:20px' href='" + TextUtils.htmlEncode(failed)
						+ "'>Tentar de novo</a></p></body></html>";
				view.loadDataWithBaseURL(null, html, "text/html", "utf-8", failed);
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

			@Override
			public Bitmap getDefaultVideoPoster() {
				// Evita o "play" cinza enorme que o WebView mostra antes do vídeo carregar.
				return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
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
