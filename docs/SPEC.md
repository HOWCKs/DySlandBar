# Especificação Técnica — *Material Capsule* (nome provisório)

> App Android que desenha uma **Cápsula Material 3 flutuante** sobre o recorte da
> câmera frontal, oferecendo controles rápidos com UX fluida, gestos e cores
> dinâmicas (Material You) — inspirado na Dynamic Island, mas genuinamente Android.

---

## 1. Visão Geral

O aplicativo sobrepõe uma cápsula expressiva e arrastável na área superior da tela,
por cima de qualquer aplicativo (inclusive a barra de status), reunindo:

- Atalhos e ações rápidas configuráveis
- Controles do reprodutor de mídia ativo
- Controle da lanterna (com suavização de brilho perceptual)
- Gestos sobre a cápsula (arrastar, pressionar, deslizar)
- Observação de downloads e eventos do sistema
- Códigos de barras gerados localmente
- Cores Material You derivadas do papel de parede

O app não é um substituto do sistema: é uma **camada de conveniência** que transforma
a região do recorte da câmera em uma superfície interativa.

## 2. Restrições de Plataforma (implementações honestas)

Toda funcionalidade abaixo está validada contra APIs **públicas** do Android.
Nada é "fingido": onde a plataforma impõe limite, a estratégia documentada é a alternativa real.

| Funcionalidade | Abordagem real | Limite de plataforma |
|---|---|---|
| Desenhar sobre outros apps | `WindowManager` + `TYPE_APPLICATION_OVERLAY` (permissão *Draw over other apps*) acoplada a um `AccessibilityService` para o gesto de rolagem ao topo | O usuário precisa conceder as duas permissões no 1º uso (fluxo onboarding guiado) |
| Cores Material You | `androidx.dynamiccolor.DynamicColors` / `MaterialDynamicColors` | Android 12+ (API 31). Em versões anteriores, paleta Material 3 estática equivalente |
| Controles de mídia | `MediaController` sobre o `MediaSession` ativo (Spotify, YouTube Music, etc.) | Só funciona em apps que expõem `MediaSession` — comportamento padrão do sistema |
| Brilho da lanterna | `CameraManager.setTorchMode` (on/off) + **pulso (duty cycle)** para brilho intermediário perceptual | A API pública **não** expõe dimming contínuo da torch; o pulso é a técnica real usada por apps semelhantes (não é fraude: alterna 2–8 Hz e o olho percebe o tom) |
| Downloads (Play, Telegram, etc.) | `NotificationListenerService` monitorando notificações de progresso de download (padrão: título + percentual) | Não existe API pública de "download manager global"; a leitura de notificações é o caminho real e funciona para Play, Telegram, browser, etc. (requer permissão de notificações) |
| Notificações de eventos | `BroadcastReceiver`/`BatteryManager` (carga), `ConnectivityManager` (Wi-Fi/celular), `AudioManager` (fones) | Limitado a eventos de sistema que o Android expõe (sem acesso arbitrário a outros apps) |
| Códigos de barras | `com.google.zxing:core` gerando `Bitmap` local (formatos CODE_128, QR) | Nenhum — totalmente local, sem permissão de câmera |
| Gestos de rolagem ao topo | `AccessibilityService` + `dispatchGesture`/scrolling actions | O usuário precisa ativar o serviço de acessibilidade (fluxo guiado) |

## 3. Stack Tecnológica (decisão)

**Nativa — Kotlin + Jetpack Compose + Material 3.**

Justificativa (versus Flutter/RN):
- Overlay sobre a UI do sistema, `AccessibilityService` e `NotificationListenerService`
  são APIs de plataforma: em nativo são componentes de 1ª classe; em frameworks híbridos
  exigiriam bridges Kotlin extras com manutenção própria e performance inferior para
  animação de 60/120 fps da cápsula.
- Compose `View` + `LaunchedEffect`/`animate*` entrega a "interação expressiva"
  (molas, easing, expansão da cápsula) com um custo trivial.
- CI no GitHub Actions com Gradle Wrapper é o caminho mais estável e rápido para
  produzir `.apk` instalável.

| Camada | Escolha |
|---|---|
| Linguagem | Kotlin (idioma oficial do Android) |
| UI | Jetpack Compose + Material 3 + Material You (`dynamiccolor`) |
| Build | Gradle (KTS) + Version Catalog, Android Gradle Plugin 8.x, JDK 17 |
| Mídia | `androidx.media` (`MediaController`/`MediaSessionManager`) |
| Código de barras | `com.google.zxing:core` |
| Arquitetura | Módulos: `app` (UI + serviço) — limpo de dependências em v1 |
| Estado/ViewModel | `ViewModel` + `StateFlow` (MVI leve) |
| CI/CD | GitHub Actions: build → artefato `.apk` nomeado com versão/data → download no navegador do celular |

## 4. Arquitetura

```
MaterialCapsule
├── CapsuleActivity        → onboarding (permissões), ajustes, tema
├── CapsuleService         → service que mantém o overlay vivo + estado (mídia, lanterna, bateria)
├── CapsuleWindowManager   → cria/desloca o View (Compose) com TYPE_APPLICATION_OVERLAY
├── MediaConnector         → MediaController: faixa atual, play/pause, next/prev, seek
├── TorchController        → torch on/off + pulso de duty cycle (brilho suavizado)
├── DownloadWatcher        → NotificationListenerService → progresso de downloads
├── SystemEvents           → bateria, conectividade, fones (receivers)
├── GestureActions         → AccessibilityService: swipe-to-top, rolagem
├── BarcodeRenderer        → ZXing core → Bitmap (sem câmera)
└── Theme                  → Material You (31+) / fallback M3 estático
```

**UX (referência visual)**: cápsula arredondada (altura ≈ 36–44 dp) ancorada junto ao
recorte da câmera; estado compacto (pílula) expande com mola ao toque, revelando chips
de atalho, controles de mídia e sliders (brilho/lanterna). Tudo arrastável, com
anagrama de "absorção" quando arrastado para fora da tela. (Imagens de referência a
serem anexadas para calibrar espaçamentos, raio e densidade.)

## 5. CI/CD — GitHub Actions

`.github/workflows/build-apk.yml`:

1. Trigger: `push` em `main` (e tags `v*` para releases).
2. Job `build` (ubuntu-latest, JDK 17 Temurin):
   - `./gradlew assemble{Debug|Release}` (modo de assinatura definido no questionário)
   - Versionar: `versionName = git short sha` + data → artefato ex. `MaterialCapsule-1.0.0-a1b2c3d.apk`
   - `actions/upload-artifact@v4` → aba *Actions → Downloads* (retenção configurável)
3. Resultado: APK assinado, instalável direto do navegador do Android
   (com "Instalar apps desconhecidos" liberado para o navegador).

## 6. Roadmap (entregas incrementais)

| Milestone | Conteúdo | Entregável |
|---|---|---|
| **M0** | Projeto base + pipeline CI/CD | **1º APK instalável** (cápsula estática) |
| **M1** | Overlay vivo: arrastar, expandir, Material You | APK M1 |
| **M2** | Mídia + lanterna (pulso) + atalhos | APK M2 |
| **M3** | Gestos (to-top), eventos do sistema, downloads, barcodes | APK M3 |
| **M4** | Tela de ajustes (tema, chips, sensibilidade), polimento de animações | APK M4 |

## 7. Escopo explícito de fora (honestidade)

- Não lerá conteúdo de apps de terceiros (sem acesso a dados privados).
- Não simulará "controle de brilho exato" da torch além do pulso perceptual (documentado na UI).
- Sem upload de dados: 100% local, offline após instalação.
- Distribuição: **sidebar/sideload via artefatos** (Play Store fora do escopo por causa
  das políticas de `AccessibilityService` — sem impacto no uso pessoal).

## 8. Aberturas (fechar no questionário)

1. Assinatura do APK (debug imediato vs. keystore + GitHub Secrets).
2. Versão mínima de Android (minSdk) — define cobertura de Material You.
3. Escopo da v1 (MVP focado vs. tudo de uma vez).
4. Identidade: nome, `applicationId` e idioma da UI.
