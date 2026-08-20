# Quantis

Quantis is an Android voice assistant. You talk into the phone; it listens, thinks, and speaks back. The session can be interrupted at any time.

Speech engines follow the **app language**:

| App language | Speech-to-text | Text-to-speech |
| --- | --- | --- |
| Chinese | Volcengine SAUC streaming ASR 2.0 | Volcengine `seed-tts-2.0` |
| English | OpenAI `gpt-4o-mini-transcribe` | OpenAI `tts-1` |

Replies come from an OpenAI-compatible chat model (OpenAI or DeepSeek). Chat mode stays short; Thinking mode answers in fuller spoken paragraphs.

This is a personal project, not an App Store or Play Store listing. There is no public web demo.

## Features

- Hands-free loop: start a session, speak, hear the reply, speak again
- Barge-in: tap or talk over playback to cut the assistant off
- Chat vs Thinking: two system prompts, two spoken lengths
- Language-aware voices: Volcengine speakers in Chinese, OpenAI voices in English, remembered separately
- Speech rate, theme (system / dark / light), haptics, optional in-session wake phrase
- Local conversation history with “continue voice” on an existing thread
- Optional PIN lock on cold start (`APP_LOCK_PASSWORD`)

Quantis is not a system wake-word app. “Hey Quantis” only works while a voice session is already running. It cannot replace a phone vendor assistant.

## Voice pipeline

```text
Microphone
    → energy VAD (end-of-utterance)
    → STT  (Volcengine or OpenAI, by UI language)
    → LLM  (OpenAI-compatible, Chat or Thinking prompt)
    → sentence-split TTS
    → speaker
         ↺ barge-in / next turn
```

The session runs in an Android foreground service so recording is less likely to be killed when the screen is off.

**Chinese TTS** uses Volcengine voices such as Vivi, 小何, 高冷御姐, 灿灿, 清新女声, 北京小爷, and 儒雅逸辰 (`*_uranus_bigtts` on `seed-tts-2.0`).

**English TTS** uses OpenAI `tts-1`: Alloy, Nova, Shimmer, Echo, Onyx, Fable.

## Architecture

```text
UI (Compose)
    Home / History / Settings / lock screen
        ↓
VoiceSessionService          ← foreground session + state machine
        ↓
ChatService + RoutingLlmClient
        ↓
OpenAI or DeepSeek (HTTPS / SSE)
```

Speech I/O is routed the same way:

- `RoutingSpeechToTextClient` → Volcengine WebSocket or OpenAI transcription
- `RoutingTextToSpeechClient` → Volcengine unidirectional TTS or OpenAI speech

Settings and history live on device (DataStore + Room). API keys are compiled into `BuildConfig` from a local `.env` file and are never stored in the UI.

### Layout

```text
app/src/main/java/com/waynelinnn/voiceagent/
  audio/          capture + VAD
  data/           STT, TTS, LLM transport, Room, DataStore
  domain/         models, repositories, conversation policy
  llm/            routing client, providers, prompt catalog
  presentation/   Compose screens
  service/        VoiceSessionService
app/src/main/assets/llm/prompts/
  voice_assistant.txt
  voice_assistant_thinking.txt
```

## Stack

- Kotlin, Jetpack Compose, Hilt
- Coroutines / Flow
- OkHttp (HTTPS, SSE, WebSocket)
- Room, DataStore
- minSdk 26, targetSdk 35, JDK 17

## Setup

1. Clone the repo and open it in Android Studio (or use the Gradle wrapper).
2. Copy `.env.example` to `.env` in the project root. Do not commit `.env`.

```env
OPENAI_API_KEY=
DEEPSEEK_API_KEY=
VOLC_API_KEY=
APP_LOCK_PASSWORD=
```

3. Fill at least one LLM key. For Chinese speech, create a Volcengine API key and enable **streaming ASR 2.0** and **TTS 2.0**:  
   [Volcengine speech API keys](https://console.volcengine.com/speech/new/setting/apikeys?projectName=default)
4. Rebuild after changing `.env` so `BuildConfig` picks up the values.

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/`. On some OEM devices (including vivo), `./gradlew installDebug` can time out; pushing the APK and using `pm install -r` is more reliable.

## Usage

1. Grant the microphone permission.
2. Unlock if you set `APP_LOCK_PASSWORD`.
3. Open **Settings** and set the app language (Chinese or English). That switch selects both STT and TTS.
4. Pick a voice and speech rate.
5. On Home, start a session and talk. Stop or interrupt from the same screen.
6. Open **History** to reread a thread or continue it by voice.

## Configuration notes

- Volcengine new-console auth is `X-Api-Key` only (`VOLC_API_KEY`).
- Chinese TTS speakers must be `seed-tts-2.0` / `*_uranus_bigtts` IDs. Older `*_moon_bigtts` voices do not match this resource.
- English STT defaults to `gpt-4o-mini-transcribe` (optional `gpt-4o-transcribe` in settings).
- A local Sherpa path exists in the codebase but is not the main recognition path.

## License

Private personal project. Ask before reuse.
