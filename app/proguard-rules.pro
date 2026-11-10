# Room, WorkManager and DataStore ship their own consumer rules.
# Keep enum names stable: Room and settings store them as text (TxType, PeriodMode, …).
-keepclassmembers enum com.budgy.app.** { *; }
