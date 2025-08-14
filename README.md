# Juggling Progress Tracker

*Last updated: 2025-08-14T10:08:00Z*

## Overview

The Juggling Progress Tracker is an Android application designed to help jugglers track their practice sessions, monitor progress, and manage juggling patterns. The app provides comprehensive tools for recording test sessions, analyzing performance data, and organizing juggling patterns with video demonstrations.

## Features

### Core Functionality
- **Pattern Management**: Create, edit, and organize juggling patterns with detailed descriptions
- **Progress Tracking**: Record test sessions with success rates, attempt counts, and duration
- **Video Integration**: Record, import, trim, and export videos for pattern demonstrations
- **Performance Analytics**: View progress charts and statistics over time
- **Tag System**: Organize patterns with customizable tags and colors
- **Pattern Relationships**: Define prerequisites, dependents, and related patterns

### User Interface
- **Material Design**: Modern, intuitive interface following Material Design guidelines
- **Navigation Drawer**: Easy access to all major sections of the app
- **Responsive Design**: Optimized for various screen sizes and orientations
- **Accessibility**: Full accessibility support with content descriptions and screen reader compatibility

### Data Management
- **Local Storage**: All data stored locally using Room database
- **Data Export/Import**: Backup and restore functionality for data portability
- **Offline Support**: Full functionality without internet connection

## Technical Architecture

### Technology Stack
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (SQLite)
- **UI Framework**: Android Jetpack components
- **Charts**: MPAndroidChart
- **Video Processing**: CameraX and video trimmer libraries
- **Dependency Injection**: Manual DI with repository pattern

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
│   ├── progress/         # Progress charts and analytics
│   ├── history/          # Test session history
│   └── settings/         # App settings and preferences
├── utils/                # Utility classes
└── di/                   # Dependency injection
```

### Database Schema
- **Pattern**: Core juggling pattern entity with metadata
- **TestSession**: Individual practice session records
- **Tag**: Categorization system for patterns
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
- **Primary Colors**: Blue (#3182CE) and Green (#38A169)
- **Accent Colors**: Red (#E53E3E), Yellow (#ECC94B), Orange (#ED8936), Purple (#805AD5)
- **Difficulty Colors**: Gradient from green (easy) to purple (expert)
- **Tag Colors**: 10 distinct colors for pattern categorization

### Icons and Visual Design
- **App Icon**: Custom juggling balls design with motion trails
- **Navigation Icons**: Themed icons for patterns, charts, history, and settings
- **Consistent Visual Language**: Circular elements representing juggling balls throughout the UI

## Development Status

### Completed Components
- ✅ **Database Architecture**: Complete Room database implementation with entities, DAOs, and repositories
- ✅ **String Resources**: Comprehensive localization-ready string resources
- ✅ **App Branding**: Custom color scheme and icon set
- ✅ **Project Structure**: Clean architecture with proper package organization

### In Progress
- 🔄 **UI Implementation**: Fragment and ViewModel development
- 🔄 **Navigation**: Navigation component integration
- 🔄 **Video Features**: Camera integration and video processing

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

*This README was last updated on 2025-08-14 at 10:08 UTC as part of the comprehensive string resources and app branding implementation.*