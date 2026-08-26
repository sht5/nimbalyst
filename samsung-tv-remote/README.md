# Samsung TV Remote

A small native Android app (Kotlin + Jetpack Compose) that turns your phone into a
remote for a Samsung Smart TV — and, on the same screen, for an Android TV /
Google TV streamer (Chromecast with Google TV, Shield, Mecool, etc.) plugged
into one of its HDMI inputs. It talks directly to both devices over your local
Wi-Fi network — no cloud account, no SmartThings/Google login, no internet
access required beyond the phone and TV/streamer being on the same network.

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

## Controlling a streamer plugged into the TV

Certified Android TV / Google TV devices (Chromecast with Google TV, NVIDIA
Shield, most Google TV-branded sets, and Google-certified boxes like Mecool's
Android TV models) run a local **Android TV Remote** service — the same one
the official "Android TV Remote Control" and Google Home apps use. It's a
different protocol from the Samsung one above:

- **Discovery** is mDNS (`_androidtvremote2._tcp`) via Android's built-in
  `NsdManager`, not SSDP.
- **Pairing** (port 6467) is a TLS handshake using a self-signed certificate
  the app generates once in the Android Keystore. The streamer shows a
  6-digit code on screen; typing it back in proves both sides hold the
  certs a SHA-256 hash was computed over — there's no bearer token, the
  certificate itself is the credential the streamer remembers.
- **Control** (port 6466) is a persistent TLS connection carrying
  protobuf-encoded key presses (`network/androidtv/RemoteMessages.kt`) using
  Android's own standard key codes.

The remote screen's TV identity block becomes a two-device switcher once a
streamer is paired: tapping its pill both re-points button presses at it and
sends the TV's own Source key, mirroring what pressing Source on the
physical remote does. Volume stays routed to whichever device's pill is
active, since a streamer's volume keys typically control the TV over HDMI-CEC
passthrough anyway. Unlike the Samsung side, there's no Wake-on-LAN flow
here — switching HDMI input to the streamer wakes it on its own.

Wire-format details (field numbers, the pairing-secret hash, framing) are
ported directly from the open-source reference client
[tronikos/androidtvremote2](https://github.com/tronikos/androidtvremote2)
rather than hand-rolled protobuf definitions, since a wrong field number
there fails silently with no useful error from the TV side.

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
  MainActivity.kt              Compose entry point
  RemoteApp.kt                  Navigation: Connect, Remote, Add-streamer screens
  TvRemoteViewModel.kt          Holds connection state for both devices, exposed to Compose
  data/RemoteKeys.kt            Samsung remote key codes
  data/AndroidTvKey.kt          Android TV remote key codes
  data/DeviceKind.kt            Which device the remote screen currently targets
  data/TvPrefs.kt                Saved Samsung IP + pairing token (SharedPreferences)
  data/AndroidTvPrefs.kt         Saved streamer IP + paired flag (SharedPreferences)
  network/SamsungTvClient.kt    Websocket client: connect, pair, send keys, wake
  network/WakeOnLan.kt          Sends the Wake-on-LAN magic packet
  network/TvDiscovery.kt        SSDP scan for Samsung TVs on the LAN
  network/androidtv/AndroidTvClient.kt      TLS pairing + persistent control connection
  network/androidtv/AndroidTvCertificate.kt Keystore-backed self-signed client cert
  network/androidtv/PairingSecret.kt        The pairing-code hash/verification
  network/androidtv/Proto.kt                Minimal hand-rolled protobuf wire format
  network/androidtv/PoloMessages.kt         Pairing-channel messages (port 6467)
  network/androidtv/RemoteMessages.kt       Control-channel messages (port 6466)
  network/androidtv/AndroidTvDiscovery.kt   mDNS scan for Android TV devices on the LAN
  ui/ConnectScreen.kt           IP entry + discovery results (Samsung)
  ui/AndroidTvConnectScreen.kt  IP entry + discovery + pairing-code dialog (streamer)
  ui/RemoteScreen.kt            The remote control surface, both device panes
  ui/components/DPad.kt         Directional pad component
```

## Notes on the self-signed certificate

The TV's encrypted port (8002) presents a self-signed TLS certificate with no
public CA behind it — this is normal for Samsung's local control API, and every
third-party implementation of this protocol has to accept it explicitly.
`SamsungTvClient` uses a dedicated `OkHttpClient` (never reused for anything
else) with a trust manager that accepts the TV's certificate, scoped strictly
to that one local connection.
