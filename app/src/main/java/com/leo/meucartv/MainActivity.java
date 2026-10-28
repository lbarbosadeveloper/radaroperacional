package com.leo.meucartv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
		root.addView(actions);

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
			row.setOrientation(LinearLayout.VERTICAL);
			row.setPadding(dp(16), dp(12), dp(16), dp(12));
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(Color.parseColor("#1C222B"));
			bg.setCornerRadius(dp(12));
			row.setBackground(bg);
			TextView name = text(s.name, 18, Color.WHITE);
			name.setTypeface(Typeface.DEFAULT_BOLD);
			row.addView(name);
			row.addView(text(s.url, 13, Color.GRAY));
			row.setOnClickListener(v -> editDialog(index));
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, dp(6), 0, dp(6));
			list.addView(row, lp);
		}
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
