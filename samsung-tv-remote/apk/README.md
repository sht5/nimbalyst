# Prebuilt APK

`SamsungTvRemote-debug.apk` is a debug build of the app in `../app`, signed with
Android's standard auto-generated debug key (not a release keystore) — normal
for sideloading, not for a Play Store submission.

To install: copy it to your phone and open it (you'll likely need to allow
"install unknown apps" for whichever app opens it), or `adb install
SamsungTvRemote-debug.apk`.

To rebuild it yourself instead of trusting this binary, see the build
instructions in `../README.md`.
