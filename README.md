# Minima — AI-Native Android Launcher

A minimal launcher with a **universal AI-assistant layer**. Pick Gemini, ChatGPT,
Grok, or Claude once — every launcher entry point (dock bar, search,
long-press Home, text selection) routes to your pick.

No vendor SDKs, no API keys. Integration is via the assistant **apps you
already have installed** (deep link → `ACTION_SEND` → Play Store / web fallback).

## Features

- **Home launcher** (`MAIN` / `HOME`): favorites grid, full app drawer, search.
- **AI dock bar**: `Search apps or ask <AI>…` — app match launches, otherwise the
  query is sent to your default AI. Mic button = voice entry.
- **AI picker**: tap the AI chip to switch default (persisted in DataStore).
  Missing app → Play Store / web fallback, never a crash.
- **System Assist**: `AssistHandlerActivity` handles `ASSIST` / `VOICE_COMMAND`.
  Set Minima as *Default digital assistant* and long-press Home / corner swipe
  opens the Ask sheet instead of Google. Voice gesture jumps straight to the
  assistant's voice UI.
- **Ask AI about selection**: `AskAiActivity` handles `PROCESS_TEXT` + `SEND`,
  so any app's *select text → Ask Minima AI* sends context to your assistant.

## Project layout

```
app/src/main/java/com/minima/launcher/
  MainActivity.kt                 HOME entry
  data/AppModel.kt, AppRepository.kt, LauncherViewModel.kt
  ai/AIAssistant.kt               universal interface + 4 providers
  ai/AssistantRegistry.kt         add a provider here = UI-wide support
  ai/AIManager.kt                 default persistence, safe launch, fallbacks
  ai/AssistHandlerActivity.kt     ASSIST / VOICE_COMMAND overlay
  ai/AskAiActivity.kt             PROCESS_TEXT / SEND target
  ui/HomeScreen.kt, AppDrawer.kt  Compose UI
  ui/theme/MinimaTheme.kt
```

## Add a new assistant (e.g. Copilot, Perplexity, on-device LLM)

1. Add an `object XAssistant : AIAssistant` in `ai/AIAssistant.kt`
   (`packageName`, `buildTextIntent`, `buildVoiceIntent`, `webFallbackUrl`).
2. Register it in `AssistantRegistry.all`.
3. Done — dock, picker, Assist sheet, and search pick it up automatically.

## Build & run

```bash
# Requires Android Studio Hedgehog+ / AGP 8.5, JDK 17
local.properties:  sdk.dir=/path/to/Android/Sdk
./gradlew :app:installDebug
# Then: press Home → choose Minima → "Always"
# Optional: Settings → Apps → Default apps → Digital assistant → Minima
```

Key config: `minSdk 29`, `targetSdk 34`, Compose + Material3, no API keys needed.

## Notes / limits

- `QUERY_ALL_PACKAGES` is used for the app list; on Play releases you must
  justify it or switch to the `<intent>`-filtered query path (already declared).
- Full `VoiceInteractionService` (hotword, TTS callbacks) is intentionally out
  of scope — `ASSIST` + assistant-app deep links cover Gemini/ChatGPT/Grok/Claude
  without privileged permissions.
- App icons use `LauncherApps.getBadgedIcon` (work-profile aware).
