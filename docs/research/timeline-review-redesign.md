# Timeline / Review / Favorites / Trash Redesign Proposal

> Scope: TimelineScreen, DiaryReviewScreen, FavoritesScreen, TrashScreen + image management
> Base: `experiment/v2-redesign` branch, Jetpack Compose + Room

---

## 1. Timeline Rework

### Current Problems

- **All entries loaded into memory** (`getAllPreviews()` returns entire table). With 1000+ entries this causes jank on initial load and unnecessary memory pressure.
- **Month-based expand/collapse** is the only navigation. No way to jump to a specific month quickly.
- **No visual variety** -- every day group looks identical. No sense of "this month was special."
- **Timeline axis** (vertical line + dot) is decorative but takes 36dp horizontal space on every row, adding visual noise.

### Proposed Solution

**A. Paginated loading with `PagingSource`**

Replace `getAllPreviews()` with Room's `PagingSource`:

```kotlin
// DiaryDao.kt
@Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries ORDER BY createdAt DESC")
fun getPreviewsPaging(): PagingSource<Int, DiaryPreview>

// TimelineViewModel.kt
val entries: Flow<PagingData<DiaryPreview>> = Pager(
    config = PagingConfig(pageSize = 30, enablePlaceholders = false)
) { dao.getPreviewsPaging() }.flow.cachedIn(viewModelScope)
```

UI side uses `LazyPagingItems` instead of `itemsIndexed`. Month grouping logic moves to a `insertSeparators` transform.

**B. Quick-jump month sidebar**

Add a vertical month indicator on the right edge (like iOS Photos). Tapping scrolls to that month. Implementation: a fixed `Column` overlay with `YearMonth` labels, each calling `listState.animateScrollToItem(index)`.

**C. Month summary card**

At the top of each month group, insert a summary card showing:
- Entry count, dominant mood (most frequent `moodLevel`), most used tags
- A mini mood-bar: 6 colored segments proportional to mood distribution

```kotlin
data class MonthSummary(
    val month: YearMonth,
    val entryCount: Int,
    val dominantMood: Int?,
    val moodDistribution: Map<Int, Int>, // moodLevel -> count
    val topTags: List<String>
)
```

This is computed in the ViewModel from the month's entries -- no new DAO queries needed.

**D. Visual refinement: remove timeline axis**

The vertical line + dot pattern consumes space without conveying information. Replace with a subtle left-border accent on the date header row (3dp colored bar, mood-colored). Day cards become full-width with rounded corners and a light shadow -- same as the current `DayGroupCard` but without the 36dp left column.

### Effort

| Task | Estimate |
|------|----------|
| PagingSource migration | 1 day |
| Month summary card | 0.5 day |
| Quick-jump sidebar | 0.5 day |
| Remove timeline axis, refine layout | 0.5 day |
| **Total** | **2.5 days** |

---

## 2. "On This Day" Enhancement

### Current Problems

- "On This Day" is buried inside `DiaryReviewScreen` alongside "one week ago" / "one month ago" entries. Users must actively navigate to see it.
- No notification or widget -- the feature only works if you open the app and visit the review screen.
- No sense of ritual or emotional impact. It's just another card in a list.

### Proposed Solution

**A. Daily notification**

Use `WorkManager` to schedule a daily check at a user-configurable time (default 8:00 AM). If `getOnThisDayPreviews()` returns non-empty results, post a notification:

```kotlin
class OnThisDayWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = (applicationContext as DiaryApplication).database.diaryDao()
        val today = LocalDate.now()
        val entries = dao.getEntriesByMonthDay(today.monthValue, today.dayOfMonth)
        val pastEntries = entries.filter {
            val year = Instant.ofEpochMilli(it.createdAt)
                .atZone(ZoneId.systemDefault()).year
            year < today.year
        }
        if (pastEntries.isNotEmpty()) {
            showOnThisDayNotification(pastEntries.size, pastEntries.maxOf { it.createdAt })
        }
        return Result.success()
    }
}
```

**B. App Widget (Glance)**

A Glance widget showing the most recent "On This Day" entry preview. Tapping opens the detail screen. Falls back to "No memories for today" if empty.

**C. Memory Lane slideshow mode**

A full-screen, swipeable card stack showing all "On This Day" entries across years, ordered oldest-first. Each card shows the year, title, content preview, mood. Swipe up to dismiss. Think of it as a vertical pager:

```kotlin
@Composable
fun MemoryLaneScreen(entries: List<DiaryEntry>, onEntryClick: (Long) -> Unit) {
    val pagerState = rememberPagerState { entries.size }
    VerticalPager(state = pagerState) { page ->
        MemoryCard(entry = entries[page], yearDiff = currentYear - entryYear)
    }
}
```

**D. Year-in-review auto-summary**

Every December 31 (or on demand), generate a summary card:
- Total entries this year, longest writing streak, dominant mood, most-used tags
- A mood heatmap (12 months x mood levels, colored by frequency)

This requires a new DAO query:

```kotlin
@Query("SELECT moodLevel, COUNT(*) as count FROM diary_entries WHERE createdAt >= :yearStart AND createdAt < :yearEnd GROUP BY moodLevel")
suspend fun getMoodDistributionForYear(yearStart: Long, yearEnd: Long): List<MoodCount>
```

### Effort

| Task | Estimate |
|------|----------|
| WorkManager daily notification | 0.5 day |
| Glance widget | 1 day |
| Memory Lane slideshow | 1 day |
| Year-in-review summary | 1.5 day |
| **Total** | **4 days** |

---

## 3. Favorites Enhancement

### Current Problems

- Flat list with tag filter chips. No way to organize favorites beyond tags.
- No sorting options (always `createdAt DESC`).
- No export or sharing capability.
- Tag filter only shows tags that exist on favorited entries' cross-refs -- but `allTags` fetches ALL tags, so empty-tag chips appear.

### Proposed Solution

**A. Smart collections (auto-group)**

Add preset collection tabs alongside the tag filter:

| Collection | Logic |
|-----------|-------|
| Recent | Last 30 days |
| Happy | moodLevel >= 5 |
| Reflective | moodLevel <= 2 |
| With Photos | entries that have images |

Implementation: filter `entries` list in ViewModel using additional predicates. No new DAO queries.

```kotlin
enum class SmartCollection(val label: String) {
    ALL("全部"), RECENT("近期"), HAPPY("开心"), REFLECTIVE("沉思"), WITH_PHOTOS("有图")
}
```

**B. Pin to top**

Add `pinnedAt: Long?` field to `diary_entries`. Favorited + pinned entries sort first. New DAO:

```kotlin
@Query("SELECT id, title, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries WHERE isFavorite = 1 ORDER BY pinnedAt DESC, createdAt DESC")
fun getFavoritePreviewsSorted(): Flow<List<DiaryPreview>>
```

Migration: `ALTER TABLE diary_entries ADD COLUMN pinnedAt INTEGER DEFAULT NULL`

UI: long-press a favorite card to toggle pin. Pinned cards show a small pin icon.

**C. Share / export collection**

A "Share" button in the top bar that exports the currently filtered favorites as a formatted text file (Markdown) and opens the Android share sheet:

```kotlin
fun exportFavoritesAsMarkdown(entries: List<DiaryPreview>): String {
    return buildString {
        appendLine("# My Favorite Entries")
        entries.forEach { entry ->
            appendLine("\n## ${entry.title}")
            appendLine("*${formatDate(entry.createdAt)}*\n")
            appendLine(entry.plainText)
        }
    }
}
```

### Effort

| Task | Estimate |
|------|----------|
| Smart collections | 0.5 day |
| Pin to top (migration + UI) | 1 day |
| Share/export | 0.5 day |
| **Total** | **2 days** |

---

## 4. Trash Enhancement

### Current Problems

- **No automatic cleanup.** The "30 days" text is display-only -- nothing actually deletes expired entries. Users must manually "empty trash."
- **No preview** before restoring or deleting. The card shows a 3-line text snippet but no way to see the full entry.
- **No storage tracking.** Users don't know how much space trash entries occupy.

### Proposed Solution

**A. WorkManager auto-cleanup**

Schedule a periodic worker (once daily) that deletes entries older than 30 days:

```kotlin
class TrashCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = (applicationContext as DiaryApplication).database.diaryDao()
        val cutoff = System.currentTimeMillis() - 30.days.inWholeMilliseconds
        dao.deleteTrashEntriesBefore(cutoff)  // already exists in DAO
        return Result.success()
    }
}

// Schedule in DiaryApplication.onCreate()
val cleanupRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(1, TimeUnit.DAYS)
    .setInitialDelay(calculateDelayToMidnight(), TimeUnit.MILLISECONDS)
    .build()
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "trash_cleanup", ExistingPeriodicWorkPolicy.KEEP, cleanupRequest
)
```

**B. Recovery preview**

Tap a trash card to open a read-only detail view (reuse existing `DiaryDetailScreen` in a "preview" mode). This lets users verify content before restoring.

**C. Permanent delete confirmation**

The current `AlertDialog` is fine for single deletes. Add a "batch select" mode: long-press to enter selection, then bulk restore or delete. The "empty trash" dialog should show the total size:

```kotlin
// In TrashViewModel
suspend fun getTrashSizeEstimate(): String {
    val entries = entries.value
    val totalChars = entries.sumOf { it.content.length }
    return formatSize(totalChars * 2L) // rough estimate: 2 bytes per char
}
```

**D. Storage space tracking**

Show a small info bar at the top of TrashScreen: "5 entries, ~12KB, auto-delete in 3-28 days". Computed from the `entries` list.

### Effort

| Task | Estimate |
|------|----------|
| WorkManager auto-cleanup | 0.5 day |
| Recovery preview | 0.5 day |
| Batch select mode | 1 day |
| Storage info bar | 0.25 day |
| **Total** | **2.25 days** |

---

## 5. Image Management

### Current Problems

- `DiaryImage` stores only `localPath`. No dimensions, no thumbnail path, no file size.
- No compression -- images saved at original resolution. A 12MP photo can be 5MB+.
- No gallery view. Images are only visible inside individual diary entries.
- No image cleanup when entries are permanently deleted (the `ForeignKey.CASCADE` handles DB rows, but files on disk remain).

### Proposed Solution

**A. Image compression pipeline**

Compress on insert. Target: max 1920px on longest edge, JPEG quality 85, strip EXIF GPS (privacy). Use `BitmapFactory` + `Bitmap.compress`:

```kotlin
object ImageProcessor {
    suspend fun processImage(context: Context, uri: Uri): ProcessedImage = withContext(Dispatchers.IO) {
        val bitmap = decodeSampledBitmap(uri, maxSize = 1920)
        val compressedPath = saveCompressed(context, bitmap, quality = 85)
        val thumbnailPath = saveThumbnail(context, bitmap, maxSize = 300)
        ProcessedImage(compressedPath, thumbnailPath, bitmap.width, bitmap.height)
    }
}
```

**B. Schema update**

```kotlin
@Entity(tableName = "diary_images")
data class DiaryImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val localPath: String,
    val thumbnailPath: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0,
    val sortOrder: Int = 0
)
```

Migration: `ALTER TABLE diary_images ADD COLUMN thumbnailPath TEXT`, etc.

**C. Global image gallery**

A new screen accessible from the timeline header or bottom nav, showing all images across all entries in a grid. Uses thumbnail paths for fast loading. Tapping opens the full image and navigates to the parent entry.

```kotlin
@Query("""
    SELECT di.*, de.createdAt as entryCreatedAt
    FROM diary_images di
    INNER JOIN diary_entries de ON di.entryId = de.id
    ORDER BY de.createdAt DESC
""")
fun getAllImagesWithEntryDate(): Flow<List<ImageWithDate>>
```

**D. File cleanup**

When permanently deleting an entry (from trash), also delete its image files:

```kotlin
suspend fun cleanupImageFiles(context: Context, entryId: Long) {
    val images = dao.getImagesForEntry(entryId)
    images.forEach { image ->
        File(image.localPath).delete()
        image.thumbnailPath?.let { File(it).delete() }
    }
}
```

### Effort

| Task | Estimate |
|------|----------|
| Compression pipeline | 1 day |
| Schema migration + DAO updates | 0.5 day |
| Thumbnail generation | 0.5 day |
| Global gallery screen | 1.5 days |
| File cleanup integration | 0.5 day |
| **Total** | **4 days** |

---

## Summary

| Area | Effort | Priority |
|------|--------|----------|
| Timeline rework (paging + summary) | 2.5 days | High |
| "On This Day" (notification + widget) | 4 days | Medium |
| Favorites (smart collections + pin) | 2 days | Low |
| Trash (auto-cleanup + preview) | 2.25 days | High |
| Image management (compression + gallery) | 4 days | High |
| **Total** | **~15 days** | |

Recommended order: Trash auto-cleanup -> Image compression -> Timeline paging -> On This Day -> Favorites polish.
