# Juggling Progress Tracker - Architecture Plan
*Created: 2025-08-14*

## 1. Project Overview

This document outlines the architecture plan for transforming the current Android project (CollisionTest) into a Juggling Progress Tracker app. The app will allow users to track their juggling patterns, record test sessions, view progress over time, and manage their juggling practice.

### Current Project Structure

The current project is a basic Android app with:
- Navigation drawer with three fragments (Home, Gallery, Slideshow)
- MVVM architecture with ViewModels for each fragment
- Navigation Component for navigation
- Material Design components
- ViewBinding for view binding

### Target Application Features

The Juggling Progress Tracker app will include:
- 6 screens: Main (patterns list), Pattern Detail, Add/Edit Pattern, Progress Chart, Test History, Settings
- Room database with Pattern, TestSession, Tag entities and cross-reference tables
- Video handling with recording, importing, trimming, exporting
- Progress charts with MPAndroidChart
- Material Design components throughout

## 2. Package Structure Refactoring Plan

### Current Package Structure
```
com.example.collisiontest
├── MainActivity.kt
└── ui
    ├── gallery
    │   ├── GalleryFragment.kt
    │   └── GalleryViewModel.kt
    ├── home
    │   ├── HomeFragment.kt
    │   └── HomeViewModel.kt
    └── slideshow
        ├── SlideshowFragment.kt
        └── SlideshowViewModel.kt
```

### New Package Structure
```
com.example.jugglingtracker
├── MainActivity.kt
├── JugglingTrackerApplication.kt
├── data
│   ├── entities
│   │   ├── Pattern.kt
│   │   ├── TestSession.kt
│   │   ├── Tag.kt
│   │   ├── PatternWithTags.kt
│   │   └── PatternWithTestSessions.kt
│   ├── dao
│   │   ├── PatternDao.kt
│   │   ├── TestSessionDao.kt
│   │   └── TagDao.kt
│   ├── database
│   │   └── JugglingDatabase.kt
│   └── repository
│       ├── PatternRepository.kt
│       ├── TestSessionRepository.kt
│       └── TagRepository.kt
├── ui
│   ├── patterns
│   │   ├── list
│   │   │   ├── PatternListFragment.kt
│   │   │   └── PatternListViewModel.kt
│   │   ├── detail
│   │   │   ├── PatternDetailFragment.kt
│   │   │   └── PatternDetailViewModel.kt
│   │   └── edit
│   │       ├── PatternEditFragment.kt
│   │       └── PatternEditViewModel.kt
│   ├── progress
│   │   ├── ProgressChartFragment.kt
│   │   └── ProgressChartViewModel.kt
│   ├── history
│   │   ├── TestHistoryFragment.kt
│   │   └── TestHistoryViewModel.kt
│   └── settings
│       ├── SettingsFragment.kt
│       └── SettingsViewModel.kt
├── utils
│   ├── DateTimeUtils.kt
│   ├── VideoUtils.kt
│   └── ChartUtils.kt
└── di
    └── AppModule.kt
```

### Refactoring Steps

1. Rename package from `com.example.collisiontest` to `com.example.jugglingtracker`
2. Update AndroidManifest.xml, build.gradle.kts, and other configuration files
3. Create the new package structure
4. Migrate existing components to the new structure
5. Add new components as needed

## 3. MVVM Architecture Implementation

### Architecture Components

The app will follow the MVVM (Model-View-ViewModel) architecture pattern with the following components:

#### View Layer
- Activities and Fragments
- Observes LiveData/Flow from ViewModels
- Handles UI interactions
- No business logic

#### ViewModel Layer
- Exposes data to the View layer using LiveData/Flow
- Handles UI-related logic
- Communicates with the Repository layer
- Survives configuration changes

#### Repository Layer
- Single source of truth for data
- Abstracts data sources (local database, network)
- Provides clean API to the ViewModel layer
- Handles data operations and business logic

#### Data Layer
- Room Database for local persistence
- Entities, DAOs, and Database classes
- Data models and relationships

### Data Flow

```
UI (Fragment) ↔ ViewModel ↔ Repository ↔ Local Database (Room)
                                       ↕
                                    File System (Videos)
```

### Coroutines and Flow Usage Strategy

- **Coroutines**: Used for asynchronous operations
  - ViewModelScope for ViewModel operations
  - LifecycleScope for UI-related operations
  - IO Dispatcher for database and file operations
  - Main Dispatcher for UI updates

- **Flow**: Used for reactive data streams
  - Room DAOs return Flow objects
  - Repositories transform and expose Flow objects
  - ViewModels collect Flow objects and expose as LiveData or StateFlow
  - UI observes LiveData/StateFlow for updates

### Example Implementation

```kotlin
// DAO
@Dao
interface PatternDao {
    @Query("SELECT * FROM patterns")
    fun getAllPatterns(): Flow<List<Pattern>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: Pattern): Long
}

// Repository
class PatternRepository(private val patternDao: PatternDao) {
    fun getAllPatterns(): Flow<List<Pattern>> = patternDao.getAllPatterns()
    
    suspend fun savePattern(pattern: Pattern): Long {
        return patternDao.insertPattern(pattern)
    }
}

// ViewModel
class PatternListViewModel(private val repository: PatternRepository) : ViewModel() {
    val patterns: LiveData<List<Pattern>> = repository.getAllPatterns().asLiveData()
    
    fun savePattern(pattern: Pattern) {
        viewModelScope.launch {
            repository.savePattern(pattern)
        }
    }
}

// Fragment
class PatternListFragment : Fragment() {
    private val viewModel: PatternListViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.patterns.observe(viewLifecycleOwner) { patterns ->
            // Update UI with patterns
        }
    }
}
```

## 4. Room Database Design

### Entity Relationships

```
Pattern (1) ←→ (N) TestSession
Pattern (N) ←→ (N) Tag (via junction table)
```

### Entities

#### Pattern
```kotlin
@Entity(tableName = "patterns")
data class Pattern(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val difficulty: Int, // 1-10 scale
    val videoPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### TestSession
```kotlin
@Entity(
    tableName = "test_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patternId")]
)
data class TestSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patternId: Long,
    val date: Long,
    val duration: Long, // in milliseconds
    val successCount: Int,
    val attemptCount: Int,
    val notes: String?,
    val videoPath: String?
)
```

#### Tag
```kotlin
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int
)
```

#### PatternTagCrossRef (Junction Table)
```kotlin
@Entity(
    tableName = "pattern_tag_cross_ref",
    primaryKeys = ["patternId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("patternId"),
        Index("tagId")
    ]
)
data class PatternTagCrossRef(
    val patternId: Long,
    val tagId: Long
)
```

### Relationships

```kotlin
data class PatternWithTestSessions(
    @Embedded val pattern: Pattern,
    @Relation(
        parentColumn = "id",
        entityColumn = "patternId"
    )
    val testSessions: List<TestSession>
)

data class PatternWithTags(
    @Embedded val pattern: Pattern,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PatternTagCrossRef::class,
            parentColumn = "patternId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
```

### Database Class

```kotlin
@Database(
    entities = [
        Pattern::class,
        TestSession::class,
        Tag::class,
        PatternTagCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class JugglingDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun tagDao(): TagDao
    
    companion object {
        @Volatile
        private var INSTANCE: JugglingDatabase? = null
        
        fun getDatabase(context: Context): JugglingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JugglingDatabase::class.java,
                    "juggling_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

## 5. Navigation Structure

### Screen Mapping

The app will have 6 main screens:

1. **Main (Pattern List)** - Replaces HomeFragment
2. **Pattern Detail** - Replaces GalleryFragment
3. **Add/Edit Pattern** - New screen
4. **Progress Chart** - New screen
5. **Test History** - Replaces SlideshowFragment
6. **Settings** - New screen

### Navigation Graph

```xml
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/mobile_navigation"
    app:startDestination="@+id/nav_pattern_list">

    <fragment
        android:id="@+id/nav_pattern_list"
        android:name="com.example.jugglingtracker.ui.patterns.list.PatternListFragment"
        android:label="@string/menu_patterns"
        tools:layout="@layout/fragment_pattern_list">
        <action
            android:id="@+id/action_pattern_list_to_pattern_detail"
            app:destination="@id/nav_pattern_detail" />
        <action
            android:id="@+id/action_pattern_list_to_pattern_edit"
            app:destination="@id/nav_pattern_edit" />
    </fragment>

    <fragment
        android:id="@+id/nav_pattern_detail"
        android:name="com.example.jugglingtracker.ui.patterns.detail.PatternDetailFragment"
        android:label="@string/menu_pattern_detail"
        tools:layout="@layout/fragment_pattern_detail">
        <argument
            android:name="patternId"
            app:argType="long" />
        <action
            android:id="@+id/action_pattern_detail_to_pattern_edit"
            app:destination="@id/nav_pattern_edit" />
        <action
            android:id="@+id/action_pattern_detail_to_progress_chart"
            app:destination="@id/nav_progress_chart" />
        <action
            android:id="@+id/action_pattern_detail_to_test_history"
            app:destination="@id/nav_test_history" />
    </fragment>

    <fragment
        android:id="@+id/nav_pattern_edit"
        android:name="com.example.jugglingtracker.ui.patterns.edit.PatternEditFragment"
        android:label="@string/menu_pattern_edit"
        tools:layout="@layout/fragment_pattern_edit">
        <argument
            android:name="patternId"
            app:argType="long"
            app:nullable="true"
            android:defaultValue="-1L" />
    </fragment>

    <fragment
        android:id="@+id/nav_progress_chart"
        android:name="com.example.jugglingtracker.ui.progress.ProgressChartFragment"
        android:label="@string/menu_progress_chart"
        tools:layout="@layout/fragment_progress_chart">
        <argument
            android:name="patternId"
            app:argType="long"
            app:nullable="true"
            android:defaultValue="-1L" />
    </fragment>

    <fragment
        android:id="@+id/nav_test_history"
        android:name="com.example.jugglingtracker.ui.history.TestHistoryFragment"
        android:label="@string/menu_test_history"
        tools:layout="@layout/fragment_test_history">
        <argument
            android:name="patternId"
            app:argType="long"
            app:nullable="true"
            android:defaultValue="-1L" />
    </fragment>

    <fragment
        android:id="@+id/nav_settings"
        android:name="com.example.jugglingtracker.ui.settings.SettingsFragment"
        android:label="@string/menu_settings"
        tools:layout="@layout/fragment_settings" />
</navigation>
```

### Navigation Arguments

- **patternId**: Used to pass the pattern ID between fragments
  - From PatternListFragment to PatternDetailFragment
  - From PatternDetailFragment to PatternEditFragment
  - From PatternDetailFragment to ProgressChartFragment
  - From PatternDetailFragment to TestHistoryFragment

### Navigation Drawer Menu

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    tools:showIn="navigation_view">

    <group android:checkableBehavior="single">
        <item
            android:id="@+id/nav_pattern_list"
            android:icon="@drawable/ic_menu_patterns"
            android:title="@string/menu_patterns" />
        <item
            android:id="@+id/nav_progress_chart"
            android:icon="@drawable/ic_menu_chart"
            android:title="@string/menu_progress_chart" />
        <item
            android:id="@+id/nav_test_history"
            android:icon="@drawable/ic_menu_history"
            android:title="@string/menu_test_history" />
    </group>

    <item android:title="@string/menu_other">
        <menu>
            <item
                android:id="@+id/nav_settings"
                android:icon="@drawable/ic_menu_settings"
                android:title="@string/menu_settings" />
        </menu>
    </item>
</menu>
```

## 6. Dependency Management

### Required Dependencies

#### Core Dependencies
```kotlin
// Kotlin
implementation(libs.kotlin.stdlib)
implementation(libs.androidx.core.ktx)

// AppCompat and UI
implementation(libs.androidx.appcompat)
implementation(libs.material)
implementation(libs.androidx.constraintlayout)
implementation(libs.androidx.recyclerview)
implementation(libs.androidx.swiperefreshlayout)

// Architecture Components
implementation(libs.androidx.lifecycle.livedata.ktx)
implementation(libs.androidx.lifecycle.viewmodel.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)

// Navigation
implementation(libs.androidx.navigation.fragment.ktx)
implementation(libs.androidx.navigation.ui.ktx)

// Room
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
kapt(libs.androidx.room.compiler)

// Coroutines
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.core)

// MPAndroidChart for charts
implementation(libs.mpandroidchart)

// Video Trimmer
implementation(libs.k4l.video.trimmer)

// CameraX
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
implementation(libs.androidx.camera.extensions)

// Glide for image loading
implementation(libs.glide)
kapt(libs.glide.compiler)
```

### Version Compatibility

Update the `gradle/libs.versions.toml` file to include all required dependencies with compatible versions:

```toml
[versions]
agp = "8.11.1"
kotlin = "2.0.21"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"
constraintlayout = "2.1.4"
lifecycleLivedataKtx = "2.6.1"
lifecycleViewmodelKtx = "2.6.1"
lifecycleRuntimeKtx = "2.6.1"
navigationFragmentKtx = "2.6.0"
navigationUiKtx = "2.6.0"
roomVersion = "2.6.0"
coroutinesVersion = "1.7.3"
mpandroidchartVersion = "v3.1.0"
videoTrimmerVersion = "1.0.3"
cameraxVersion = "1.3.0"
glideVersion = "4.16.0"
recyclerviewVersion = "1.3.2"
swiperefreshlayoutVersion = "1.1.0"

[libraries]
# Existing libraries...

# New libraries
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "roomVersion" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "roomVersion" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "roomVersion" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutinesVersion" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutinesVersion" }
mpandroidchart = { group = "com.github.PhilJay", name = "MPAndroidChart", version.ref = "mpandroidchartVersion" }
k4l-video-trimmer = { group = "com.github.a914-gowtham", name = "android-video-trimmer", version.ref = "videoTrimmerVersion" }
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "cameraxVersion" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraxVersion" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraxVersion" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "cameraxVersion" }
androidx-camera-extensions = { group = "androidx.camera", name = "camera-extensions", version.ref = "cameraxVersion" }
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glideVersion" }
glide-compiler = { group = "com.github.bumptech.glide", name = "compiler", version.ref = "glideVersion" }
androidx-recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerviewVersion" }
androidx-swiperefreshlayout = { group = "androidx.swiperefreshlayout", name = "swiperefreshlayout", version.ref = "swiperefreshlayoutVersion" }

[plugins]
# Existing plugins...
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

### Repository Configuration

Add the following to the project's `settings.gradle.kts` file to include the required repositories:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // For MPAndroidChart and video-trimmer
    }
}
```

## 7. File Organization Strategy

### Screen Organization

Each screen will have its own package under the `ui` package, containing:
- Fragment class
- ViewModel class
- Adapter classes (if needed)
- Custom view classes (if needed)

### Layout Organization

```
res/layout/
├── activity_main.xml
├── app_bar_main.xml
├── content_main.xml
├── nav_header_main.xml
├── fragment_pattern_list.xml
├── fragment_pattern_detail.xml
├── fragment_pattern_edit.xml
├── fragment_progress_chart.xml
├── fragment_test_history.xml
├── fragment_settings.xml
├── item_pattern.xml
├── item_test_session.xml
├── item_tag.xml
├── dialog_add_test_session.xml
└── dialog_add_tag.xml
```

### Resource Organization

```
res/
├── drawable/
│   ├── ic_launcher_background.xml
│   ├── ic_launcher_foreground.xml
│   ├── ic_menu_patterns.xml
│   ├── ic_menu_chart.xml
│   ├── ic_menu_history.xml
│   ├── ic_menu_settings.xml
│   ├── ic_add.xml
│   ├── ic_edit.xml
│   ├── ic_delete.xml
│   ├── ic_video.xml
│   ├── ic_tag.xml
│   └── side_nav_bar.xml
├── menu/
│   ├── activity_main_drawer.xml
│   └── main.xml
├── values/
│   ├── colors.xml
│   ├── dimens.xml
│   ├── strings.xml
│   └── themes.xml
├── values-night/
│   └── themes.xml
└── navigation/
    └── mobile_navigation.xml
```

### String Resources

```xml
<resources>
    <string name="app_name">Juggling Tracker</string>
    
    <!-- Menu Strings -->
    <string name="menu_patterns">Patterns</string>
    <string name="menu_pattern_detail">Pattern Detail</string>
    <string name="menu_pattern_edit">Edit Pattern</string>
    <string name="menu_progress_chart">Progress Chart</string>
    <string name="menu_test_history">Test History</string>
    <string name="menu_settings">Settings</string>
    <string name="menu_other">Other</string>
    
    <!-- Pattern List Screen -->
    <string name="add_pattern">Add Pattern</string>
    <string name="no_patterns">No patterns yet. Add one to get started!</string>
    <string name="pattern_difficulty">Difficulty: %1$d/10</string>
    
    <!-- Pattern Detail Screen -->
    <string name="pattern_description">Description</string>
    <string name="pattern_difficulty_label">Difficulty</string>
    <string name="pattern_tags">Tags</string>
    <string name="pattern_video">Video</string>
    <string name="pattern_test_sessions">Test Sessions</string>
    <string name="add_test_session">Add Test Session</string>
    <string name="view_progress">View Progress</string>
    <string name="view_history">View History</string>
    <string name="edit_pattern">Edit Pattern</string>
    
    <!-- Pattern Edit Screen -->
    <string name="pattern_name">Pattern Name</string>
    <string name="pattern_description_hint">Describe the pattern</string>
    <string name="pattern_difficulty_hint">Set difficulty (1-10)</string>
    <string name="record_video">Record Video</string>
    <string name="import_video">Import Video</string>
    <string name="trim_video">Trim Video</string>
    <string name="add_tag">Add Tag</string>
    <string name="save">Save</string>
    <string name="cancel">Cancel</string>
    
    <!-- Test Session Dialog -->
    <string name="test_session_date">Date</string>
    <string name="test_session_duration">Duration (minutes)</string>
    <string name="test_session_success_count">Successful Attempts</string>
    <string name="test_session_attempt_count">Total Attempts</string>
    <string name="test_session_notes">Notes</string>
    <string name="test_session_video">Record Video</string>
    
    <!-- Progress Chart Screen -->
    <string name="success_rate">Success Rate</string>
    <string name="attempts_per_session">Attempts per Session</string>
    <string name="duration_per_session">Duration per Session</string>
    <string name="filter_by_date">Filter by Date</string>
    
    <!-- Test History Screen -->
    <string name="test_history_empty">No test sessions recorded yet</string>
    <string name="test_session_success_rate">Success Rate: %1$.1f%%</string>
    <string name="test_session_attempts">Attempts: %1$d</string>
    <string name="test_session_duration_format">Duration: %1$d min</string>
    
    <!-- Settings Screen -->
    <string name="settings_theme">Theme</string>
    <string name="settings_theme_light">Light</string>
    <string name="settings_theme_dark">Dark</string>
    <string name="settings_theme_system">System Default</string>
    <string name="settings_video_quality">Video Quality</string>
    <string name="settings_video_storage">Video Storage Location</string>
    <string name="settings_backup">Backup Data</string>
    <string name="settings_restore">Restore Data</string>
    <string name="settings_about">About</string>
</resources>
```

## 8. Implementation Guidelines

### Code Style and Best Practices

1. **Kotlin Coding Conventions**
   - Follow the official Kotlin coding conventions
   - Use meaningful names for variables, functions, and classes
   - Keep functions small and focused on a single responsibility

2. **Architecture Guidelines**
   - Strictly adhere to the MVVM architecture
   - Keep UI logic in ViewModels, not in Fragments or Activities
   - Use Repository pattern for data operations
   - Use dependency injection for providing dependencies

3. **Database Guidelines**
   - Use Room for all database operations
   - Define clear entity relationships
   - Use DAOs for database access
   - Use transactions for complex operations

4. **UI Guidelines**
   - Follow Material Design guidelines
   - Use ConstraintLayout for complex layouts
   - Use RecyclerView for lists
   - Use ViewBinding for view access

### Implementation Phases

1. **Phase 1: Project Setup**
   - Rename package and update configuration files
   - Set up the new package structure
   - Add required dependencies
   - Create the database schema

2. **Phase 2: Core Features**
   - Implement the Pattern List screen
   - Implement the Pattern Detail screen
   - Implement the Pattern Edit screen
   - Implement basic navigation

3. **Phase 3: Advanced Features**
   - Implement the Progress Chart screen
   - Implement the Test History screen
   - Implement the Settings screen
   - Implement video recording and trimming

4. **Phase 4: Polish and Testing**
   - Add animations and transitions
   - Implement error handling
   - Add unit and instrumentation tests
   - Optimize performance

### Testing Strategy

1. **Unit Tests**
   - Test ViewModels with JUnit and MockK
   - Test Repositories with JUnit and MockK
   - Test DAOs with Room testing

2. **Instrumentation Tests**
   - Test UI flows with Espresso
   - Test database migrations
   - Test navigation

3. **Manual Testing**
   - Test video recording and trimming
   - Test chart rendering
   - Test on different device sizes and orientations

## 9. Conclusion

This architecture plan provides a comprehensive guide for transforming the current Android project into a Juggling Progress Tracker app. By following this plan, the development team can implement a robust, maintainable, and feature-rich application that adheres to Android best practices and provides a great user experience.

The plan covers all the required aspects:
- Package structure refactoring
- MVVM architecture implementation
- Room database design
- Navigation structure
- Dependency management
- File organization strategy

The next steps are to implement the architecture as outlined in this document, starting with the project setup and core features.