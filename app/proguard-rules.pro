# DualClone ProGuard and R8 rules.
# NOTE: Room and Hilt ship their own consumer rules, so only Shizuku needs keeps here.
# NOTE: Shizuku.newProcess is private static and is called by reflection in ShizukuProcessHelper.
-keep class rikka.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.Shizuku {
    private static *** newProcess(...);
}
-dontwarn rikka.shizuku.**
-keepattributes *Annotation*
