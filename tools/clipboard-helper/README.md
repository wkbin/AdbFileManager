# Clipboard helper

`ClipboardHelper.java` is compiled to the small DEX payload embedded at
`src/main/composeResources/files/android/clipboard-helper.dex.b64`.

To rebuild it, compile the Java source against `android.jar`, run Android SDK
Build Tools `d8 --min-api 23`, then Base64-encode the resulting `classes.dex`.
The helper is executed with `app_process` under the ADB shell UID and contains
no third-party code.
