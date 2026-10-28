package com.leo.meucartv;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * Desenha a TvView na superfície do carro usando uma tela virtual (VirtualDisplay) e uma Presentation.
 * Os toques que o Android Auto informa (clique e arrastar) viram toques de verdade na TvView.
 */
public class SurfaceTvScreen extends Screen implements SurfaceCallback {
	private static final String TAG = "MeuCarTV";
	private static final long SCROLL_END_DELAY_MS = 300;

	private final Handler main = new Handler(Looper.getMainLooper());
	private VirtualDisplay display;
	private Presentation presentation;
	private FrameLayout container;
	private TvView tv;
	private Rect visibleArea;
	private int width;
	private int height;

	private boolean scrolling;
	private long scrollDownTime;
	private float scrollX;
	private float scrollY;
	private final Runnable endScroll = this::finishScroll;

	public SurfaceTvScreen(@NonNull CarContext carContext) {
		super(carContext);
		carContext.getCarService(AppManager.class).setSurfaceCallback(this);
		getLifecycle().addObserver(new DefaultLifecycleObserver() {
			@Override
			public void onDestroy(@NonNull LifecycleOwner owner) {
				release();
			}
		});
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		Action home = new Action.Builder()
				.setIcon(icon(R.drawable.ic_home))
				.setOnClickListener(() -> ui(() -> {
					if (tv != null) tv.showHome();
				}))
				.build();
		Action back = new Action.Builder()
				.setIcon(icon(R.drawable.ic_back))
				.setOnClickListener(() -> ui(() -> {
					if (tv != null) tv.handleBack();
				}))
				.build();
		return new NavigationTemplate.Builder()
				.setActionStrip(new ActionStrip.Builder().addAction(home).addAction(back).build())
				.setMapActionStrip(new ActionStrip.Builder().addAction(Action.PAN).build())
				.build();
	}

	private CarIcon icon(int res) {
		return new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), res)).build();
	}

	// ---------- superfície ----------

	@Override
	public void onSurfaceAvailable(@NonNull SurfaceContainer sc) {
		if (sc.getSurface() == null) return;
		ui(() -> {
			width = sc.getWidth();
			height = sc.getHeight();
			try {
				if (display == null) {
					DisplayManager dm = (DisplayManager) getCarContext().getSystemService(Context.DISPLAY_SERVICE);
					display = dm.createVirtualDisplay("MeuCarTV", width, height, sc.getDpi(), sc.getSurface(),
							DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
					showPresentation();
				} else {
					display.resize(width, height, sc.getDpi());
					display.setSurface(sc.getSurface());
				}
				if (tv != null) tv.onResume();
				applyVisibleArea();
			} catch (RuntimeException e) {
				Log.e(TAG, "Falha ao desenhar na tela do carro", e);
			}
		});
	}

	@Override
	public void onSurfaceDestroyed(@NonNull SurfaceContainer sc) {
		ui(() -> {
			if (tv != null) tv.onPause();
			if (display != null) display.setSurface(null);
		});
	}

	@Override
	public void onVisibleAreaChanged(@NonNull Rect area) {
		ui(() -> {
			visibleArea = new Rect(area);
			applyVisibleArea();
		});
	}

	private void showPresentation() {
		presentation = new Presentation(getCarContext(), display.getDisplay());
		container = new FrameLayout(presentation.getContext());
		tv = new TvView(presentation.getContext(), new TemplateKeyboard());
		container.addView(tv, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		presentation.setContentView(container);
		presentation.show();
	}

	/** Deixa a TvView fora da área coberta pelos botões do Android Auto. */
	private void applyVisibleArea() {
		if (container == null || visibleArea == null || visibleArea.isEmpty()) return;
		container.setPadding(Math.max(0, visibleArea.left), Math.max(0, visibleArea.top),
				Math.max(0, width - visibleArea.right), Math.max(0, height - visibleArea.bottom));
	}

	private void release() {
		main.removeCallbacksAndMessages(null);
		if (tv != null) {
			tv.destroy();
			tv = null;
		}
		if (presentation != null) {
			presentation.dismiss();
			presentation = null;
		}
		if (display != null) {
			display.release();
			display = null;
		}
		container = null;
	}

	// ---------- toques ----------

	@Override
	public void onClick(float x, float y) {
		ui(() -> {
			if (scrolling) finishScroll();
			long t = SystemClock.uptimeMillis();
			touch(MotionEvent.ACTION_DOWN, t, x, y);
			main.postDelayed(() -> touch(MotionEvent.ACTION_UP, t, x, y), 60);
		});
	}

	@Override
	public void onScroll(float distanceX, float distanceY) {
		ui(() -> {
			long now = SystemClock.uptimeMillis();
			if (!scrolling) {
				scrolling = true;
				scrollDownTime = now;
				scrollX = visibleArea != null ? visibleArea.exactCenterX() : width / 2f;
				scrollY = visibleArea != null ? visibleArea.exactCenterY() : height / 2f;
				touch(MotionEvent.ACTION_DOWN, now, scrollX, scrollY);
			}
			scrollX -= distanceX;
			scrollY -= distanceY;
			touch(MotionEvent.ACTION_MOVE, scrollDownTime, scrollX, scrollY);
			main.removeCallbacks(endScroll);
			main.postDelayed(endScroll, SCROLL_END_DELAY_MS);
		});
	}

	private void finishScroll() {
		main.removeCallbacks(endScroll);
		if (!scrolling) return;
		scrolling = false;
		touch(MotionEvent.ACTION_UP, scrollDownTime, scrollX, scrollY);
	}

	private void touch(int action, long downTime, float x, float y) {
		if (presentation == null || presentation.getWindow() == null) return;
		View root = presentation.getWindow().getDecorView();
		MotionEvent ev = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
		ev.setSource(InputDevice.SOURCE_TOUCHSCREEN);
		root.dispatchTouchEvent(ev);
		ev.recycle();
	}

	private void ui(Runnable r) {
		if (Looper.myLooper() == Looper.getMainLooper()) r.run();
		else main.post(r);
	}

	// ---------- teclado ----------

	/** Ao tocar no campo de busca, abre a tela de busca do Android Auto, que tem o teclado do carro. */
	private final class TemplateKeyboard implements TvView.Input {
		@Override
		public EditText create(Context context, Runnable onSubmit) {
			EditText field = new EditText(context);
			field.setFocusable(false); // não chama o teclado do celular
			field.setTag(onSubmit);
			return field;
		}

		@Override
		public void begin(EditText field) {
			Runnable submit = (Runnable) field.getTag();
			getScreenManager().push(new SearchScreen(getCarContext(), text -> ui(() -> {
				field.setText(text);
				if (submit != null) submit.run();
			})));
		}

		@Override
		public void end(EditText field) {
		}
	}
}
