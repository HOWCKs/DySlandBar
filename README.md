# DySlandBar

Cápsula material para Android, inspirada na Dynamic Island e pensada para transformar o recorte da câmera em uma área útil do sistema.

## Análise do repositório antes desta configuração

O checkout recebido continha apenas um `README.md` com o título `PortMobileTerraria-Tlaucher`. Não havia:

- projeto Android ou módulo `app`;
- `settings.gradle`, `build.gradle` ou Gradle Wrapper;
- `AndroidManifest.xml`, código-fonte ou recursos de interface;
- workflow em `.github/workflows`;
- APK ou qualquer configuração de assinatura/build.

Portanto, não existia um aplicativo compilável nem era possível apenas “ativar” um CI/CD sem criar a base do projeto. O nome do app e o `applicationId` foram alinhados para `DySlandBar` nesta etapa.

## O que foi implementado

- projeto Android nativo em Java, sem dependências de UI de terceiros;
- `compileSdk 35`, `targetSdk 35`, `minSdk 26` e Java 17;
- tela inicial funcional com prévia visual da cápsula e painel de ativação;
- integração inicial e explícita com `AccessibilityService` usando apenas uma janela `TYPE_ACCESSIBILITY_OVERLAY`;
- configuração que não habilita leitura de conteúdo de janelas (`canRetrieveWindowContent=false`) e não inspeciona eventos;
- ícone, tema escuro e identidade visual inicial;
- workflow `.github/workflows/android.yml` que compila `assembleDebug`, calcula SHA-256 e publica o APK como artefato.

Esta é uma fundação compilável, não uma alegação de que todos os recursos do prompt original já estejam prontos. Controles de mídia, widgets, downloads, códigos de barras, gestos de produção, Material You dinâmico e integrações de notificações ainda precisam ser implementados e testados em aparelhos reais.

## Como baixar o APK pelo GitHub Actions

1. Faça push desta branch para o GitHub.
2. Abra a aba **Actions** e selecione **Android APK**.
3. Abra a execução concluída com sucesso.
4. Em **Artifacts**, baixe `DySlandBar-debug-apk`.
5. Extraia o ZIP e instale `DySlandBar-debug.apk` no aparelho. Pelo ADB:

   ```bash
   adb install -r DySlandBar-debug.apk
   ```

O workflow também pode ser disparado manualmente em **Run workflow**. Ele usa Java 17, Gradle 8.9, Android SDK 35 e gera um APK **debug assinado pela chave de debug**, adequado para instalação e testes. Para distribuição pública será necessário configurar uma chave de release em secrets do GitHub; ela não deve ser commitada no repositório.

## Como testar a cápsula

1. Abra o app instalado.
2. Toque em **Abrir configurações de acessibilidade**.
3. Ative **Cápsula DySlandBar**.
4. Volte ao app e ligue **Mostrar cápsula sobre outros apps**.

O desenho sobre outros apps usa a janela própria de acessibilidade, portanto não foi adicionada uma permissão genérica de overlay. O Android pode mostrar avisos próprios ao habilitar serviços de acessibilidade; o usuário deve conceder a permissão conscientemente.

## Desenvolvimento local

Com Java 17, Android SDK 35 e Gradle 8.9 instalados:

```bash
gradle --no-daemon assembleDebug
```

O mesmo comando é executado no GitHub Actions. O APK local fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Próximas etapas sugeridas

1. Testar a janela de acessibilidade em versões Android e recortes de câmera diferentes.
2. Adicionar controles de mídia com `MediaSessionManager`, respeitando as permissões e sessões disponíveis.
3. Modelar notificações e downloads com APIs oficiais, sem coletar conteúdo além do necessário.
4. Implementar widgets e códigos de barras com fluxos de permissão claros.
5. Adicionar gestos com `dispatchGesture` e testes instrumentados.
6. Criar build de release assinado por secrets do GitHub e publicar uma Release apenas após testes em dispositivos reais.
