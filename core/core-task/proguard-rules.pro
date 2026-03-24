# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all task related classes
-keep class com.cn.core.task.** { *; }

# Keep coroutine related classes
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
