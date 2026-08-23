# GeckoView：native/JNI 回调与内部反射依赖类名，整体保留
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-dontwarn org.mozilla.**

# baseokhttpx：请求回调/参数解析存在反射路径
-keep class com.kongzue.baseokhttp.** { *; }
-dontwarn com.kongzue.baseokhttp.**

# 传递依赖中的 JVM-only 引用（snakeyaml 的 java.beans introspection）
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn org.slf4j.**

# 通过 Intent 传递的 Serializable 数据类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
}
-keep class dev1503.browseevo.download.DownloadRecord { *; }

# 崩溃堆栈可读性
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
