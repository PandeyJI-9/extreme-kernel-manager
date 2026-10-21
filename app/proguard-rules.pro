# Extreme+ Kernel Manager Proguard / R8 Shrinker Rules
# Safe shrinking: remove unused classes without bytecode obfuscation or aggressive re-writing
-dontobfuscate
-dontoptimize

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Extreme Features & Handshake
-keep class id.nkz.nokontzzzmanager.extreme.** { *; }

# Root & Shell Execution
-keep class com.topjohnwu.superuser.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,Descriptor
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room & SQLite
-keep class * extends androidx.room.RoomDatabase
