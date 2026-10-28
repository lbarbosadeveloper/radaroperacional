package com.leo.meucartv;

import android.content.Context;
import android.support.car.input.CarRestrictedEditText;
import android.view.inputmethod.EditorInfo;

import com.google.android.gms.car.input.CarEditable;
import com.google.android.gms.car.input.CarEditableListener;

/** Campo de texto que usa o teclado da tela do carro (mesma técnica do Fermata Auto). */
public class CarEditText extends CarRestrictedEditText implements CarEditable {
	interface OnSubmit {
		void submit();
	}

	private OnSubmit onSubmit;

	public CarEditText(Context context) {
		super(context, null);
		setImeOptions(EditorInfo.IME_ACTION_SEARCH);
	}

	void setOnSubmit(OnSubmit s) {
		onSubmit = s;
	}

	@Override
	public void setCarEditableListener(CarEditableListener listener) {
		super.setCarEditableListener(listener == null ? null : listener::onUpdateSelection);
	}

	@Override
	public void onEditorAction(int actionCode) {
		switch (actionCode) {
			case EditorInfo.IME_ACTION_GO:
			case EditorInfo.IME_ACTION_SEARCH:
			case EditorInfo.IME_ACTION_SEND:
			case EditorInfo.IME_ACTION_NEXT:
			case EditorInfo.IME_ACTION_DONE:
				if (onSubmit != null) {
					onSubmit.submit();
					return;
				}
		}
		super.onEditorAction(actionCode);
	}
}
