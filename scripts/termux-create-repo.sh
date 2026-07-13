#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_NAME="wemwem-morphosis"
OWNER="0mangoscam"

pkg update -y
pkg install -y git gh unzip

echo
if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub needs authentication. Follow the browser/device-code flow."
  gh auth login --hostname github.com --git-protocol https --web
fi

if [ ! -d .git ]; then
  git init -b main
fi

git config user.name "Brian Novillo"
git config user.email "cryptonovillox@gmail.com"

git add .
git commit -m "Create WEMWEM Morphosis prototype" || true

if ! gh repo view "$OWNER/$REPO_NAME" >/dev/null 2>&1; then
  gh repo create "$OWNER/$REPO_NAME" \
    --public \
    --source=. \
    --remote=origin \
    --push \
    --description "Audio-reactive reaction-diffusion instrument for Android"
else
  git remote remove origin 2>/dev/null || true
  git remote add origin "https://github.com/$OWNER/$REPO_NAME.git"
  git push -u origin main
fi

echo
echo "Repository ready: https://github.com/$OWNER/$REPO_NAME"
