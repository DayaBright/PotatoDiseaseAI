# --- REGLAS PARA TENSORFLOW LITE (Vital para tu IA) ---
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-keep class org.tensorflow.lite.task.vision.** { *; }

# --- REGLAS PARA ROOM (Base de Datos) ---
-keep class com.tesis.potatodiseaseai.data.database.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# --- REGLAS PARA KOTLIN Y COROUTINES ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# --- REGLAS PARA COIL (Carga de imágenes) ---
-keep class io.coilkt.** { *; }
-dontwarn io.coilkt.**