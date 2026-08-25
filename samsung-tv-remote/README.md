# Samsung TV Remote

A small native Android app (Kotlin + Jetpack Compose) that turns your phone into a
remote for a Samsung Smart TV. It talks directly to the TV over your local Wi-Fi
network — no cloud account, no SmartThings login, no internet access required
beyond the phone and TV being on the same network.

This is a standalone project, unrelated to the rest of this repository. It lives at
the repo root (rather than under `packages/`) because it isn't part of the
Nimbalyst npm workspace or its build.

## How it works

Samsung Smart TVs (2016+, Tizen-based) run a local websocket control server:

- `wss://<tv-ip>:8002/api/v2/channels/samsung.remote.control` (encrypted, most models)
- `ws://<tv-ip>:8001/api/v2/channels/samsung.remote.control` (plaintext fallback)

The first time the app connects, the TV shows an on-screen "Allow this device to
connect?" prompt — accept it with your physical remote once. The TV hands back a
pairing token, which the app stores locally and reuses on future connections so
you won't see the prompt again (unless you reset the TV's device list).

Button presses are sent as small JSON messages over that same websocket, e.g.:

```json
{
  "method": "ms.remote.control",
  "params": {
    "Cmd": "Click",
    "DataOfCmd": "KEY_VOLUP",
    "Option": "false",
    "TypeOfRemote": "SendRemoteKey"
  }
}
```

This is the same protocol Samsung's own SmartThings app and third-party tools
(`samsungctl`, `samsung-tv-ws-api`, etc.) use — there's no official public spec,
but it's stable and widely documented by the home-automation community.

## Features

- Manual TV IP entry, with the last-used IP remembered
- "Discover TVs" — SSDP/UPnP scan of the local network to find Samsung TVs
  automatically
- Power, volume (+/−/mute), channel (+/−), directional pad, OK/Enter, back,
  home, menu, source, and a numeric keypad
- Automatically falls back from the encrypted port (8002) to the plaintext
  port (8001) if the TV doesn't answer on 8002

## Before your TV will respond

On the TV: **Settings → General → External Device Manager → Device Connect
Manager** — make sure it's set to allow connections (or "First Time
Connection" is not set to block everything). Some models are under
**Settings → General → Network → Expert Settings**.

## Installing without building

`apk/SamsungTvRemote-debug.apk` is a prebuilt debug APK, kept up to date with
`app/`. Copy it to your phone and open it to sideload, or `adb install
apk/SamsungTvRemote-debug.apk`. See `apk/README.md` for details.

## Building

1. Open the `samsung-tv-remote/` folder directly in Android Studio (Koala or
   newer). Android Studio will bootstrap the Gradle wrapper automatically on
   first sync — the wrapper jar itself isn't checked into this repo.
   - If you'd rather build from the command line, run `gradle wrapper` once
     inside this directory (with a local Gradle install) to generate
     `gradlew` / `gradlew.bat`, then use those from then on.
2. Build/run the `app` module on a device or emulator on the same Wi-Fi
   network as your TV. (An emulator's Wi-Fi is virtual, so a physical device
   works best for actually reaching the TV.)

Requires JDK 17, compileSdk/targetSdk 35, minSdk 26.

## Project layout

```
app/src/main/java/com/tvremote/samsung/
  MainActivity.kt            Compose entry point
  RemoteApp.kt                Navigation between Connect and Remote screens
  TvRemoteViewModel.kt        Holds connection state, exposed to Compose
  data/RemoteKeys.kt          Samsung remote key codes
  data/TvPrefs.kt             Saved IP + pairing token (SharedPreferences)
  network/SamsungTvClient.kt  Websocket client: connect, pair, send keys
  network/TvDiscovery.kt      SSDP scan for Samsung TVs on the LAN
  ui/ConnectScreen.kt         IP entry + discovery results
  ui/RemoteScreen.kt          The remote control surface
  ui/components/DPad.kt       Directional pad component
```

## Notes on the self-signed certificate

The TV's encrypted port (8002) presents a self-signed TLS certificate with no
public CA behind it — this is normal for Samsung's local control API, and every
third-party implementation of this protocol has to accept it explicitly.
`SamsungTvClient` uses a dedicated `OkHttpClient` (never reused for anything
else) with a trust manager that accepts the TV's certificate, scoped strictly
to that one local connection.
