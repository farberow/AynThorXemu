#!/usr/bin/env bash

set -euo pipefail

project_source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
android_dir="${project_source_dir}/android"
key_props="${android_dir}/key.properties"
release_aab="${android_dir}/app/build/outputs/bundle/release/app-release.aab"

if [[ ! -f "${key_props}" ]]; then
  printf 'Creating "%s" with placeholders...\n' "${key_props}"
  cat >"${key_props}" <<'EOF'
storeFile=/path/to/your-release-key.jks
storePassword=REPLACE_ME
keyAlias=REPLACE_ME
keyPassword=REPLACE_ME
EOF
  printf '\nEdit %s with your real values, then rerun this script.\n' "${key_props}"
  exit 1
fi

if grep -Fq 'REPLACE_ME' "${key_props}"; then
  printf '\nPlease replace the placeholders in %s before building.\n' "${key_props}"
  exit 1
fi

if grep -Fq '/path/to/your-release-key.jks' "${key_props}" || \
  grep -Fq 'C:/path/to/your-release-key.jks' "${key_props}"; then
  printf '\nPlease replace the storeFile placeholder in %s before building.\n' "${key_props}"
  exit 1
fi

cd "${android_dir}"
bash ./gradlew bundleRelease

printf '\nBuild complete.\n'
printf 'Release AAB:\n'
printf '  %s\n' "${release_aab}"
