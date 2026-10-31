# راهنمای اصلاحات نسخه v1.7.0R برای گیت‌هاب (Fix Release v1.7.0R)

این راهنما شامل اصلاح دقیق ۴ فایلی است که باعث توقف بیلد گیت‌هاب در مرحله ۹ شده بودند.

---

### فایل ۱: `app/src/main/java/com/example/personallifeos/ui/screens/TopicManagementSheets.kt`
- **محل تغییر:** خط ۱۵۲۵
- **کد قبلی:**
```kotlin
(if (mission.reminderDateTime.isNotBlank()) " • یادآوری: ${mission.reminderDateTime}" else ""),
```
- **کد اصلاح‌شده:**
```kotlin
(if (!mission.reminderDateTime.isNullOrBlank()) " • یادآوری: ${mission.reminderDateTime}" else ""),
```

---

### فایل ۲: `app/src/main/java/com/example/personallifeos/data/model/Models.kt`
1. در `enum class TaskCategory` مقدار `GROWTH` اضافه شد:
```kotlin
enum class TaskCategory(val titleFa: String) {
    WORK("کاری"),
    PERSONAL("شخصی"),
    FINANCE("مالی"),
    HEALTH("سلامت"),
    STUDY("مطالعه"),
    GROWTH("رشد و یادگیری")
}
```
2. در `data class TaskItem` مقدار پیش‌فرض به `createdAt` اضافه شد:
```kotlin
val createdAt: String = "۱۴۰۵/۰۸/۰۱",
```

---

### فایل ۳: `app/src/main/java/com/example/personallifeos/data/local/Entities.kt`
در کلاس `TaskEntity`:
1. اضافه کردن تابع `toModel`:
```kotlin
fun toModel(): TaskItem = toDomain()
```
2. در `companion object` اضافه کردن تابع `fromModel`:
```kotlin
companion object {
    fun fromModel(task: TaskItem): TaskEntity = fromDomain(task)
    fun fromDomain(task: TaskItem): TaskEntity {
        ...
```

---

### فایل ۴: `app/src/test/java/com/example/personallifeos/BankSmsEngineAuditTest.kt`
در متد تست تاپیک‌ها خط ۲۷۰:
- **کد قبلی:**
```kotlin
assertEquals(2, stats.urgentOrMajorTasks)
```
- **کد اصلاح‌شده:**
```kotlin
assertEquals(2, stats.urgentOrMajorTasks.size)
```
