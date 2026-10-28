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
4. Plugue no carro e procure, na lista de apps do Android Auto, o **Meu CarTV Tela** (modo navegação, tela cheia)
   ou o **Meu CarTV**. O **Meu CarTV Áudio** só existe para ajudar o Android Auto a aceitar o app.

## Recursos

- Grade de atalhos com o ícone de cada site, editável e reordenável no app do celular.
- Busca na tela do carro com o teclado do Android Auto: pesquisa direto no YouTube ou abre um site/busca no Google.
- "Continuar": volta para a última página assistida.
- Navegador com Início, Voltar, Recarregar e um botão para forçar o vídeo em tela cheia.
- Bloqueio com o carro em movimento: pausa o vídeo acima de 10 km/h e libera após 5 s parado.
  Usa a velocidade do carro pelo Android Auto e, se o carro não informar, o GPS do celular.
  Vem desligado; dá para ligar no app do celular.
- Tela sempre acesa enquanto o app está aberto e página amigável quando falta internet.

## Limitações conhecidas

- Netflix e Globoplay usam proteção contra cópia (DRM) e podem não reproduzir no navegador embutido. YouTube funciona.
- Versões recentes do Android Auto escondem apps instalados fora da Play Store. O modo navegação e o
  serviço de mídia são tentativas de contornar isso, sem garantia.
- A integração com o Android Auto usa um SDK não oficial (`app/libs/aauto.aar`), o mesmo usado pelo projeto
  [Fermata Auto](https://github.com/AndreyPavlenko/Fermata). O Google não documenta esse caminho e pode mudar o
  Android Auto a qualquer momento.
- Alguns carros desativam o teclado da tela com o carro andando (regra do próprio Android Auto).
- Se nem o carro nem o GPS informarem a velocidade, o bloqueio em movimento não funciona: assista só parado.

## Estrutura

- `ShortcutCodec` / `Shortcut`: lista de atalhos, reordenação e endereços (com testes em `app/src/test`).
- `SpeedGate`: regra do bloqueio em movimento (com testes). `CarSpeedSource` / `GpsSpeedSource`: fontes de velocidade.
- `SearchUrls`: transforma o texto digitado em busca ou endereço (com testes).
- `CarEditText`: campo que usa o teclado da tela do carro.
- `ShortcutStore`: salva os atalhos no celular.
- `TvView`: a tela (grade de atalhos + navegador + tela cheia), usada no carro e na prévia.
- `CarService` / `MainCarActivity`: entrada do Android Auto (SDK não oficial).
- `ScreenService` / `SurfaceTvScreen` / `SearchScreen`: modo navegação — desenha a TvView na superfície do carro
  por uma tela virtual e repassa os toques; a busca usa o teclado do Android Auto.
- `AudioService`: serviço de mídia mínimo para o app passar no filtro de Fontes desconhecidas.
- `MainActivity` / `PreviewActivity`: telas do celular.

O histórico anterior deste repositório (projeto Radar Operacional) continua disponível nos commits antigos.
