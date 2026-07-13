# WEMWEM // MORPHOSIS

An Android audiovisual instrument where sound grows, mutates and erodes a reaction-diffusion organism in real time.

The screen begins as empty black matter. Microphone energy creates colonies. Bass expands mass, mids alter internal movement, treble generates fine seeds, and silence slowly dissolves the organism.

## Prototype features

- Kotlin + Jetpack Compose interface
- OpenGL ES 3.0 ping-pong reaction-diffusion simulation
- Live `AudioRecord` microphone input
- Built-in radix-2 FFT with no DSP dependency
- Six live parameters: Matter, Mutation, Decay, Depth, Memory and Sensitivity
- Touch-driven perturbation
- Automatic debug APK through GitHub Actions

## Create the repository from Termux

1. Download the project ZIP into Android's **Downloads** folder.
2. In Termux, run:

```bash
termux-setup-storage
pkg update -y && pkg install -y unzip
cd ~/storage/downloads
unzip wemwem-morphosis.zip
cd wemwem-morphosis
chmod +x scripts/termux-create-repo.sh
./scripts/termux-create-repo.sh
```

The script installs `git`, `gh` and `unzip`, authenticates GitHub if needed, creates `Mangoscam/wemwem-morphosis`, commits the project and pushes `main`.

## Manual Termux route

```bash
pkg update -y
pkg install -y git gh unzip
gh auth login --hostname github.com --git-protocol https --web

git init -b main
git config user.name "Brian Novillo"
git config user.email "cryptonovillox@gmail.com"
git add .
git commit -m "Create WEMWEM Morphosis prototype"
gh repo create Mangoscam/wemwem-morphosis --public --source=. --remote=origin --push
```

## Get the APK

After the first push:

1. Open the repository on GitHub.
2. Enter **Actions**.
3. Open the latest **Android APK** run.
4. Download the `wemwem-morphosis-debug` artifact.
5. Extract and install `app-debug.apk` on Android.

## Local build

The project uses:

- Android Gradle Plugin 9.2.1
- Gradle 9.4.1
- JDK 17
- compileSdk 37
- Compose BOM 2026.06.00

```bash
gradle assembleDebug
```

A full Android SDK and Gradle 9.4.1 are required for local builds. Editing and Git operations work normally in Termux, while GitHub Actions handles the heavy build by default. The repository intentionally avoids a committed Gradle wrapper binary so the starter ZIP stays transparent and text-first.

## Artistic rule

No sound, no birth. Touch can disturb existing matter, but the organism is fundamentally fed by audio.
