# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepparameternames

-keep public class com.htc.wallet.skrsdk.ZionSkrSdkManager {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.ZionSkrSdkManager$* {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.util.Callback {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.adapter.** {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.tools.** {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.keyserver.** {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.restore.reconnect.ReconnectNameUtils {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.applink.BaseAppLinkReceiverActivity {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.backup.CheckSKRFragment {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.backup.CheckSKRUtil {
    public <methods>;
    public <fields>;
}

-keep public class com.htc.wallet.skrsdk.messaging.message.Message {
    public <methods>;
    public <fields>;
}

# branch.io
# https://github.com/BranchMetrics/android-branch-deep-linking-attribution
-dontwarn com.google.firebase.appindexing.**
-dontwarn com.android.installreferrer.api.**

# GSON
# https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

##---------------End: proguard configuration for Gson  ----------

# Prevent proguard the pushy library
-dontwarn me.pushy.**
-keep class me.pushy.** { *; }