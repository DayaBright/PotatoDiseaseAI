# Potato Disease AI

## Overview
**Potato Disease AI** is an Android application designed to identify and classify potato plant diseases using artificial intelligence and on-device machine learning. This project was developed as part of a thesis ("Tesis") and focuses on evaluating the performance and accuracy of mobile-based image classification.

## Key Features
- **Real-time Classification**: Utilizes a custom TensorFlow Lite model (`potato_classifier.tflite`) to analyze images of potato leaves and detect diseases.
- **On-Device Inference**: Operates completely offline, ensuring fast response times and data privacy.
- **Performance Monitoring**: Built-in tracking for critical metrics such as:
  - Model load time
  - Average inference time
  - Memory (RAM) usage during execution
- **Modern UI**: Built with Jetpack Compose for a smooth, reactive user experience.
- **Camera Integration**: Seamless image capture and preview using the Android CameraX library.
- **Local History**: Uses Room database to store local data.

## Technologies & Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Machine Learning**: TensorFlow Lite Task Vision
- **Camera**: Android CameraX
- **Database**: Room
- **Image Loading**: Coil
- **Asynchronous Operations**: Kotlin Coroutines
- **Testing**: JUnit, Coroutines Test, Room Testing

## Important Components
- **`ImageClassifierHelper`**: The core component responsible for initializing the TFLite model, handling image preprocessing (resizing, cropping, rotation), executing inferences, and calculating performance metrics.
- **`LabelNormalizer` / `ErrorHandler`**: Utility classes to maintain clean output labels and centralize error logging.
- **`CameraPreview`**: Jetpack Compose component handling the live camera feed for immediate disease detection.

## Getting Started

### Prerequisites
- **Android Studio** (Latest stable version recommended)
- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)

### Running the App
1. Clone this repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle files to download all dependencies (TensorFlow Lite, CameraX, Compose, etc.).
4. Connect a physical Android device or start an emulator with a configured camera.
5. Build and run the app (`Shift + F10` or the Run button).

## License
*Add license information here if applicable.*
