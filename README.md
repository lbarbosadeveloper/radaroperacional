# Meu CarTV

App Android para uso próprio que mostra, na tela do carro pelo Android Auto, uma grade de atalhos
(YouTube, Netflix, Globoplay, Google ou o que você quiser) e um navegador embutido para assistir com o carro parado.

## Como baixar o APK

Cada envio para a branch `main` dispara o GitHub Actions (aba **Actions**), que roda os testes e gera o APK.
O arquivo `MeuCarTV.apk` fica na página **Releases** do repositório, pronta para baixar direto pelo celular.

## Como instalar e ativar no carro

1. Baixe o `MeuCarTV.apk` pelo celular Android e instale (permita a instalação dessa fonte quando pedir).
2. Abra o app **Meu CarTV** no celular: edite os atalhos e use **Prévia da tela** para testar sem o carro.
3. No Android Auto do celular: toque 10 vezes em **Versão** para liberar o modo desenvolvedor, depois
   menu (⋮) → **Configurações do desenvolvedor** → marque **Fontes desconhecidas**.
4. Plugue no carro e procure o **Meu CarTV** na tela do Android Auto.

## Limitações conhecidas

- Netflix e Globoplay usam proteção contra cópia (DRM) e podem não reproduzir no navegador embutido. YouTube funciona.
- A integração com o Android Auto usa um SDK não oficial (`app/libs/aauto.aar`), o mesmo usado pelo projeto
  [Fermata Auto](https://github.com/AndreyPavlenko/Fermata). O Google não documenta esse caminho e pode mudar o
  Android Auto a qualquer momento.
- Não há teclado na tela do carro: os atalhos são editados no app do celular.
- Ainda não há bloqueio automático com o carro em movimento. Assista só parado.

## Estrutura

- `ShortcutCodec` / `Shortcut`: lista de atalhos e normalização de endereços (com testes em `app/src/test`).
- `ShortcutStore`: salva os atalhos no celular.
- `TvView`: a tela (grade de atalhos + navegador + tela cheia), usada no carro e na prévia.
- `CarService` / `MainCarActivity`: entrada do Android Auto.
- `MainActivity` / `PreviewActivity`: telas do celular.

O histórico anterior deste repositório (projeto Radar Operacional) continua disponível nos commits antigos.
