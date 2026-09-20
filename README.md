# Twitch Mod Panel (Android MVP)

Панель модератора Twitch для Android — каркас MVP на **Kotlin + Jetpack Compose + Material 3**.

English (short): Open in Android Studio → Sync → set `TWITCH_CLIENT_ID` in `local.properties` → Run. Most APIs are stubs so the UI works without real tokens.

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
3. Дождитесь Gradle Sync. Если wrapper отсутствует, Studio предложит его сгенерировать.
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

На экране Login можно нажать **«Продолжить без OAuth (stub)»** — приложение откроет список каналов и чат с демо-данными без реального токена.

## Экраны

| Экран | Описание |
|--------|----------|
| **Login** | Placeholder OAuth Twitch + пояснение про Client ID; stub-вход |
| **ChannelPicker** | Список каналов, где пользователь — модератор (sample data) |
| **Chat** | Лента сообщений, композер, статус стрима, mod-действия (timeout/ban/delete/unban), быстрые команды |
| **QuickCommands** | Локальный CRUD шаблонов ответов (DataStore) |

## Архитектура

```
ui/          Compose screens + ViewModel + StateFlow
domain/      модели + интерфейсы репозиториев
data/        stub-реализации Helix / Chat / 7TV + DataStore + OAuth config
```

- Navigation Compose (single-activity)
- Repository interfaces в `domain`, stubs в `data`
- OAuth: структура под **AppAuth** (`TwitchOAuthConfig`, deep link `twitchmodpanel://oauth`), пока без полного Custom Tabs flow

## Что stub, что можно сделать «настоящим»

| Компонент | Сейчас | Дальше |
|-----------|--------|--------|
| OAuth / токены | Stub sign-in; Client ID из BuildConfig | AppAuth + TokenRequest |
| Helix (каналы, user) | `HelixApiStub` sample | Retrofit → `api.twitch.tv/helix` |
| Chat | `ChatStub` (seed + таймер) | IRC / EventSub WebSocket |
| Mod actions | Локальный delete + no-op ban/timeout | Helix moderation endpoints |
| 7TV emotes | `SevenTvApiStub` URL-список | 7TV REST + Coil |
| Quick commands | DataStore Preferences (JSON) | уже «реально» локально |

## Redirect URI

В консоли Twitch добавьте OAuth Redirect URL:

```
twitchmodpanel://oauth
```

Intent-filter уже прописан в `AndroidManifest.xml`.

## Структура пакета

`com.d3ff96.twitchmodpanel` · applicationId тот же.

## Лицензия / статус

MVP scaffold — не production-ready. Секреты не коммитить: только `local.properties.example`.
