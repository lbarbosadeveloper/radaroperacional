package com.leo.meucartv;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;

import java.util.function.Consumer;

/** Tela de busca do próprio Android Auto, com o teclado do carro. */
public class SearchScreen extends Screen {
	private final Consumer<String> onSubmit;

	public SearchScreen(@NonNull CarContext carContext, Consumer<String> onSubmit) {
		super(carContext);
		this.onSubmit = onSubmit;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		return new SearchTemplate.Builder(new SearchTemplate.SearchCallback() {
			@Override
			public void onSearchTextChanged(@NonNull String searchText) {
			}

			@Override
			public void onSearchSubmitted(@NonNull String searchText) {
				getScreenManager().pop();
				if (!searchText.trim().isEmpty()) onSubmit.accept(searchText);
			}
		})
				.setHeaderAction(Action.BACK)
				.setSearchHint("YouTube ou endereço de site")
				.setShowKeyboardByDefault(true)
				.setItemList(new ItemList.Builder()
						.setNoItemsMessage("Digite e toque em buscar no teclado")
						.build())
				.build();
	}
}
