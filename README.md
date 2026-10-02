# TrueNorth Compass 🧭

**TrueNorth** is a modern, privacy-focused, offline Android digital compass app built with Jetpack Compose and Material Design 3. TrueNorth provides high-precision magnetic heading readings using hardware orientation sensors with smooth low-pass filtering, complete offline support, and zero location permission requirements.

---

## 🌟 Key Features

- **🧭 Live Precision Compass**: Real-time magnetic heading calculation (0°–359°) with smooth rotation and low-pass sensor filtering.
- **📍 8-Point Directional Indicator**: Displays 8-point cardinal and intercardinal direction indicators (N, NE, E, SE, S, SW, W, NW).
- **🎨 Interactive Canvas Dial**: Custom Jetpack Compose Canvas rendering featuring degree ticks, cardinal markers, animated needle, and lock bearing indicator.
- **🔒 Heading Lock & Target Bearing**: Lock the current heading to freeze needle positions and visualize target bearings.
- **📊 Sensor Accuracy & Calibration**: Live sensor accuracy status reporting (Unreliable, Low, Medium, High) with interactive figure-8 calibration guidance.
- **📜 Bearing History**: Save important bearings and heading logs locally with custom DataStore persistence.
- **⚙️ Customization Settings**:
  - Light, Dark, and System Default Themes
  - Material You Dynamic Colors
  - Flexible Heading Display Formats (Degrees & Direction, Degrees Only, Direction Only)
  - Customizable Haptic Feedback and Degree Ticks visibility
- **🔒 100% Offline & Private**: No location permissions requested, no internet connectivity required, and no data tracking.

---

## 🛠️ Architecture & Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3](https://m3.material.io/)
- **Architecture**: Model-View-ViewModel (MVVM) pattern with `StateFlow`
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Persistence**: [Jetpack DataStore (Preferences)](https://developer.android.com/topic/libraries/architecture/datastore)
- **Sensor Management**: Android `SensorManager` using `TYPE_ROTATION_VECTOR` with low-pass filtered fallback to `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD`
- **Minimum SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 37

---

## 📁 Project Structure

```
app/src/main/java/com/example/a62compass/
├── data/
│   ├── model/         # Data classes (CompassState, CompassSettings, SavedHeading)
│   └── repository/    # CompassRepository managing DataStore settings & history persistence
├── sensor/
│   └── CompassSensorManager.kt  # Sensor listener & low-pass filtering logic
├── ui/
│   ├── components/    # Reusable UI components (CompassDial, CalibrationDialog)
│   ├── navigation/    # App Navigation Graph & Screen routes
│   ├── screens/       # Compose Screens (Compass, History, Settings, About, Splash)
│   ├── theme/        # Material 3 Color scheme, Typography, & Custom Needles
│   └── viewmodel/     # CompassViewModel, HistoryViewModel, SettingsViewModel
└── util/
    ├── CompassMath.kt     # Math utilities for low-pass filtering & bearing calculations
    └── FeedbackManager.kt # Haptic & sound feedback manager
```

---

## 🚀 Getting Started

### Prerequisites

- [Android Studio Ladybug | 2024.2.1](https://developer.android.com/studio) or newer
- JDK 11 or higher
- Android Device or Emulator running Android 7.0 (API level 24) or higher *(Note: Hardware compass sensors are required on physical devices for live sensor readings)*

### Building the App

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/TrueNorth.git
   cd TrueNorth
   ```

2. **Open in Android Studio**:
   Open Android Studio, select **Open**, and navigate to the cloned project directory.

3. **Build and Run**:
   - Select your target device/emulator.
   - Click the **Run** button (or press `Shift + F10`).

---

## 🧪 Testing

Run unit tests via Gradle:
```bash
./gradlew test
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
