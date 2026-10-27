# libGDX reaches its backend, natives and a few helpers reflectively; keep them whole.
-keep class com.badlogic.gdx.backends.android.** { *; }
-keep class com.badlogic.gdx.graphics.g2d.freetype.** { *; }
-keep class com.badlogic.gdx.utils.** { *; }
-keep class com.badlogic.gdx.physics.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
-dontwarn com.badlogic.gdx.**
-dontwarn org.lwjgl.**
-dontwarn javax.**
# The launcher is named in the manifest.
-keep class com.iagothz.mergetd.android.AndroidLauncher { *; }

# libGDX creates pooled objects (GlyphLayout, GlyphRun, ...) by reflection and needs their no-arg constructors.
-keepclassmembers class * implements com.badlogic.gdx.utils.Pool$Poolable { <init>(); }
-keepclassmembers class com.badlogic.gdx.** { public <init>(); }
