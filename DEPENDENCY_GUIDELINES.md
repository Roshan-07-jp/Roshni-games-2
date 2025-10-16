# Dependency Management Guidelines

This document outlines the dependency management strategy for the Roshni Games Android app multi-module project.

## Overview

The project uses Gradle Version Catalogs for centralized dependency management and follows strict module boundary rules to ensure clean architecture and maintainability.

## Version Catalog

All dependencies are managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.22"
compose = "1.6.0"
room = "2.6.1"
# ... other versions

[plugins]
android-application = { id = "com.android.application", version.ref = "androidGradlePlugin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlinGradlePlugin" }
# ... other plugins

[libraries]
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlin" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
# ... other libraries
```

## Module Types and Dependencies

### 1. Core Modules (`:core:*`)

**Purpose**: Provide fundamental functionality used across the app

**Allowed Dependencies**:
- Other `:core:*` modules
- External libraries (kotlinx.coroutines, androidx.*, etc.)
- `:core:utils` as the base dependency

**Examples**:
```kotlin
// core:ui
dependencies {
    implementation(project(":core:utils"))
    implementation(project(":core:design-system"))
    implementation(libs.androidx.compose.foundation)
}

// core:database
dependencies {
    implementation(project(":core:utils"))
    implementation(libs.androidx.room.runtime)
}
```

### 2. Feature Modules (`:feature:*`)

**Purpose**: Implement specific app features

**Allowed Dependencies**:
- `:core:*` modules
- `:service:*` modules (if needed)
- Other `:feature:*` modules (sparingly)
- External libraries

**Examples**:
```kotlin
// feature:home
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:database"))
    implementation(project(":feature:splash")) // Cross-feature dependency
}

// feature:game-library
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":service:game-loader"))
}
```

### 3. Service Modules (`:service:*`)

**Purpose**: Provide app-wide services and background tasks

**Allowed Dependencies**:
- `:core:*` modules
- External libraries
- Other `:service:*` modules (if needed)

**Examples**:
```kotlin
// service:game-loader
dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(libs.androidx.work.runtime)
}

// service:analytics
dependencies {
    implementation(project(":core:utils"))
    implementation(libs.firebase.analytics)
}
```

### 4. Game Modules (`:game:*`)

**Purpose**: Individual game implementations as dynamic features

**Allowed Dependencies**:
- `:app` (for dynamic feature integration)
- `:core:*` modules
- `:service:*` modules
- Game-specific libraries

**Examples**:
```kotlin
// game:puzzle-001
dependencies {
    implementation(project(":app"))
    implementation(project(":core:ui"))
    implementation(project(":service:game-loader"))
    // Game engine specific dependencies
}
```

## Dependency Rules

### ✅ Allowed Dependencies

1. **Bottom-up dependencies**: Higher-level modules can depend on lower-level modules
   ```
   :feature:* → :core:*
   :service:* → :core:*
   :game:* → :core:*, :service:*
   ```

2. **Cross-feature dependencies**: Allowed but should be minimized
   ```
   :feature:home → :feature:splash
   ```

3. **External libraries**: Allowed in all modules based on need

### ❌ Forbidden Dependencies

1. **Circular dependencies**: No module should depend on another that depends on it
   ```
   :feature:a → :feature:b → :feature:a (FORBIDDEN)
   ```

2. **Reverse dependencies**: Lower-level modules should not depend on higher-level modules
   ```
   :core:ui → :feature:home (FORBIDDEN)
   :core:database → :service:analytics (FORBIDDEN)
   ```

3. **Implementation bleeding**: Avoid exposing implementation details across modules

## API Boundaries

### Interface Segregation

Each module should define clear APIs for interaction:

```kotlin
// Domain layer interface
interface GameRepository {
    suspend fun getGames(): Flow<List<Game>>
    suspend fun getGameById(id: String): Game?
}

// Implementation in data layer
class GameRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : GameRepository {
    // Implementation
}
```

### Dependency Injection

Use Hilt for dependency injection across modules:

```kotlin
// In feature module
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gameRepository: GameRepository, // From core module
    private val gameLoaderService: GameLoaderService // From service module
) : ViewModel()
```

## Dynamic Feature Considerations

### Module Installation Order

Game modules should be designed to handle dependencies correctly:

1. **Base dependencies**: Core modules are always available
2. **Service dependencies**: Services may need to be installed first
3. **Feature dependencies**: Other features may be required

### Asset Management

Game assets should be self-contained within each game module:

```
game:puzzle-001/
├── src/main/assets/
│   ├── levels/
│   ├── sounds/
│   └── images/
└── src/main/res/raw/
    └── config.properties
```

## Best Practices

### 1. Minimize Dependencies

- Only add dependencies you actually use
- Prefer implementation over api when possible
- Use compileOnly for annotation processors

### 2. Version Management

- Keep versions consistent across modules
- Use version catalogs for all version management
- Avoid version conflicts

### 3. Testing Dependencies

- Add test dependencies only where tests exist
- Use testImplementation for unit tests
- Use androidTestImplementation for instrumented tests

### 4. Build Optimization

- Use kapt for Kotlin annotation processing
- Enable build caching
- Configure proper ABI splits for game modules

## Adding New Dependencies

### 1. Update Version Catalog

Add new versions and libraries to `gradle/libs.versions.toml`:

```toml
[versions]
new-library = "1.0.0"

[libraries]
androidx-new-library = { group = "androidx.new", name = "library", version.ref = "new-library" }
```

### 2. Add to Modules

Add the dependency to relevant modules:

```kotlin
dependencies {
    implementation(libs.androidx.new.library)
}
```

### 3. Update Documentation

Update this guide if the dependency affects module boundaries.

## Troubleshooting

### Common Issues

1. **Circular Dependencies**
   - Use Gradle dependency analysis: `./gradlew dependencies --configuration runtimeClasspath`
   - Check for cycles in the dependency graph

2. **Version Conflicts**
   - Use `./gradlew dependencies --configuration runtimeClasspath | grep conflict`
   - Resolve by updating versions in the catalog

3. **Build Failures**
   - Check for missing dependencies
   - Verify module structure
   - Ensure proper plugin application

### Tools

- **Gradle Build Scan**: For detailed dependency analysis
- **Dependency Analysis Plugin**: For automated dependency checks
- **Version Catalog Update Plugin**: For keeping versions up to date

## Migration Guide

When refactoring existing code to follow these guidelines:

1. **Identify module boundaries**: Separate concerns clearly
2. **Extract interfaces**: Define APIs between layers
3. **Update dependencies**: Follow the dependency rules
4. **Test thoroughly**: Ensure functionality is preserved
5. **Document changes**: Update this guide as needed

---

This dependency management strategy ensures the project remains maintainable, scalable, and follows clean architecture principles.