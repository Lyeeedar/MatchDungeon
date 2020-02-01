# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontobfuscate
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
-optimizationpasses 5
-allowaccessmodification

-verbose

-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.jnigen.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn com.badlogic.gdx.tools.**
-dontwarn com.badlogic.gdx.backends.lwjgl.**
-dontwarn org.lwjgl.**
-dontwarn sun.misc.Unsafe
-dontwarn sun.misc.Cleaner
-dontwarn com.sun.xml.internal.fastinfoset.alphabet.BuiltInRestrictedAlphabets
-dontwarn kotlinx.coroutines.**

-keep,allowshrinking class java.beans.** { *; }
-keep,allowshrinking class sun.reflect.** { *; }
-keep,allowshrinking class com.esotericsoftware.kryo.** { *; }
-keep,allowshrinking class com.esotericsoftware.kryo.io.** { *; }
-keep,allowshrinking class sun.nio.ch.** { *; }
-dontwarn sun.nio.ch.**
-keep public class * extends com.google.protobuf.GeneratedMessage { *; }
-keep class de.javakaffee.kryoserializers.** { *; }
-dontwarn de.javakaffee.kryoserializers.**
-keep class com.esotericsoftware.kryo.serializers.** { *; }
-dontwarn com.esotericsoftware.kryo.serializers.**

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
   <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}