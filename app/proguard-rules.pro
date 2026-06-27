# Gson
-keep class com.google.gson.** { *; }
-keep class com.diary.app.update.ChangelogRelease { *; }
-keep class com.diary.app.update.GitHubRelease { *; }
-keep class com.diary.app.update.GitHubAsset { *; }
-keep class com.diary.app.data.DiaryBackup { *; }
-keep class com.diary.app.data.BackupEntry { *; }
-keep class com.diary.app.data.BackupTag { *; }
-keep class com.diary.app.data.BackupTodo { *; }
-keep class com.diary.app.data.BackupCountDown { *; }
-keep class com.diary.app.data.BackupCapsule { *; }
-keep class com.diary.app.data.BackupTrashEntry { *; }
-keep class com.diary.app.data.BackupHabitRecord { *; }
-keep class com.diary.app.data.BackupNotification { *; }
-keep class com.diary.app.data.BackupChatConversation { *; }
-keep class com.diary.app.data.BackupChatMessage { *; }
-keepattributes *Annotation*,Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Amap SDK
-keep class com.amap.api.**{*;}
-keep class com.amap.api.maps.**{*;}
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.navi.**{*;}
-keep class com.autonavi.**{*;}
-keep class com.loc.**{*;}
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**
-dontwarn com.loc.**
