# iTantra

> Offline, low-latency multilingual communication prototype for constrained and disconnected environments.

This repository contains the iTantra solution materials and prototype implementation for a Smart India Hackathon problem statement. The project explores an offline communication pipeline that captures speech, converts it into a compact semantic representation, transfers it between devices, and produces translated speech on the receiving device.

## Table of Contents

- [Overview](#overview)
- [Use Case](#use-case)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Repository Structure](#repository-structure)
- [How the System Works](#how-the-system-works)
- [Getting Started](#getting-started)
- [Running the Python Demo](#running-the-python-demo)
- [Building the Android Project](#building-the-android-project)
- [Project Assets](#project-assets)
- [Limitations](#limitations)
- [Future Improvements](#future-improvements)
- [Contributing](#contributing)
- [License](#license)

## Overview

Many communication scenarios require language translation without dependable internet connectivity. iTantra is designed around an **offline-first** approach for environments where bandwidth, infrastructure, or cloud access may be limited.

The prototype demonstrates the following end-to-end flow:

1. Capture speech on a source device.
2. Detect the speech segment using voice activity detection.
3. Convert speech to text with an offline automatic speech recognition model.
4. Encode the meaning into a compact semantic vector.
5. Quantize the vector to reduce the amount of data transferred.
6. Send the compact packet over a local wireless connection.
7. Search a local cache for a known translation.
8. Fall back to local translation and text-to-speech when no cache match is available.

## Use Case

The primary use case is **real-time multilingual communication in offline or intermittently connected environments**, including:

- Field operations where internet access is unavailable.
- Disaster response and emergency coordination.
- Border, defense, and tactical communication scenarios.
- Rural or remote locations with limited connectivity.
- Device-to-device communication where minimizing bandwidth and latency is important.

The repository currently serves as a hackathon prototype and design reference. It should not be treated as a production-ready communications or safety-critical system without additional validation, security review, and field testing.

## Key Features

- **Offline-first design:** Core processing is intended to happen locally on the device.
- **Speech processing pipeline:** Includes conceptual stages for audio capture, voice activity detection, ASR, semantic encoding, and speech playback.
- **Compact data transfer:** Demonstrates INT8 quantization of semantic embeddings before transport.
- **Local translation cache:** Uses a local similarity-search concept to provide a fast path for frequently used phrases.
- **Low-latency fast path:** Cached phrases can avoid the more expensive translation and speech-synthesis path.
- **Android direction:** Includes an Android project for device-side implementation.
- **Python simulation:** Includes a runnable console demo of the proposed pipeline.
- **Supporting materials:** Includes presentation files and architecture diagrams for the SIH submission.

## Technology Stack

The repository uses a multi-language prototype stack:

| Technology | Role |
| --- | --- |
| **Kotlin** | Android application and device-side implementation. |
| **Java** | Android/platform components and supporting implementation. |
| **Python** | Pipeline demonstration, experimentation, model tooling, training, and validation utilities. |
| **HTML** | Repository/project assets and supporting web-oriented material. |
| **Gradle Kotlin DSL** | Android build configuration through `build.gradle.kts` and `settings.gradle.kts`. |
| **NumPy** | Numerical processing and vector quantization in the Python demonstration. |
| **Local storage/cache** | Intended for offline phrase, embedding, and audio lookup. |
| **Bluetooth/BLE transport** | Proposed device-to-device transport layer using a compact packet. |

The language breakdown reported by GitHub is approximately:

- HTML: 43%
- Java: 34.8%
- Kotlin: 13.3%
- Python: 8.9%

Some components are represented as a prototype or simulation rather than a complete production implementation. Check the relevant source files before integrating any individual model, transport, or storage component.

## Repository Structure

```text
.
├── README.md
└── itantra/
    ├── android/                         # Android project
    ├── demo.py                          # End-to-end Python pipeline simulation
    ├── tooling/                         # Training, validation, export, and data utilities
    │   ├── requirements_tooling.txt
    │   ├── train.py
    │   ├── cache/
    │   ├── channel/
    │   ├── data_ops/
    │   ├── export/
    │   ├── models/
    │   └── validate/
    ├── *.pptx                            # SIH presentations and solution material
    └── *.png                             # Architecture and system diagrams
```

## How the System Works

### Transmitter

1. Audio is captured from the microphone.
2. Voice activity detection identifies the beginning and end of an utterance.
3. An offline ASR model converts the audio into text.
4. The text is mapped into a semantic embedding.
5. The embedding is quantized from floating-point values to an INT8 representation.
6. The quantized payload and target-language information are packaged for transport.

### Transport

The prototype models a compact packet sent over a local Bluetooth/BLE connection. A real implementation should define packet framing, versioning, integrity checks, retries, acknowledgements, encryption, and compatibility rules.

### Receiver

1. The packet is received and decoded.
2. The quantized embedding is reconstructed.
3. A local cache is searched for a sufficiently similar phrase.
4. On a cache hit, a pre-rendered translation/audio response can be played quickly.
5. On a cache miss, the system can run the slower local translation and text-to-speech path.

## Getting Started

### Prerequisites

Install the tools required for the part of the project you want to run:

- **Python 3.9+** for the demo and tooling.
- **NumPy** for the Python demo.
- **Android Studio** with a compatible Android SDK for the Android project.
- **Java/JDK** compatible with the Gradle and Android plugin versions configured in the Android project.
- A physical Android device or emulator for Android testing.

Because this repository contains both prototype assets and implementation experiments, dependency versions may vary between components. Prefer the Gradle wrapper and the dependency files included in the repository where available.

### Clone the repository

```bash
git clone https://github.com/gogul098/itantra-repo.git
cd itantra-repo
```

## Running the Python Demo

The demo is a console simulation of the complete iTantra pipeline. It does not require microphone hardware, Bluetooth communication, or live translation models; several stages are intentionally mocked to illustrate the data flow.

```bash
cd itantra
python -m venv .venv
```

Activate the virtual environment:

```bash
# macOS/Linux
source .venv/bin/activate

# Windows PowerShell
.venv\Scripts\Activate.ps1
```

Install the demo dependency and run the simulation:

```bash
python -m pip install --upgrade pip
python -m pip install numpy
python demo.py
```

The output demonstrates speech detection, ASR, embedding creation, INT8 quantization, packet transfer, cache lookup, and audio playback decisions.

## Building the Android Project

The Android project is located in `itantra/android`.

1. Open the `itantra/android` directory in Android Studio.
2. Allow Android Studio to import and synchronize the Gradle project.
3. Install any SDK components requested by the project.
4. Select an emulator or connected Android device.
5. Build and run the application from Android Studio.

On macOS/Linux, the Gradle wrapper can typically be invoked from the Android project directory:

```bash
cd itantra/android
./gradlew tasks
./gradlew assembleDebug
```

On Windows:

```powershell
cd itantra/android
.\gradlew.bat tasks
.\gradlew.bat assembleDebug
```

The exact application behavior depends on the current state of the Android modules and model integrations. Treat the Python demo as the most direct way to inspect the intended pipeline behavior.

## Tooling

The `itantra/tooling` directory contains supporting utilities for experimentation and model workflows, including:

- Data operations.
- Model training.
- Validation.
- Export utilities.
- Channel and cache experiments.

If you are working on the tooling environment, install the dependencies listed in:

```text
itantra/tooling/requirements_tooling.txt
```

A typical setup is:

```bash
cd itantra/tooling
python -m venv .venv
source .venv/bin/activate  # macOS/Linux
python -m pip install -r requirements_tooling.txt
```

Review each script's arguments and expected input files before running training or export commands.

## Project Assets

The repository includes supporting materials for the problem statement and solution presentation, such as:

- SIH presentation decks in PowerPoint format.
- Architecture and data-flow diagrams in PNG format.
- The `geolens_offline.pptx` concept presentation.
- The `itantra.pptx` solution presentation.

These files provide additional context for the product vision, system architecture, and proposed workflow.

## Limitations

This repository is an evolving prototype. Current limitations include:

- The Python demo simulates several components rather than connecting to real audio, translation, or Bluetooth hardware.
- Model accuracy, supported languages, and device performance require benchmarking.
- Production transport requires authentication, encryption, packet validation, retries, and robust connection management.
- Cache similarity thresholds need to be tuned using representative data.
- Battery usage, memory usage, thermal behavior, and latency need to be measured on target devices.
- Safety-critical or tactical deployments require extensive testing and formal review.

## Future Improvements

Potential next steps include:

- Integrate real offline ASR, translation, and text-to-speech models.
- Complete Bluetooth/BLE transport and device pairing flows.
- Add encrypted communication and packet integrity protection.
- Build a persistent local embedding/audio cache.
- Add language-pack management and model versioning.
- Improve Android background processing and power efficiency.
- Add automated tests for quantization, packet parsing, cache matching, and translation quality.
- Measure end-to-end latency and accuracy across supported devices and languages.
- Add CI for Python tooling and Android builds.
- Document supported language pairs and minimum device requirements.

## Contributing

Contributions are welcome. Before opening a pull request:

1. Create a feature branch.
2. Keep changes focused and document new behavior.
3. Update this README when setup or usage changes.
4. Test Python changes locally.
5. Build the Android project when Android files are modified.
6. Do not commit private data, model credentials, generated secrets, or large temporary build artifacts.

## License

No license file is currently included in the repository. Until a license is added, all rights are reserved by the repository owner. If you intend to reuse or distribute this project, contact the maintainers and request clarification.

## Acknowledgements

This project was created as part of the Smart India Hackathon preparation and is maintained as a collaborative exploration of offline multilingual communication.
