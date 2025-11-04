# MANTER CLASSES DO FIREBASE
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# MANTER CLASSES USADAS PELO RETROFIT E GSON
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# MANTER CLASSES UTILIZADAS PELO OKHTTP3
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# MANTER VIEWMODEL E LIFECYCLE
-keepclassmembers class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# COMPATIBILIDADE COM GLIDE
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public class * implements com.bumptech.glide.model.ModelLoader
-dontwarn com.bumptech.glide.**

# COMPATIBILIDADE COM COIL
-keep class coil.** { *; }
-dontwarn coil.**

# COMPATIBILIDADE COM PICASSO
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

# MANTER ANIMAÇÕES E FUNÇÕES DO JETPACK COMPOSE
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# AJUSTES PARA WEBSOCKET (caso use classes específicas)
-dontwarn org.java_websocket.**

# PRESERVAR LINHAS EM LOGS (opcional)
-keepattributes SourceFile,LineNumberTable
