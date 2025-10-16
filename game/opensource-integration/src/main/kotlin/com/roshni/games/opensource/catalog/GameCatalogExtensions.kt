package com.roshni.games.opensource.catalog

import com.roshni.games.opensource.metadata.*

/**
 * Extensions to add more games to the catalog
 */

// Card Games
fun OpenSourceGameCatalog.addCardGames() {
    // Solitaire
    addGame(GameMetadata(
        id = "solitaire",
        name = "PySolFC",
        displayName = "Solitaire Collection",
        description = "Collection of over 1000 solitaire card games",
        longDescription = "PySolFC is a collection of over 1000 solitaire card games. It includes popular games like Klondike, Spider, and FreeCell.",
        version = "2.21.0",
        category = GameCategory.CARD,
        subcategory = "Patience",
        difficulty = GameDifficulty.MEDIUM,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.GPL_V2,
            name = "GNU General Public License v2.0",
            url = "https://www.gnu.org/licenses/gpl-2.0.html",
            isCompatible = true,
            requiresAttribution = true,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = true
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/pysolfc/pysolfc",
            repositoryType = RepositoryType.GITHUB,
            branch = "master",
            stars = 320,
            forks = 95,
            issues = 22
        ),
        author = AuthorInfo(
            name = "PySolFC Team"
        ),
        tags = listOf("solitaire", "cards", "patience", "collection"),
        features = listOf(GameFeature.SAVE_LOAD, GameFeature.SETTINGS, GameFeature.HIGH_SCORE),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 64,
            recommendedRamMb = 128,
            minStorageMb = 25,
            recommendedStorageMb = 50
        ),
        statistics = GameStatistics(
            downloadCount = 400000,
            rating = 4.3f,
            ratingCount = 18000,
            playCount = 1500000,
            averagePlayTime = 20,
            completionRate = 0.4f
        ),
        attribution = AttributionInfo(
            displayText = "PySolFC - Solitaire Collection",
            licenseText = "GPL v2.0",
            sourceUrl = "https://github.com/pysolfc/pysolfc",
            attributionRequired = true
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 2, 15, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2004, 1, 1, 0, 0)
    ))

    // Blackjack
    addGame(GameMetadata(
        id = "blackjack",
        name = "OpenBlackjack",
        displayName = "Blackjack",
        description = "Classic casino blackjack card game",
        longDescription = "OpenBlackjack is a classic blackjack game with realistic casino rules and multiple betting options.",
        version = "1.8.0",
        category = GameCategory.CARD,
        subcategory = "Casino",
        difficulty = GameDifficulty.EASY,
        ageRating = AgeRating.TEEN,
        license = OpenSourceLicense(
            type = LicenseType.MIT,
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT",
            isCompatible = true,
            requiresAttribution = false,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = false
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/blackjack/blackjack",
            repositoryType = RepositoryType.GITHUB,
            branch = "main",
            stars = 180,
            forks = 75,
            issues = 12
        ),
        author = AuthorInfo(
            name = "Blackjack Community"
        ),
        tags = listOf("blackjack", "cards", "casino", "gambling"),
        features = listOf(GameFeature.SAVE_LOAD, GameFeature.SETTINGS, GameFeature.HIGH_SCORE),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 32,
            recommendedRamMb = 64,
            minStorageMb = 8,
            recommendedStorageMb = 15
        ),
        statistics = GameStatistics(
            downloadCount = 350000,
            rating = 4.2f,
            ratingCount = 15000,
            playCount = 1200000,
            averagePlayTime = 15,
            completionRate = 0.3f
        ),
        attribution = AttributionInfo(
            displayText = "OpenBlackjack",
            licenseText = "MIT License",
            sourceUrl = "https://github.com/blackjack/blackjack",
            attributionRequired = false
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 1, 30, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2018, 5, 1, 0, 0)
    ))
}

// Arcade Games
fun OpenSourceGameCatalog.addArcadeGames() {
    // Snake
    addGame(GameMetadata(
        id = "snake",
        name = "Snake",
        displayName = "Snake Game",
        description = "Classic Snake game where you grow by eating food",
        longDescription = "Guide the snake to eat food and grow longer while avoiding walls and your own tail.",
        version = "2.0.0",
        category = GameCategory.ARCADE,
        subcategory = "Classic",
        difficulty = GameDifficulty.EASY,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.MIT,
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT",
            isCompatible = true,
            requiresAttribution = false,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = false
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/snake-game/snake",
            repositoryType = RepositoryType.GITHUB,
            branch = "main",
            stars = 250,
            forks = 120,
            issues = 18
        ),
        author = AuthorInfo(
            name = "Snake Game Community"
        ),
        tags = listOf("snake", "arcade", "classic", "mobile-friendly"),
        features = listOf(GameFeature.HIGH_SCORE, GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 24,
            recommendedRamMb = 48,
            minStorageMb = 4,
            recommendedStorageMb = 8
        ),
        statistics = GameStatistics(
            downloadCount = 800000,
            rating = 4.4f,
            ratingCount = 40000,
            playCount = 5000000,
            averagePlayTime = 8,
            completionRate = 0.2f
        ),
        attribution = AttributionInfo(
            displayText = "Classic Snake Game",
            licenseText = "MIT License",
            sourceUrl = "https://github.com/snake-game/snake",
            attributionRequired = false
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 2, 5, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2016, 1, 1, 0, 0)
    ))

    // Pac-Man Clone
    addGame(GameMetadata(
        id = "pacman",
        name = "Pacman",
        displayName = "Pac-Man Clone",
        description = "Classic maze chase game",
        longDescription = "Navigate mazes, eat dots, and avoid ghosts in this classic arcade game clone.",
        version = "1.5.0",
        category = GameCategory.ARCADE,
        subcategory = "Maze Chase",
        difficulty = GameDifficulty.MEDIUM,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.GPL_V3,
            name = "GNU General Public License v3.0",
            url = "https://www.gnu.org/licenses/gpl-3.0.html",
            isCompatible = true,
            requiresAttribution = true,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = true
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/pacman/pacman",
            repositoryType = RepositoryType.GITHUB,
            branch = "master",
            stars = 400,
            forks = 180,
            issues = 25
        ),
        author = AuthorInfo(
            name = "Pacman Community"
        ),
        tags = listOf("pacman", "arcade", "maze", "classic"),
        features = listOf(GameFeature.HIGH_SCORE, GameFeature.SAVE_LOAD, GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 48,
            recommendedRamMb = 96,
            minStorageMb = 12,
            recommendedStorageMb = 25
        ),
        statistics = GameStatistics(
            downloadCount = 600000,
            rating = 4.3f,
            ratingCount = 28000,
            playCount = 2800000,
            averagePlayTime = 12,
            completionRate = 0.25f
        ),
        attribution = AttributionInfo(
            displayText = "Pacman Clone",
            licenseText = "GPL v3.0",
            sourceUrl = "https://github.com/pacman/pacman",
            attributionRequired = true
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 1, 18, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2015, 8, 1, 0, 0)
    ))
}

// Strategy Games
fun OpenSourceGameCatalog.addStrategyGames() {
    // Tower Defense
    addGame(GameMetadata(
        id = "tower-defense",
        name = "OpenTD",
        displayName = "Tower Defense",
        description = "Defend your base against waves of enemies",
        longDescription = "Strategic tower defense game where you place towers to defend against enemy waves.",
        version = "1.2.0",
        category = GameCategory.STRATEGY,
        subcategory = "Tower Defense",
        difficulty = GameDifficulty.HARD,
        ageRating = AgeRating.EVERYONE_10,
        license = OpenSourceLicense(
            type = LicenseType.APACHE_2,
            name = "Apache License 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0",
            isCompatible = true,
            requiresAttribution = false,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = false
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/towerdefense/towerdefense",
            repositoryType = RepositoryType.GITHUB,
            branch = "main",
            stars = 350,
            forks = 140,
            issues = 20
        ),
        author = AuthorInfo(
            name = "Tower Defense Community"
        ),
        tags = listOf("tower-defense", "strategy", "defense", "towers"),
        features = listOf(GameFeature.SAVE_LOAD, GameFeature.HIGH_SCORE, GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 64,
            recommendedRamMb = 128,
            minStorageMb = 20,
            recommendedStorageMb = 40
        ),
        statistics = GameStatistics(
            downloadCount = 250000,
            rating = 4.1f,
            ratingCount = 12000,
            playCount = 800000,
            averagePlayTime = 45,
            completionRate = 0.15f
        ),
        attribution = AttributionInfo(
            displayText = "Open Tower Defense",
            licenseText = "Apache 2.0",
            sourceUrl = "https://github.com/towerdefense/towerdefense",
            attributionRequired = false
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 2, 8, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2017, 12, 1, 0, 0)
    ))
}

// Trivia Games
fun OpenSourceGameCatalog.addTriviaGames() {
    // Quiz Game
    addGame(GameMetadata(
        id = "quiz-game",
        name = "OpenQuiz",
        displayName = "Quiz Game",
        description = "Test your knowledge with thousands of questions",
        longDescription = "Comprehensive quiz game with multiple categories and difficulty levels.",
        version = "2.5.0",
        category = GameCategory.TRIVIA,
        subcategory = "Knowledge Quiz",
        difficulty = GameDifficulty.MEDIUM,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.GPL_V3,
            name = "GNU General Public License v3.0",
            url = "https://www.gnu.org/licenses/gpl-3.0.html",
            isCompatible = true,
            requiresAttribution = true,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = true
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/quizgame/quizgame",
            repositoryType = RepositoryType.GITHUB,
            branch = "master",
            stars = 280,
            forks = 110,
            issues = 15
        ),
        author = AuthorInfo(
            name = "Quiz Game Community"
        ),
        tags = listOf("quiz", "trivia", "knowledge", "education"),
        features = listOf(GameFeature.HIGH_SCORE, GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 48,
            recommendedRamMb = 96,
            minStorageMb = 30,
            recommendedStorageMb = 60
        ),
        statistics = GameStatistics(
            downloadCount = 400000,
            rating = 4.2f,
            ratingCount = 20000,
            playCount = 1800000,
            averagePlayTime = 18,
            completionRate = 0.35f
        ),
        attribution = AttributionInfo(
            displayText = "OpenQuiz - Knowledge Game",
            licenseText = "GPL v3.0",
            sourceUrl = "https://github.com/quizgame/quizgame",
            attributionRequired = true
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 1, 22, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2016, 9, 1, 0, 0)
    ))
}

// Action Games
fun OpenSourceGameCatalog.addActionGames() {
    // Platformer
    addGame(GameMetadata(
        id = "platformer",
        name = "SuperTux",
        displayName = "Platform Adventure",
        description = "Jump and run through levels collecting coins",
        longDescription = "SuperTux is a classic 2D platform game featuring Tux the penguin in a Super Mario-like adventure.",
        version = "0.6.0",
        category = GameCategory.ACTION,
        subcategory = "Platformer",
        difficulty = GameDifficulty.MEDIUM,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.GPL_V3,
            name = "GNU General Public License v3.0",
            url = "https://www.gnu.org/licenses/gpl-3.0.html",
            isCompatible = true,
            requiresAttribution = true,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = true
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/SuperTux/supertux",
            repositoryType = RepositoryType.GITHUB,
            branch = "master",
            stars = 1200,
            forks = 400,
            issues = 85
        ),
        author = AuthorInfo(
            name = "SuperTux Team"
        ),
        tags = listOf("platformer", "adventure", "jumping", "mario-like"),
        features = listOf(GameFeature.SAVE_LOAD, GameFeature.HIGH_SCORE, GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 64,
            recommendedRamMb = 128,
            minStorageMb = 40,
            recommendedStorageMb = 80
        ),
        statistics = GameStatistics(
            downloadCount = 500000,
            rating = 4.4f,
            ratingCount = 25000,
            playCount = 2000000,
            averagePlayTime = 35,
            completionRate = 0.2f
        ),
        attribution = AttributionInfo(
            displayText = "SuperTux - Open Source Platformer",
            licenseText = "GPL v3.0",
            sourceUrl = "https://github.com/SuperTux/supertux",
            attributionRequired = true
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 2, 12, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2003, 4, 1, 0, 0)
    ))
}

// Board Games
fun OpenSourceGameCatalog.addBoardGames() {
    // Tic-tac-toe
    addGame(GameMetadata(
        id = "tic-tac-toe",
        name = "TicTacToe",
        displayName = "Tic-Tac-Toe",
        description = "Classic 3x3 grid game for two players",
        longDescription = "The classic tic-tac-toe game with multiple difficulty levels and game modes.",
        version = "1.0.0",
        category = GameCategory.BOARD,
        subcategory = "Grid Game",
        difficulty = GameDifficulty.VERY_EASY,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.MIT,
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT",
            isCompatible = true,
            requiresAttribution = false,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = false
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/tictactoe/tictactoe",
            repositoryType = RepositoryType.GITHUB,
            branch = "main",
            stars = 120,
            forks = 60,
            issues = 8
        ),
        author = AuthorInfo(
            name = "TicTacToe Community"
        ),
        tags = listOf("tic-tac-toe", "board", "classic", "two-player"),
        features = listOf(GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 16,
            recommendedRamMb = 32,
            minStorageMb = 2,
            recommendedStorageMb = 5
        ),
        statistics = GameStatistics(
            downloadCount = 300000,
            rating = 4.0f,
            ratingCount = 15000,
            playCount = 1200000,
            averagePlayTime = 5,
            completionRate = 0.8f
        ),
        attribution = AttributionInfo(
            displayText = "Classic Tic-Tac-Toe",
            licenseText = "MIT License",
            sourceUrl = "https://github.com/tictactoe/tictactoe",
            attributionRequired = false
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 1, 10, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2019, 1, 1, 0, 0)
    ))
}

// Casual Games
fun OpenSourceGameCatalog.addCasualGames() {
    // Match-3
    addGame(GameMetadata(
        id = "match3",
        name = "Match3Game",
        displayName = "Match 3 Puzzle",
        description = "Swap gems to create matches of three or more",
        longDescription = "Addictive match-3 puzzle game with colorful gems and special effects.",
        version = "1.8.0",
        category = GameCategory.CASUAL,
        subcategory = "Match 3",
        difficulty = GameDifficulty.EASY,
        ageRating = AgeRating.EVERYONE,
        license = OpenSourceLicense(
            type = LicenseType.APACHE_2,
            name = "Apache License 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0",
            isCompatible = true,
            requiresAttribution = false,
            allowsCommercial = true,
            allowsModification = true,
            shareAlike = false
        ),
        sourceCode = SourceCodeInfo(
            repositoryUrl = "https://github.com/match3/match3",
            repositoryType = RepositoryType.GITHUB,
            branch = "main",
            stars = 220,
            forks = 95,
            issues = 15
        ),
        author = AuthorInfo(
            name = "Match3 Community"
        ),
        tags = listOf("match3", "casual", "puzzle", "gems"),
        features = listOf(GameFeature.SAVE_LOAD, GameFeature.HIGH_SCORE, GameFeature.SETTINGS),
        requirements = GameRequirements(
            minAndroidVersion = 16,
            recommendedAndroidVersion = 21,
            minRamMb = 48,
            recommendedRamMb = 96,
            minStorageMb = 15,
            recommendedStorageMb = 30
        ),
        statistics = GameStatistics(
            downloadCount = 600000,
            rating = 4.3f,
            ratingCount = 30000,
            playCount = 4000000,
            averagePlayTime = 12,
            completionRate = 0.25f
        ),
        attribution = AttributionInfo(
            displayText = "Open Match 3 Game",
            licenseText = "Apache 2.0",
            sourceUrl = "https://github.com/match3/match3",
            attributionRequired = false
        ),
        lastUpdated = kotlinx.datetime.LocalDateTime(2024, 2, 20, 0, 0),
        createdDate = kotlinx.datetime.LocalDateTime(2017, 6, 1, 0, 0)
    ))
}