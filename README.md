# Twitch Mod Panel (Android MVP)

Панель модератора Twitch для Android — каркас MVP на **Kotlin + Jetpack Compose + Material 3**.

English (short): Open in Android Studio → Sync → set `TWITCH_CLIENT_ID` in `local.properties` → Run. Use stub login without a Client ID, or Device Code OAuth when configured.

## Требования

- Android Studio Ladybug / Koala+ (или новее) с JDK 17
- Android SDK 35, Min SDK 26
- Аккаунт [Twitch Developers](https://dev.twitch.tv/) для Client ID (опционально для stub-режима)

## Быстрый старт

1. Клонируйте / скопируйте проект:
   ```bash
   git clone https://github.com/D3FF96/twitch-mod-panel.git
   cd twitch-mod-panel
   ```
2. Откройте папку в **Android Studio** (Open → выбрать корень проекта).
3. Дождитесь Gradle Sync.
4. Скопируйте пример свойств:
   ```bash
   cp local.properties.example local.properties
   ```
5. В `local.properties` укажите:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   TWITCH_CLIENT_ID=ваш_client_id
   ```
6. Run → `app` (эмулятор или устройство).

### Stub без Client ID

На экране Login нажмите **«Продолжить без OAuth (stub)»** — список каналов и чат с демо-данными без реального токена.

### Реальный вход (Device Code Grant)

1. Зарегистрируйте приложение на [dev.twitch.tv](https://dev.twitch.tv/console/apps):
   - Category: Mobile / Other
   - OAuth Redirect URLs можно оставить пустым или указать `twitchmodpanel://oauth` (для будущего Authorization Code); **для Device Code login кастомный redirect не нужен**.
2. Скопируйте **Client ID** в `local.properties` → `TWITCH_CLIENT_ID=…` (попадёт в `BuildConfig`).
3. **Client Secret не кладите в приложение** — он не используется Device Code flow на клиенте и не должен попадать в APK.
4. Запустите приложение → **«Войти с Twitch»**:
   - появится `user_code` (крупно, можно скопировать тапом);
   - кнопка «Открыть Twitch» → `ACTION_VIEW` на `verification_uri`;
   - приложение опрашивает `POST /oauth2/token` с `grant_type=urn:ietf:params:oauth:grant-type:device_code`;
   - после подтверждения токены сохраняются (EncryptedSharedPreferences), профиль — через Helix `/users`, переход к каналам.
5. «Отменить вход» останавливает polling Job.

Ошибки (нет Client ID, сеть, отказ, истёкший код) показываются snackbar’ами на русском.

## Экраны

| Экран | Описание |
|--------|----------|
| **Login** | Device Code OAuth + stub-вход |
| **ChannelPicker** | Список каналов, где пользователь — модератор (sample data) |
| **Chat** | Лента, ответ (@mention), профиль по нику, stream panel (WebView embed), mod-действия, быстрые команды |
| **QuickCommands** | Локальный CRUD шаблонов ответов (DataStore) |

## Новые возможности (MVP)

### Ответ на сообщение
- Тап по **тексту** сообщения → reply: композер префиллится `@userLogin `, фокус на поле.
- Чип над композером: «Ответ @login» с кнопкой ✕ (снимает mention).
- Long-press или кнопка **⋯** → диалог mod-действий (+ Reply).

### Профиль по нику
- Тап по **display name** → bottom sheet: имя, @login, бейджи, stub bio.
- «Открыть на Twitch» → `ACTION_VIEW` на `https://www.twitch.tv/{login}`.
- «Ответить», Timeout / Ban.

### Stream panel
- **Свёрнуто (по умолчанию):** полоска ~56dp с названием канала, LIVE, mute/unmute, expand.
- **Развёрнуто:** ~⅓ высоты экрана — WebView Twitch embed  
  `https://player.twitch.tv/?channel={login}&parent=localhost&muted=…`
- Состояние: `streamExpanded`, `streamAudioEnabled` в `ChatUiState`.

## Архитектура

```
ui/          Compose screens + ViewModel + StateFlow
domain/      модели + интерфейсы репозиториев
data/        OAuth Device Code (OkHttp) + SecureTokenStore + stubs Helix/Chat/7TV + DataStore
```

- Navigation Compose (single-activity)
- Repository interfaces в `domain`, реализации в `data`
- OAuth: **Device Code Grant** (`TwitchDeviceCodeClient`, `TwitchAuthRepository`); scopes из `TwitchOAuthConfig`

## Что stub, что можно сделать «настоящим»

| Компонент | Сейчас | Дальше |
|-----------|--------|--------|
| OAuth / токены | **Device Code** + EncryptedSharedPreferences; stub sign-in | refresh token renewal, revoke |
| Helix (каналы) | `HelixApiStub` sample | Retrofit → moderated channels |
| Helix (user после OAuth) | реальный `GET /helix/users` | — |
| Chat | `ChatStub` (seed + таймер) | IRC / EventSub WebSocket |
| Mod actions | Локальный delete + no-op ban/timeout | Helix moderation endpoints |
| 7TV emotes | `SevenTvApiStub` URL-список | 7TV REST + Coil |
| Quick commands | DataStore Preferences (JSON) | уже «реально» локально |

## Redirect URI (опционально)

Device Code **не требует** redirect для логина. Intent-filter `twitchmodpanel://oauth` остаётся для возможного Authorization Code / AppAuth позже.

## Структура пакета

`com.d3ff96.twitchmodpanel` · applicationId тот же.

## Лицензия / статус

MVP scaffold — не production-ready. Секреты не коммитить: только `local.properties.example`. **Client Secret никогда не должен быть в приложении.**
