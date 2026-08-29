{ pkgs ? import <nixpkgs> {
    config = {
      allowUnfree = true;
      android_sdk.accept_license = true;
    };
  }
}:

let
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "35" "36" ];
    buildToolsVersions = [ "35.0.0" ];
    platformToolsVersion = "35.0.2";
    includeEmulator = false;
    includeSystemImages = false;
    includeNDK = false;
  };
  androidSdk = androidComposition.androidsdk;
in
pkgs.mkShell {
  packages = [
    pkgs.jdk17
    pkgs.gradle
    androidSdk
  ];

  ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
  ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
  JAVA_HOME = pkgs.jdk17.home;

  shellHook = ''
    export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/35.0.0/aapt2"
  '';
}
