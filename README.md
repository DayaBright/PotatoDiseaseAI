# Potato Disease AI

## Descripción General
**Potato Disease AI** es una aplicación para Android diseñada para identificar y clasificar enfermedades en plantas de papa utilizando inteligencia artificial y aprendizaje automático directamente en el dispositivo (On-Device). Este proyecto fue desarrollado como parte de un trabajo de titulación ("Tesis") y se enfoca en evaluar el rendimiento y la precisión de los modelos de clasificación de imágenes en dispositivos móviles.

## Resultados Clave
 
| Métrica | Valor |
|---|---|
| Accuracy | 96.70% |
| Macro F1-score | 96.65% |
| Clases clasificadas | 8 |
| Tamaño del modelo (APK) | 18 MB |
| Latencia de inferencia | 106 ms |
| Arquitectura del modelo | MobileNetV2 + cuantización INT8 (TFLite) |

## Características Principales
- **Clasificación en tiempo real**: modelo TensorFlow Lite (`potato_classifier.tflite`, MobileNetV2 INT8) que analiza imágenes de hojas de papa y detecta 8 categorías, incluyendo una clase de rechazo para imágenes fuera de dominio.
- **Inferencia 100% offline**: sin dependencia de conexión a internet, pensado para zonas rurales con conectividad limitada.
- **Preprocesamiento consistente (letterbox)**: el pipeline de preprocesamiento en Android replica el usado en entrenamiento (letterbox con padding de color medio, RGB 113,123,96), evitando el desajuste de dominio que se detectó entre el recorte central inicial de Android y el preprocesamiento de Python.
- **Monitoreo de rendimiento**: seguimiento de tiempo de carga del modelo, tiempo de inferencia y uso de RAM, usado como instrumento de evaluación de la tesis.
- **Interfaz moderna**: construida con Jetpack Compose.
- **Captura de imágenes**: integración con CameraX.
- **Historial local**: persistencia de resultados con Room.

## Arquitectura de Software
 
La aplicación sigue el patrón **MVVM (Model-View-ViewModel)**:
 
- **View**: composables de Jetpack Compose.
- **ViewModel**: gestiona el estado de la UI y coordina la lógica de inferencia.
- **Model**: `ImageClassifierHelper` (inferencia TFLite) + entidades Room (persistencia local).
## Tecnologías
 
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose, Material Design 3
- **Machine Learning**: TensorFlow Lite Task Vision (MobileNetV2, cuantizado INT8)
- **Cámara**: Android CameraX
- **Base de datos**: Room
- **Carga de imágenes**: Coil
- **Concurrencia**: Kotlin Coroutines
- **Testing**: JUnit, Coroutines Test, Room Testing

## Componentes Importantes
- **`ImageClassifierHelper`**: El componente central responsable de inicializar el modelo TFLite, manejar el preprocesamiento de las imágenes (redimensionamiento, recorte, rotación), ejecutar las inferencias y calcular las métricas de rendimiento.
- **`LabelNormalizer` / `ErrorHandler`**: Clases de utilidad para mantener las etiquetas de los resultados estandarizadas y centralizar el manejo de errores.
- **`CameraPreview`**: Componente de Jetpack Compose que gestiona la transmisión en vivo de la cámara para la detección inmediata de enfermedades.

## Cómo empezar (Getting Started)

### Requisitos Previos
- **Android Studio** (Se recomienda la última versión estable).
- **SDK Mínimo**: API 21 (Android 5.0 Lollipop).
- **SDK Objetivo**: API 34 (Android 14).

### Ejecutar la Aplicación
1. Clona este repositorio en tu máquina local.
2. Abre el proyecto en Android Studio.
3. Sincroniza el proyecto con los archivos de Gradle para descargar todas las dependencias necesarias (TensorFlow Lite, CameraX, Compose, etc.).
4. Conecta un dispositivo Android físico o inicia un emulador que tenga soporte para cámara.
5. Compila y ejecuta la aplicación (Presionando `Shift + F10` o el botón de *Run*).
