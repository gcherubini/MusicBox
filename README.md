# MusicBox

## 🎵 Android Music Catalog App

An **Android application** displaying a **catalog of electronic music tracks**.
Inspired by platforms like **Beatport**, it focuses on providing **detailed information**
about each track **without streaming functionality** (at least initially).

---

## 📱 Project Overview

- A list of tracks displayed in a clean, scrollable catalog.
- A detail view for each track, including:
  - **Track name**
  - **Artist**
  - **Release title**
  - **Record label**
  - Additional metadata

> Unlike Beatport, the app does **not include audio playback** at this stage.
> It is intended as an **informational resource** — a curated database of music releases for artists, labels, or fans to explore.

---

## 🛠️ Backend (`MusicBox-backend/`)

Real local backend — **Ktor 3.5.2 + Exposed 1.5.0 + SQLite** — serving the catalog via REST
(full CRUD; the app itself only reads). First boot seeds the 10 catalog items into
`MusicBox-backend/data/musicbox.db`.

| Route | Success | Errors |
|---|---|---|
| `GET /musics` | 200 `Music[]` | 500 |
| `GET /musics/{id}` | 200 `Music` | 404 |
| `POST /musics` | 201 `Music` | 400, 409 |
| `PUT /musics/{id}` | 200 `Music` | 400, 404 |
| `DELETE /musics/{id}` | 204 | 404 |

Errors always respond JSON: `{ "error": "<message>" }`.

**Run it** (PowerShell — `java` is not on PATH, JDK must be set):

```powershell
cd MusicBox-backend
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
.\gradlew run
```

Server listens on `http://0.0.0.0:8080`. Verify with `curl http://localhost:8080/musics`.

---

## 📲 App (Clean Architecture + MVI)

```
presentation ──▶ domain ◀── data
```

- `domain`: models, repository interface, use cases (pure Kotlin, no Android).
- `data`: Retrofit (`http://10.0.2.2:8080/` — host `localhost` as seen by the emulator), DTOs, mappers.
- `presentation`: Compose screens with MVI contracts (`Intent` / `UiState` / `Effect`), manual DI via `AppContainer`.

**Run it:** start the backend first, then open an emulator and run
`./gradlew :app:installDebug` (or Run in Android Studio).

---

## ✅ Tests

- Backend: `./gradlew test` inside `MusicBox-backend/` (CRUD routes).
- App: `./gradlew :app:testDebugUnitTest` (mappers, use cases, ViewModels).
