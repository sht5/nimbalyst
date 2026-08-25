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
  home, menu, source, media transport (rewind/play-pause/fast-forward), and
  a numeric keypad
- Automatically falls back from the encrypted port (8002) to the plaintext
  port (8001) if the TV doesn't answer on 8002
- **Turns the TV back on from fully off**, via Wake-on-LAN — see below. The
  remote screen stays put when the TV drops off the network instead of
  bouncing back to IP entry, since Power is the only way back in.

## Turning the TV on when it's fully off

The remote-control websocket above only exists while the TV's own OS
(Tizen) is running, so once the TV is off there's nothing on the network to
connect to — the same reason a physical remote's power button doesn't use
Wi-Fi either, it talks to a receiver chip (IR or Bluetooth) that Samsung
keeps powered in standby. This app has no IR/Bluetooth-remote hardware, so
it uses the network-side equivalent instead: a **Wake-on-LAN magic packet**
addressed to the TV's MAC — the same mechanism the SmartThings app uses.

- The MAC is captured automatically, from the TV's plaintext device-info
  endpoint (`http://<tv-ip>:8001/api/v2/`), the first time you pair —
  nothing to type in.
- This only works if the TV's **"Power on with mobile"** setting is on
  (under **Settings → General → Network → Expert Settings** on most
  models) — the same setting SmartThings depends on. If a wake signal goes
  unanswered after ~15 seconds, the remote screen says so.

## Before your TV will respond

On the TV: **Settings → General → External Device Manager → Device Connect
Manager** — make sure it's set to allow connections (or "First Time
Connection" is not set to block everything). Some models are under
**Settings → General → Network → Expert Settings**, which is also where
**Power on with mobile** lives (see above).

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
  network/SamsungTvClient.kt  Websocket client: connect, pair, send keys, wake
  network/WakeOnLan.kt        Sends the Wake-on-LAN magic packet
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
