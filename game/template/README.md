# Game Module Template

This template provides the structure for creating dynamic feature modules for individual games in the Roshni Games Android app.

## Structure

```
game/
└── [game-id]/
    ├── build.gradle.kts          # Module build configuration
    └── src/main/kotlin/com/roshni/games/game/[game-id]/
        ├── data/                 # Data layer
        │   ├── repository/       # Repository implementations
        │   ├── datasource/       # Data sources (local/remote)
        │   └── model/           # Data models
        ├── domain/              # Domain layer
        │   ├── repository/      # Repository interfaces
        │   ├── usecase/         # Business logic use cases
        │   └── model/           # Domain models
        └── presentation/        # Presentation layer
            ├── viewmodel/       # ViewModels
            ├── components/      # Reusable UI components
            └── screens/         # Screen composables
```

## Creating a New Game Module

1. **Copy the template:**
   ```bash
   cp -r game/template game/[game-id]
   ```

2. **Update build.gradle.kts:**
   - Replace `GAME_ID` with your actual game identifier (e.g., `puzzle-001`)
   - Update namespace accordingly
   - Add game-specific dependencies

3. **Update package structure:**
   - Replace `template` with your game identifier in all package declarations
   - Update class names and file names

4. **Add to settings.gradle:**
   ```kotlin
   include(":game:[game-id]")
   ```

5. **Configure dynamic feature:**
   - Add module to app's dynamic feature configuration
   - Set up download conditions and delivery

## Game Module Naming Convention

- **Puzzle games:** `puzzle-001`, `puzzle-002`, etc.
- **Word games:** `word-001`, `word-002`, etc.
- **Arcade games:** `arcade-001`, `arcade-002`, etc.
- **Strategy games:** `strategy-001`, `strategy-002`, etc.

## Dependencies

All game modules should depend on:
- `:app` - Main application module
- `:core:ui` - Core UI components
- `:core:navigation` - Navigation components
- `:core:design-system` - Design system components
- `:core:database` - Database access
- `:core:network` - Network operations
- `:core:utils` - Utility functions
- `:service:game-loader` - Game loading service

## Dynamic Feature Configuration

Game modules are configured as dynamic features to enable:
- On-demand downloads
- Reduced base APK size
- Conditional game delivery
- A/B testing capabilities

## Best Practices

1. **Clean Architecture:** Follow the data/domain/presentation layer separation
2. **Dependency Injection:** Use Hilt for dependency injection
3. **Compose UI:** Use Jetpack Compose for all UI components
4. **Resource Management:** Properly handle game assets and resources
5. **Error Handling:** Implement comprehensive error handling
6. **Testing:** Write unit tests for all layers
7. **Performance:** Optimize for smooth gameplay experience

## Example Game Module Structure

```
game/puzzle-001/
├── build.gradle.kts
└── src/main/kotlin/com/roshni/games/game/puzzle001/
    ├── data/
    │   ├── repository/
    │   │   └── GameRepositoryImpl.kt
    │   ├── datasource/
    │   │   ├── LocalDataSource.kt
    │   │   └── RemoteDataSource.kt
    │   └── model/
    │       ├── GameState.kt
    │       └── Puzzle.kt
    ├── domain/
    │   ├── repository/
    │   │   └── GameRepository.kt
    │   ├── usecase/
    │   │   ├── LoadPuzzleUseCase.kt
    │   │   └── SaveGameStateUseCase.kt
    │   └── model/
    │       └── Puzzle.kt
    └── presentation/
        ├── viewmodel/
        │   └── GameViewModel.kt
        ├── components/
        │   └── PuzzleBoard.kt
        └── screens/
            └── GameScreen.kt
```

## Asset Management

Game assets should be placed in:
- `src/main/res/raw/` - Audio files, small data files
- `src/main/assets/` - Larger game assets, configuration files
- External storage - Large game data (with proper permissions)

## Integration with Game Loader Service

Game modules should integrate with the `:service:game-loader` module for:
- Game discovery and loading
- Asset management
- Progress tracking
- Error reporting