# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep TensorFlow Lite classes - more specific rules
-keep class org.tensorflow.lite.Interpreter { *; }
-keep class org.tensorflow.lite.InterpreterApi { *; }
-keep class org.tensorflow.lite.Tensor { *; }
-keep class org.tensorflow.lite.DataType { *; }
-dontwarn org.tensorflow.lite.**

# Keep TensorFlow Lite Task API classes - more specific rules
-keep class org.tensorflow.lite.task.vision.detector.ObjectDetector { *; }
-keep class org.tensorflow.lite.task.vision.detector.Detection { *; }
-keep class org.tensorflow.lite.task.vision.detector.Detection$* { *; }
-dontwarn org.tensorflow.lite.task.**

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# Keep CameraX classes - more specific rules
-keep class androidx.camera.core.ImageProxy { *; }
-keep class androidx.camera.core.ImageAnalysis { *; }
-keep class androidx.camera.lifecycle.ProcessCameraProvider { *; }
-dontwarn androidx.camera.**

# Keep Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Keep Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Keep data classes used with Room
-keepclassmembers class com.smartfind.app.data.local.entity.** {
    *;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# R8 full mode optimization
-allowaccessmodification
-repackageclasses
