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
- **Clasificación en Tiempo Real**: Utiliza un modelo personalizado de TensorFlow Lite (`potato_classifier.tflite`) para analizar imágenes de hojas de papa y detectar posibles enfermedades.
- **Inferencia en el Dispositivo (Offline)**: Funciona de manera completamente fuera de línea (sin internet), garantizando tiempos de respuesta rápidos y privacidad de los datos.
- **Monitoreo de Rendimiento**: Seguimiento integrado de métricas críticas para la evaluación de la tesis, tales como:
  - Tiempo de carga del modelo.
  - Tiempo promedio de inferencia.
  - Uso de memoria RAM durante la ejecución.
- **Interfaz de Usuario Moderna**: Construida con Jetpack Compose para ofrecer una experiencia de usuario fluida, reactiva y accesible.
- **Integración de Cámara**: Captura de imágenes y vista previa fluida utilizando la librería CameraX de Android.
- **Historial Local**: Uso de la base de datos Room para almacenar resultados y datos de manera local.

## Tecnologías y Arquitectura
- **Lenguaje**: Kotlin
- **Framework de Interfaz (UI)**: Jetpack Compose y Material Design 3
- **Machine Learning**: TensorFlow Lite Task Vision
- **Cámara**: Android CameraX
- **Base de Datos**: Room
- **Carga de Imágenes**: Coil
- **Operaciones Asíncronas**: Kotlin Coroutines
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
