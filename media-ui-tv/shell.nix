# Android TV build shell — uses nixpkgs androidsdk (unfree; license accepted in config).
{ pkgs ? import <nixpkgs> {
  config.allowUnfree = true;
  config.android_sdk.accept_license = true;
} }:

pkgs.mkShell {
  packages = with pkgs; [
    jdk17
    gradle
    androidsdk
  ];

  ANDROID_SDK_ROOT = "${pkgs.androidsdk}/libexec/android-sdk";
  ANDROID_HOME = "${pkgs.androidsdk}/libexec/android-sdk";

  shellHook = ''
    echo "media-ui-tv — ANDROID_HOME=$ANDROID_HOME"
    echo "scripts/setup-android-sdk.sh && ./gradlew :app:assembleDebug"
  '';
}
