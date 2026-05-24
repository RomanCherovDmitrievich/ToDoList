#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="$PROJECT_ROOT/mobile/ios"
BUILD_DIR="$IOS_DIR/build"
SIM_DERIVED_DATA="$BUILD_DIR/ios-sim"
DEVICE_DERIVED_DATA="$BUILD_DIR/ios-device"
DEVICE_APP="$DEVICE_DERIVED_DATA/Build/Products/Debug-iphoneos/ToDoListMobile.app"
IPA_DIR="$BUILD_DIR/ipa-package"
IPA_PATH="$BUILD_DIR/ToDoListMobile-unsigned.ipa"

cd "$IOS_DIR"

xcodegen generate
xcodebuild \
  -project ToDoListMobile.xcodeproj \
  -scheme ToDoListMobile \
  -sdk iphonesimulator \
  -destination "generic/platform=iOS Simulator" \
  -configuration Debug \
  -derivedDataPath "$SIM_DERIVED_DATA" \
  CODE_SIGNING_ALLOWED=NO \
  build

xcodebuild \
  -project ToDoListMobile.xcodeproj \
  -scheme ToDoListMobile \
  -sdk iphoneos \
  -destination "generic/platform=iOS" \
  -configuration Debug \
  -derivedDataPath "$DEVICE_DERIVED_DATA" \
  CODE_SIGNING_ALLOWED=NO \
  build

mkdir -p "$IPA_DIR/Payload"
rm -rf "$IPA_DIR/Payload/ToDoListMobile.app"
cp -R "$DEVICE_APP" "$IPA_DIR/Payload/"
rm -f "$IPA_PATH"
ditto -c -k --sequesterRsrc --keepParent "$IPA_DIR/Payload" "$IPA_PATH"

printf 'iOS simulator app: %s\n' "$SIM_DERIVED_DATA/Build/Products/Debug-iphonesimulator/ToDoListMobile.app"
printf 'iOS device app: %s\n' "$DEVICE_APP"
printf 'iOS unsigned ipa: %s\n' "$IPA_PATH"
