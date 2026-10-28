package com.leo.meucartv;

import android.app.Activity;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Tela do celular: editar os atalhos e abrir a prévia da tela do carro. */
public class MainActivity extends Activity {
	private ShortcutStore store;
	private LinearLayout list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		store = new ShortcutStore(this);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.parseColor("#0E1116"));
		root.setPadding(dp(20), dp(28), dp(20), dp(20));

		TextView title = text("Meu CarTV", 28, Color.WHITE);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		root.addView(title);
		root.addView(text("Esses atalhos aparecem na tela do carro, no Android Auto. "
				+ "Toque num atalho para editar ou apagar.", 15, Color.LTGRAY));

		LinearLayout actions = new LinearLayout(this);
		actions.setOrientation(LinearLayout.HORIZONTAL);
		actions.setPadding(0, dp(16), 0, dp(8));
		actions.addView(button("+ Adicionar", v -> editDialog(-1)));
		actions.addView(button("Prévia da tela", v -> startActivity(new Intent(this, PreviewActivity.class))));
		actions.addView(button("Como ativar", v -> showSetupGuide()));
		root.addView(actions);

		AppPrefs prefs = new AppPrefs(this);
		Switch lock = new Switch(this);
		lock.setText("Pausar vídeo com o carro em movimento");
		lock.setTextColor(Color.WHITE);
		lock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		lock.setChecked(prefs.isMotionLockEnabled());
		lock.setPadding(0, dp(4), 0, dp(8));
		lock.setOnCheckedChangeListener((b, on) -> {
			prefs.setMotionLockEnabled(on);
			if (on) requestSpeedPermissions();
		});
		root.addView(lock);
		if (prefs.isMotionLockEnabled()) requestSpeedPermissions();

		list = new LinearLayout(this);
		list.setOrientation(LinearLayout.VERTICAL);
		ScrollView scroll = new ScrollView(this);
		scroll.addView(list);
		root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		Button reset = button("Restaurar atalhos padrão", v -> new AlertDialog.Builder(this)
				.setMessage("Voltar para YouTube, Netflix, Globoplay e Google? Os seus atalhos serão substituídos.")
				.setPositiveButton("Restaurar", (d, w) -> {
					store.resetToDefaults();
					refresh();
				})
				.setNegativeButton("Cancelar", null)
				.show());
		root.addView(reset);

		setContentView(root);
		refresh();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	private void refresh() {
		list.removeAllViews();
		List<Shortcut> shortcuts = store.load();
		if (shortcuts.isEmpty()) {
			list.addView(text("Nenhum atalho ainda. Toque em \"+ Adicionar\".", 16, Color.GRAY));
		}
		for (int i = 0; i < shortcuts.size(); i++) {
			final int index = i;
			Shortcut s = shortcuts.get(i);
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setPadding(dp(16), dp(10), dp(8), dp(10));
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(Color.parseColor("#1C222B"));
			bg.setCornerRadius(dp(12));
			row.setBackground(bg);

			LinearLayout info = new LinearLayout(this);
			info.setOrientation(LinearLayout.VERTICAL);
			TextView name = text(s.name, 18, Color.WHITE);
			name.setTypeface(Typeface.DEFAULT_BOLD);
			info.addView(name);
			info.addView(text(s.url, 13, Color.GRAY));
			row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			row.addView(arrow("↑", index > 0, v -> moveShortcut(index, -1)));
			row.addView(arrow("↓", index < shortcuts.size() - 1, v -> moveShortcut(index, +1)));
			row.setOnClickListener(v -> editDialog(index));
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, dp(6), 0, dp(6));
			list.addView(row, lp);
		}
	}

	private void moveShortcut(int index, int delta) {
		store.save(ShortcutCodec.move(store.load(), index, delta));
		refresh();
	}

	private TextView arrow(String label, boolean enabled, android.view.View.OnClickListener click) {
		TextView t = text(label, 24, enabled ? Color.WHITE : Color.DKGRAY);
		t.setGravity(Gravity.CENTER);
		t.setMinWidth(dp(44));
		t.setPadding(dp(8), dp(4), dp(8), dp(4));
		if (enabled) t.setOnClickListener(click);
		return t;
	}

	/** Velocidade do carro (Android Auto) e GPS, para o bloqueio em movimento. */
	private void requestSpeedPermissions() {
		List<String> missing = new ArrayList<>();
		for (String p : new String[]{"com.google.android.gms.permission.CAR_SPEED",
				Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}) {
			if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
		}
		if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), 1);
	}

	private void showSetupGuide() {
		new AlertDialog.Builder(this)
				.setTitle("Como ativar no carro")
				.setMessage("1. Abra as configurações do Android Auto no celular "
						+ "(Configurações > Dispositivos conectados > Android Auto).\n\n"
						+ "2. Role até o fim e toque 10 vezes em \"Versão\" até liberar o modo desenvolvedor.\n\n"
						+ "3. Toque nos três pontinhos ⋮ > Configurações do desenvolvedor > marque \"Fontes desconhecidas\".\n\n"
						+ "4. Em \"Personalizar acesso rápido\", marque o Meu CarTV Tela (e o Meu CarTV, se aparecer).\n\n"
						+ "5. Plugue no carro e abra o Meu CarTV Tela. Para rolar a página, toque no botão de mover "
						+ "(cruz) do Android Auto e arraste.")
				.setPositiveButton("Entendi", null)
				.show();
	}

	/** index = -1 adiciona um atalho novo; senão edita o existente. */
	private void editDialog(int index) {
		List<Shortcut> shortcuts = new ArrayList<>(store.load());
		boolean editing = index >= 0 && index < shortcuts.size();

		LinearLayout form = new LinearLayout(this);
		form.setOrientation(LinearLayout.VERTICAL);
		form.setPadding(dp(20), dp(8), dp(20), 0);
		EditText name = new EditText(this);
		name.setHint("Nome (ex.: YouTube)");
		name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		EditText url = new EditText(this);
		url.setHint("Site (ex.: m.youtube.com)");
		url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		if (editing) {
			name.setText(shortcuts.get(index).name);
			url.setText(shortcuts.get(index).url);
		}
		form.addView(name);
		form.addView(url);

		AlertDialog.Builder b = new AlertDialog.Builder(this)
				.setTitle(editing ? "Editar atalho" : "Novo atalho")
				.setView(form)
				.setPositiveButton("Salvar", (d, w) -> {
					String n = name.getText().toString().trim();
					String u = ShortcutCodec.normalizeUrl(url.getText().toString());
					if (n.isEmpty() || u.isEmpty()) {
						Toast.makeText(this, "Preencha o nome e o site.", Toast.LENGTH_SHORT).show();
						return;
					}
					Shortcut s = new Shortcut(n, u);
					if (editing) shortcuts.set(index, s);
					else shortcuts.add(s);
					store.save(shortcuts);
					refresh();
				})
				.setNegativeButton("Cancelar", null);
		if (editing) {
			b.setNeutralButton("Apagar", (d, w) -> {
				shortcuts.remove(index);
				store.save(shortcuts);
				refresh();
			});
		}
		b.show();
	}

	private TextView text(String value, int sp, int color) {
		TextView t = new TextView(this);
		t.setText(value);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
		t.setTextColor(color);
		return t;
	}

	private Button button(String label, android.view.View.OnClickListener click) {
		Button b = new Button(this);
		b.setText(label);
		b.setAllCaps(false);
		b.setGravity(Gravity.CENTER);
		b.setOnClickListener(click);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, 0, dp(8), 0);
		b.setLayoutParams(lp);
		return b;
	}

	private int dp(int v) {
		return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
	}
}
