# Roshni Games Android App

A comprehensive gaming platform built with Kotlin, Jetpack Compose, and modern Android architecture patterns.

## Architecture Overview

This project follows Clean Architecture principles with a multi-module structure designed for scalability and maintainability.

### Module Structure

```
├── app                           # Main application module
├── core/                         # Core functionality modules
│   ├── ui                       # UI components and utilities
│   ├── navigation               # Navigation components
│   ├── database                 # Database layer
│   ├── network                  # Network layer
│   ├── utils                    # Utility functions
│   └── design-system            # Design system components
├── feature/                     # Feature modules
│   ├── splash                   # Splash screen
│   ├── home                     # Home screen
│   ├── game-library             # Game library browser
│   ├── game-player              # Game player interface
│   ├── settings                 # Settings screen
│   ├── profile                  # User profile
│   ├── search                   # Search functionality
│   ├── achievements             # Achievements system
│   ├── leaderboard              # Leaderboard
│   ├── social                   # Social features
│   ├── parental-controls        # Parental controls
│   └── accessibility            # Accessibility features
├── service/                     # Service modules
│   ├── game-loader              # Game loading service
│   ├── background-sync          # Background synchronization
│   └── analytics                # Analytics and monitoring
└── game/                        # Game modules (dynamic features)
    ├── template                 # Template for new games
    ├── puzzle-001              # Example puzzle game
    └── ...                     # Additional game modules
```

## Architecture Layers

Each module follows Clean Architecture with three main layers:

### 1. Presentation Layer
- **ViewModels**: State management and UI logic
- **Screens**: UI composables
- **Components**: Reusable UI components

### 2. Domain Layer
- **Use Cases**: Business logic
- **Repositories**: Repository interfaces
- **Models**: Domain entities

### 3. Data Layer
- **Repositories**: Repository implementations
- **Data Sources**: Local and remote data sources
- **Models**: Data transfer objects

## Key Features

### 🚀 Dynamic Feature Modules
- On-demand game downloads
- Reduced base APK size (target: < 25MB)
- Conditional game delivery
- A/B testing support

### 🎮 Gaming Platform
- 200+ game modules support
- Multiple game categories (puzzle, word, arcade, strategy)
- Game library management
- Player progress tracking

### 🔧 Technical Features
- **Kotlin + Jetpack Compose**: Modern Android development
- **Hilt**: Dependency injection
- **Room**: Local database
- **Retrofit**: Network operations
- **Coroutines + Flow**: Asynchronous programming
- **Navigation Component**: Type-safe navigation
- **Compose UI**: Declarative UI framework

### 📊 Analytics & Monitoring
- Firebase Analytics
- Crashlytics
- Performance monitoring
- Sentry integration

### 🛒 Monetization
- In-app purchases
- Ads integration
- Subscription management

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.9.22+
- Gradle 8.2+
- Java 17

### Build Requirements
- Compile SDK: 34
- Target SDK: 34
- Minimum SDK: 24
- Kotlin: 1.9.22

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd roshni-games-2
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the project directory

3. **Sync project**
   - Android Studio will automatically sync Gradle files
   - Wait for the build to complete

4. **Create a game module** (optional)
   ```bash
   python scripts/setup_game_module.py \
     --id puzzle-001 \
     --name "Amazing Puzzle Game" \
     --category puzzle
   ```

## Module Dependencies

### Core Module Dependencies
```kotlin
// Each core module depends only on :core:utils
implementation(project(":core:utils"))

// Feature modules depend on relevant core modules
implementation(project(":core:ui"))
implementation(project(":core:navigation"))
implementation(project(":core:database"))
implementation(project(":core:network"))
```

### Feature Module Dependencies
```kotlin
// Feature modules can depend on core modules and other features
implementation(project(":core:ui"))
implementation(project(":core:navigation"))
implementation(project(":feature:home")) // Example cross-feature dependency
```

### Game Module Dependencies
```kotlin
// Game modules depend on core modules and services
implementation(project(":app"))
implementation(project(":core:ui"))
implementation(project(":service:game-loader"))
```

## Dynamic Feature Configuration

Game modules are configured as dynamic features for on-demand delivery:

1. **Add to settings.gradle**
   ```kotlin
   include(":game:puzzle-001")
   ```

2. **Update app/build.gradle.kts**
   ```kotlin
   implementation(project(":game:puzzle-001"))
   ```

3. **Configure in dynamic_feature_manifest.xml**
   ```xml
   <dist:module
       dist:title="@string/title_puzzle_game_001">
       <dist:delivery>
           <dist:on-demand />
       </dist:delivery>
   </dist:module>
   ```

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add documentation for public APIs
- Write comprehensive tests

### Architecture Patterns
- **Clean Architecture**: Separate concerns across layers
- **MVVM**: For presentation layer
- **Repository Pattern**: For data access
- **Use Case Pattern**: For business logic

### Testing
- Unit tests for all layers
- Integration tests for repositories
- UI tests for screens
- Use MockK for mocking

### Performance
- Optimize Compose UI
- Use lazy loading for lists
- Implement proper image loading
- Monitor app performance metrics

## Deployment

### Build Variants
- **Dev**: Development build with debug features
- **Staging**: Testing build
- **Prod**: Production build

### Distribution Channels
- **Play Store**: Main distribution channel
- **Amazon Appstore**: Alternative marketplace
- **Huawei AppGallery**: Huawei devices

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is proprietary software. All rights reserved.

## Support

For support and questions, please contact the development team.

---

**Built with ❤️ by the Roshni Games Team**
