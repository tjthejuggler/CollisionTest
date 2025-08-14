# Juggling Progress Tracker

*Last updated: 2025-08-14T16:16:00Z*

## Overview

The Juggling Progress Tracker is an Android application designed to help jugglers track their practice sessions, monitor progress, and manage juggling patterns. The app provides comprehensive tools for recording test sessions, analyzing performance data, and organizing juggling patterns with video demonstrations. Features intelligent usage tracking with dynamic theming that adapts based on user activity levels.

## Features

### Core Functionality
- **Pattern Management**: Create, edit, and organize juggling patterns with detailed descriptions
- **Progress Tracking**: Record test sessions with success rates, attempt counts, and duration
- **Video Integration**: Record, import, trim, and export videos for pattern demonstrations
- **Performance Analytics**: View progress charts and statistics over time
- **Usage Statistics**: Comprehensive stats screen showing weekly scores, usage levels, and activity trends
- **Usage Tracking**: Intelligent activity monitoring with weekly scoring system
- **Dynamic Theming**: Accent colors that change based on usage activity levels
- **Tag System**: Organize patterns with customizable tags and colors
- **Pattern Relationships**: Define prerequisites, dependents, and related patterns

### User Interface
- **Material Design**: Modern, intuitive interface following Material Design guidelines
- **Dynamic Dark Theme Support**: Complete dark theme with dynamic accent colors based on usage activity
- **Navigation Drawer**: Easy access to all major sections of the app
- **Responsive Design**: Optimized for various screen sizes and orientations
- **Accessibility**: Full accessibility support with content descriptions and screen reader compatibility

### Data Management
- **Local Storage**: All data stored locally using Room database
- **Comprehensive Backup/Restore**: Complete backup system including database and video files
- **Data Export/Import**: Full backup and restore functionality for data portability
- **Offline Support**: Full functionality without internet connection

## Technical Architecture

### Technology Stack
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (SQLite)
- **UI Framework**: Android Jetpack components
- **Charts**: MPAndroidChart
- **Video Processing**: CameraX and video trimmer libraries
- **Dependency Injection**: Hilt for dependency injection
- **Usage Analytics**: Comprehensive activity tracking and scoring system

### Project Structure
```
com.example.jugglingtracker/
├── data/
│   ├── entities/          # Room database entities
│   ├── dao/              # Data Access Objects
│   ├── database/         # Database configuration
│   └── repository/       # Repository pattern implementation
├── ui/
│   ├── patterns/         # Pattern management screens
│   ├── stats/            # Usage statistics and analytics screen
│   ├── progress/         # Progress charts and analytics
│   ├── history/          # Test session history
│   ├── settings/         # App settings and preferences
│   └── theme/            # Dynamic theming system
├── services/             # Background services and usage tracking
├── utils/                # Utility classes
└── di/                   # Hilt dependency injection modules
```

### Database Schema
- **Pattern**: Core juggling pattern entity with metadata
- **TestSession**: Individual practice session records
- **Tag**: Categorization system for patterns
- **UsageEvent**: Individual user activity tracking records
- **WeeklyUsage**: Aggregated weekly usage statistics and scoring
- **Cross-reference tables**: Many-to-many relationships for tags and pattern relationships

## String Resources Structure

The app includes comprehensive string resources organized into logical groups:

### Resource Categories
- **App Identity**: App name, navigation, and branding
- **UI Elements**: Button labels, menu items, and interactive elements
- **Screen Content**: Titles, descriptions, and section headers
- **User Feedback**: Success messages, error handling, and confirmations
- **Data Display**: Formatting strings for statistics and measurements
- **Accessibility**: Content descriptions and screen reader support

### Localization Support
- All user-facing text defined as string resources
- Consistent naming conventions for easy maintenance
- Plurals support for count-dependent text
- String arrays for dropdown options and lists

## App Branding

### Color Scheme
The app uses a juggling-themed color palette inspired by colorful juggling balls:

#### Light Theme
- **Primary Colors**: Blue (#3182CE) and Green (#38A169)
- **Accent Colors**: Red (#E53E3E), Yellow (#ECC94B), Orange (#ED8936), Purple (#805AD5)
- **Background**: White (#FFFFFF) with light gray surfaces (#F7FAFC)
- **Text**: Dark gray (#1A202C) for primary text, medium gray (#4A5568) for secondary

#### Dark Theme
- **Primary Colors**: Light Blue (#4A90E2) and Green (#4CAF50)
- **Background**: Pure Black (#000000) with dark gray surfaces (#1E1E1E)
- **Text**: White (#FFFFFF) for primary text, light gray (#B3B3B3) for secondary
- **Difficulty Colors**: Adjusted for dark background visibility
- **Tag Colors**: Enhanced brightness for dark theme compatibility

### Icons and Visual Design
- **App Icon**: Custom juggling balls design with motion trails
- **Navigation Icons**: Themed icons for patterns, charts, history, and settings
- **Consistent Visual Language**: Circular elements representing juggling balls throughout the UI

## Development Status

### Completed Components
- ✅ **Database Architecture**: Complete Room database implementation with entities, DAOs, and repositories
- ✅ **String Resources**: Comprehensive localization-ready string resources
- ✅ **App Branding**: Custom color scheme and icon set
- ✅ **Dark Theme Implementation**: Complete dark theme with black backgrounds and proper contrast ratios
- ✅ **Project Structure**: Clean architecture with proper package organization

### In Progress
- 🔄 **UI Implementation**: Fragment and ViewModel development
- 🔄 **Navigation**: Navigation component integration
- 🔄 **Video Features**: Camera integration and video processing

### Recently Completed
- ✅ **Usage Statistics Screen**: Complete stats screen with weekly scores, usage levels, and trends

### Planned Features
- 📋 **Pattern List Screen**: Browse and manage juggling patterns
- 📋 **Pattern Detail Screen**: View pattern information and test sessions
- 📋 **Pattern Editor**: Create and edit patterns with video support
- 📋 **Progress Charts**: Visual analytics and performance tracking
- 📋 **Test History**: Detailed session history and statistics
- 📋 **Settings**: App preferences and data management

## Installation and Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API level 24 or higher
- Kotlin 1.8.0 or later

### Build Configuration
- **Target SDK**: 34
- **Minimum SDK**: 24
- **Compile SDK**: 34
- **Build Tools**: 34.0.0

### Dependencies
Key dependencies include:
- AndroidX libraries for modern Android development
- Room database for local data persistence
- Navigation Component for screen navigation
- MPAndroidChart for progress visualization
- CameraX for video recording capabilities

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Maintain MVVM architecture separation
- Include comprehensive documentation

### Testing Strategy
- Unit tests for ViewModels and Repositories
- Instrumentation tests for database operations
- UI tests for critical user flows
- Manual testing for video and camera features

## License

This project is developed as part of an Android application development exercise. All rights reserved.

## Contact

For questions or contributions, please refer to the project documentation or contact the development team.

---

*This README was last updated on 2025-08-14 at 15:49 UTC as part of the dark theme implementation.*

## Dark Theme Implementation Details

### Theme Architecture
The app implements a comprehensive dark theme using Android's DayNight theme system:

#### Files Modified/Created
- **`values/colors.xml`**: Added dark theme color definitions
- **`values-night/colors.xml`**: Color overrides for dark theme
- **`values/themes.xml`**: Enhanced light theme with proper color attributes
- **`values-night/themes.xml`**: Complete dark theme implementation
- **`drawable-night/side_nav_bar.xml`**: Dark variant of navigation drawer background

#### Key Features
- **Pure Black Background**: Uses #000000 for maximum contrast and OLED efficiency
- **Proper Contrast Ratios**: All text and UI elements meet accessibility guidelines
- **Automatic Theme Switching**: Follows system dark mode preference
- **Enhanced Visibility**: Fixed number input field visibility issues
- **Consistent Branding**: Maintains app identity while adapting to dark theme

#### Color Adaptations
- Primary colors adjusted for dark background visibility
- Difficulty indicators enhanced for better contrast
- Tag colors brightened for dark theme compatibility
- Chart and graph colors optimized for dark backgrounds
- Navigation elements properly themed for dark mode

The dark theme provides a comfortable viewing experience in low-light conditions while maintaining full functionality and visual hierarchy.

## Usage Tracking and Dynamic Theming System

### Overview
The app features an intelligent usage tracking system that monitors user activity and dynamically adjusts the app's accent colors based on weekly engagement levels. This creates a personalized experience that reflects how actively the user is practicing juggling.

### Usage Tracking Components

#### Database Entities
- **`UsageEvent`**: Records individual user actions with timestamps, event types, and metadata
- **`WeeklyUsage`**: Aggregates weekly statistics including total points, patterns created, tests completed, and more

#### Event Types and Scoring
The system tracks various user activities with different point values:

| Activity | Base Points | Bonus Criteria |
|----------|-------------|----------------|
| Pattern Created | 10 | - |
| Pattern Edited | 5 | - |
| Test Completed | 15 | +1 per 30 seconds (max +10) |
| Video Recorded | 8 | +1 per 10 seconds (max +5) |
| Pattern Viewed | 1 | - |
| App Opened | 2 | - |
| Progress Viewed | 3 | - |

#### Usage Levels and Colors
The system defines 8 usage levels with corresponding colors:

| Level | Name | Points Range | Light Color | Dark Color |
|-------|------|--------------|-------------|------------|
| 0 | Inactive | 0-9 | Gray | Gray |
| 1 | Beginner | 10-49 | Green | Light Green |
| 2 | Casual | 50-99 | Yellow | Yellow |
| 3 | Regular | 100-199 | Orange | Orange |
| 4 | Active | 200-349 | Blue | Light Blue |
| 5 | Dedicated | 350-549 | Purple | Light Purple |
| 6 | Expert | 550-799 | Red | Light Red |
| 7 | Master | 800+ | Deep Purple | Purple |

### Dynamic Theming Implementation

#### Theme Manager
The `DynamicThemeManager` class handles:
- Real-time accent color updates based on usage levels
- Theme persistence across app sessions
- Dark/light theme compatibility
- User preference management for enabling/disabling dynamic colors

#### Color Integration
- Primary and secondary colors automatically adjust based on weekly usage
- Smooth color transitions maintain visual consistency
- Theme colors are applied throughout the app using Material Design color attributes

### Usage Tracking Service

#### Automatic Tracking
The `UsageTrackingService` automatically tracks:
- App launches and session duration
- Pattern creation, editing, and viewing
- Test session starts, completions, and cancellations
- Video recording and trimming activities
- Navigation between app sections

#### Weekly Reset System
- Usage points reset every Monday at 00:00
- Historical data is preserved for analytics
- Automatic cleanup of old usage events (90 days)
- Weekly usage summaries retained for 1 year

### Integration Points

#### MainActivity Integration
- Automatic session tracking on app launch/resume
- Navigation event tracking
- Theme refresh on app resume

#### Database Migration
- Database version updated from 1 to 2
- Automatic migration creates usage tracking tables
- Backward compatibility maintained

### Privacy and Performance

#### Data Privacy
- All usage data stored locally on device
- No external analytics or tracking services
- User can disable usage tracking in settings

#### Performance Optimization
- Asynchronous database operations
- Efficient weekly aggregation queries
- Automatic cleanup of old data
- Minimal impact on app performance

### Future Enhancements
- Achievement system based on usage milestones
- Export usage data for external analysis
- Customizable color themes beyond usage-based selection

This usage tracking system creates an engaging, personalized experience that encourages consistent practice while providing valuable insights into juggling progress patterns.

## Usage Statistics Screen Implementation

### Overview
A comprehensive statistics screen has been implemented to display usage tracking data in a user-friendly interface. The screen provides insights into weekly activity, usage levels, and historical trends.

### Components Created

#### UI Components
- **`StatsFragment`**: Main fragment displaying usage statistics with Material Design cards
- **`StatsViewModel`**: ViewModel managing data flow and UI state for the stats screen
- **`WeeklyTrendAdapter`**: RecyclerView adapter for displaying weekly usage trends
- **`StatsInfoDialogFragment`**: Information dialog explaining the rating system

#### Layout Files
- **`fragment_stats.xml`**: Main stats screen layout with cards for current week, general usage, and trends
- **`item_weekly_trend.xml`**: Layout for individual weekly trend items in the RecyclerView
- **`dialog_stats_info.xml`**: Comprehensive information dialog layout

#### Resources
- **`ic_stats.xml`**: Statistics icon for navigation drawer
- **String resources**: Complete set of strings for stats screen, info dialog, and rating system explanations

### Features

#### Current Week Statistics
- **Weekly Score**: Current week's accumulated points with visual progress indicator
- **Usage Level**: Current level (Inactive to Master) with color-coded display
- **Progress Bar**: Visual representation of progress within current level

#### General Usage Statistics
- **Patterns Created**: Total number of patterns created this week
- **Tests Completed**: Number of test sessions completed
- **Videos Recorded**: Count of recorded practice videos
- **App Opens**: Number of times the app was launched
- **Total Test Time**: Cumulative practice time in minutes
- **Average Weekly Points**: Rolling average of weekly scores

#### Weekly Trends
- **Historical Data**: Display of past weeks' performance
- **Visual Indicators**: Color-coded level indicators for each week
- **Trend Analysis**: Easy comparison of current vs. previous weeks

#### Information System
- **Rating System Explanation**: Detailed breakdown of point values for different activities
- **Usage Levels Guide**: Complete explanation of all 8 usage levels with point ranges
- **Duration Bonuses**: Information about bonus points for longer activities

### Navigation Integration
- **Menu Item**: Added "Usage Stats" to navigation drawer between Patterns and Test History
- **Navigation Flow**: Seamless integration with existing app navigation
- **MainActivity Updates**: Proper handling of stats screen in navigation configuration

### Technical Implementation

#### Architecture
- **MVVM Pattern**: Clean separation of concerns with ViewModel managing business logic
- **Reactive UI**: Uses StateFlow for reactive UI updates
- **Dependency Injection**: Hilt integration for repository access

#### Data Flow
- **Repository Integration**: Direct access to `UsageTrackingRepository` for real-time data
- **Efficient Queries**: Optimized database queries for weekly trends and statistics
- **Error Handling**: Comprehensive error handling with user-friendly messages

#### UI/UX Design
- **Material Design**: Follows Material Design 3 guidelines with proper theming
- **Dark Theme Support**: Full compatibility with app's dark theme implementation
- **Responsive Layout**: Adapts to different screen sizes and orientations
- **Accessibility**: Proper content descriptions and screen reader support

### Files Created/Modified

#### New Files
- `app/src/main/java/com/example/jugglingtracker/ui/stats/StatsFragment.kt`
- `app/src/main/java/com/example/jugglingtracker/ui/stats/StatsViewModel.kt`
- `app/src/main/java/com/example/jugglingtracker/ui/adapters/WeeklyTrendAdapter.kt`
- `app/src/main/java/com/example/jugglingtracker/ui/dialogs/StatsInfoDialogFragment.kt`
- `app/src/main/res/layout/fragment_stats.xml`
- `app/src/main/res/layout/item_weekly_trend.xml`
- `app/src/main/res/layout/dialog_stats_info.xml`
- `app/src/main/res/drawable/ic_stats.xml`

#### Modified Files
- `app/src/main/res/menu/activity_main_drawer.xml`: Added stats menu item
- `app/src/main/res/navigation/mobile_navigation.xml`: Added stats fragment navigation
- `app/src/main/res/values/strings.xml`: Added comprehensive stats-related strings
- `app/src/main/java/com/example/jugglingtracker/MainActivity.kt`: Updated navigation configuration

### User Experience
The stats screen provides users with:
- **Motivation**: Visual progress indicators encourage continued engagement
- **Transparency**: Clear explanation of how the scoring system works
- **Insights**: Historical trends help users understand their practice patterns
- **Gamification**: Level-based system makes usage tracking engaging

This implementation completes the usage tracking system by providing a comprehensive interface for users to view and understand their juggling practice statistics.

## Backup and Restore System

### Overview
The app features a comprehensive backup and restore system that allows users to create complete backups of their juggling practice data and restore it on the same or different devices. The system ensures data integrity and includes all database records, video files, and app settings.

### Backup Features

#### Complete Data Coverage
- **Database Export**: All database tables including patterns, test sessions, tags, usage events, and relationships
- **Video Files**: All recorded and imported video files associated with patterns and test sessions
- **Metadata**: Backup creation date, app version, device information, and data integrity checksums
- **Settings**: App preferences and configuration data

#### Backup Format
- **ZIP Archive**: Single compressed file containing all backup data
- **JSON Database Export**: Human-readable database export with proper relationships
- **Organized Structure**: Videos organized in folders matching database references
- **Metadata File**: Complete backup information for validation and compatibility

#### Backup Process
1. **Database Export**: Exports all tables with proper foreign key relationships maintained
2. **Video Collection**: Copies all video files referenced in patterns and test sessions
3. **Metadata Creation**: Generates backup metadata with checksums and version information
4. **ZIP Creation**: Packages everything into a single, portable backup file
5. **Progress Tracking**: Real-time progress updates during backup creation

### Restore Features

#### Data Validation
- **Backup Integrity**: Validates backup file structure and checksums before restoration
- **Version Compatibility**: Checks backup version compatibility with current app version
- **Data Consistency**: Ensures all database relationships are properly maintained

#### Restore Process
1. **Backup Validation**: Verifies backup file integrity and compatibility
2. **Data Clearing**: Safely clears existing data (with user confirmation)
3. **Database Import**: Restores all database tables in correct dependency order
4. **Video Restoration**: Restores all video files to correct locations
5. **Progress Tracking**: Real-time progress updates during restoration

#### Conflict Resolution
- **Complete Replacement**: Current implementation replaces all existing data
- **Data Integrity**: Maintains all foreign key relationships during import
- **Error Recovery**: Comprehensive error handling with rollback capabilities

### Technical Implementation

#### Core Components
- **`BackupManager`**: Main class handling backup and restore operations
- **`BackupRepository`**: Repository pattern integration for backup operations
- **`BackupMetadata`**: Data classes for backup metadata and validation
- **Export Data Classes**: Serializable versions of all database entities

#### Database Integration
- **Export Methods**: Added to all DAO classes for complete data export
- **Import Methods**: Bulk insert methods for efficient data restoration
- **Relationship Preservation**: Maintains all foreign key relationships during export/import

#### File Management
- **ZIP Operations**: Efficient compression and extraction of backup archives
- **Video File Handling**: Copies video files while preserving directory structure
- **Temporary File Management**: Safe handling of temporary files during operations

#### Progress Tracking
- **Real-time Updates**: Progress callbacks provide detailed operation status
- **User Feedback**: Clear progress messages and completion notifications
- **Error Reporting**: Detailed error messages with recovery suggestions

### User Interface Integration

#### Settings Screen Integration
- **Export Button**: Creates backup with progress indication
- **Import Button**: Restores from most recent backup (expandable to file picker)
- **Progress Display**: Real-time progress updates during operations
- **Status Messages**: Success/error feedback with detailed information

#### Backup Management
- **Automatic Naming**: Timestamp-based backup file naming
- **Storage Location**: Backups stored in app-specific external storage
- **File Validation**: Pre-restore validation with user-friendly error messages

### Security and Privacy

#### Data Protection
- **Local Storage**: All backups stored locally on device
- **No Cloud Integration**: Complete privacy with no external data transmission
- **User Control**: Users have full control over backup creation and restoration

#### File Permissions
- **External Storage**: Required permissions for backup file access
- **Scoped Storage**: Compatible with Android's scoped storage requirements
- **Permission Handling**: Proper runtime permission requests

### Backup File Structure
```
juggling_backup_YYYYMMDD_HHMMSS.zip
├── backup_metadata.json          # Backup information and checksums
├── database_export.json          # Complete database export
└── videos/                       # Video files directory
    ├── pattern_videos/           # Pattern demonstration videos
    └── session_videos/           # Test session recording videos
```

### Usage Instructions

#### Creating a Backup
1. Open Settings screen
2. Tap "Export Data" in Data Management section
3. Wait for backup creation progress to complete
4. Backup file saved to app's backup directory

#### Restoring from Backup
1. Open Settings screen
2. Tap "Import Data" in Data Management section
3. Confirm data replacement warning
4. Wait for restore process to complete
5. App data fully restored from backup

### Future Enhancements
- **Selective Restore**: Choose specific data types to restore
- **Cloud Backup Integration**: Optional cloud storage for backups
- **Backup Scheduling**: Automatic periodic backup creation
- **Multiple Backup Management**: UI for managing multiple backup files
- **Incremental Backups**: Backup only changed data since last backup

### Files Created/Modified

#### New Files
- `app/src/main/java/com/example/jugglingtracker/data/backup/BackupMetadata.kt`
- `app/src/main/java/com/example/jugglingtracker/data/backup/BackupManager.kt`
- `app/src/main/java/com/example/jugglingtracker/data/repository/BackupRepository.kt`

#### Modified Files
- All DAO classes: Added export and import methods
- `JugglingDatabase.kt`: Added clearAllTables method
- `SettingsViewModel.kt`: Integrated backup functionality
- `SettingsFragment.kt`: Updated to use backup repository
- `JugglingTrackerApplication.kt`: Added backup repository
- `ViewModelFactory.kt`: Added backup repository parameter
- `AndroidManifest.xml`: Added backup-related permissions
- `app/build.gradle.kts`: Added Kotlin serialization dependency

The backup and restore system provides users with complete data portability and peace of mind, ensuring their juggling practice data is never lost and can be easily transferred between devices.