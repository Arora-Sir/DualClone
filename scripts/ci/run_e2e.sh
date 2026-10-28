#!/bin/sh
# Runs on the CI emulator. Creates a managed profile named Work, starts it, then runs the instrumented e2e tests.
# The tests swap the Shizuku gateway for UiAutomation shell access, so Shizuku is not installed on the emulator.
set -eu

chmod +x gradlew
out="$(adb shell pm create-user --profileOf 0 --managed Work)"
echo "$out"
user_id="$(echo "$out" | sed -n 's/.*user id \([0-9][0-9]*\).*/\1/p')"
if [ -z "$user_id" ]; then
  echo "Could not create the managed profile" >&2
  exit 1
fi
adb shell am start-user "$user_id"
adb shell pm list users

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.managedUserId="$user_id" \
  --stacktrace
