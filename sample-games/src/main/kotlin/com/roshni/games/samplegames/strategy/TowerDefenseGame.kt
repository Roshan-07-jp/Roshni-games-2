package com.roshni.games.samplegames.strategy

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.gametemplates.strategy.StrategyGameTemplate
import com.roshni.games.gametemplates.strategy.StrategyGameViewModel
import com.roshni.games.gametemplates.strategy.Building
import com.roshni.games.gametemplates.strategy.BuildingType
import com.roshni.games.gametemplates.strategy.GameUnit
import com.roshni.games.gametemplates.strategy.ResourceType
import com.roshni.games.gametemplates.strategy.UnitType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

/**
 * Fully playable Tower Defense game implementation
 */
class TowerDefenseGame(
    context: Context,
    gameView: View
) : StrategyGameTemplate(context, "tower-defense", gameView) {

    private val _gamePath = MutableStateFlow<List<PathPoint>>(emptyList())
    val gamePath: StateFlow<List<PathPoint>> = _gamePath.asStateFlow()

    private val _waves = MutableStateFlow<List<EnemyWave>>(emptyList())
    val waves: StateFlow<List<EnemyWave>> = _waves.asStateFlow()

    private val _currentWave = MutableStateFlow(0)
    val currentWave: StateFlow<Int> = _currentWave.asStateFlow()

    private val _enemies = MutableStateFlow<List<Enemy>>(emptyList())
    val enemies: StateFlow<List<Enemy>> = _enemies.asStateFlow()

    private val _projectiles = MutableStateFlow<List<Projectile>>(emptyList())
    val projectiles: StateFlow<List<Projectile>> = _projectiles.asStateFlow()

    override fun registerGameSystems() {
        super.registerGameSystems()
        // Tower Defense can use physics for projectiles
        if (strategyConfig.enableRealTimePhysics) {
            gameEngine.registerSystem(physicsSystem)
        }
    }

    override fun initializeGameState() {
        super.initializeGameState()
        _gamePath.value = createGamePath()
        _waves.value = generateWaves()
        _currentWave.value = 0
        _enemies.value = emptyList()
        _projectiles.value = emptyList()
    }

    override fun loadGameAssets() {
        super.loadGameAssets()
        // Load Tower Defense specific assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "towerdefense/tower_basic.png",
                    "towerdefense/tower_advanced.png",
                    "towerdefense/enemy_basic.png",
                    "towerdefense/projectile.png",
                    "towerdefense/explosion.png"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load Tower Defense assets")
            }
        }
    }

    override fun setupInputHandling() {
        super.setupInputHandling()
        // Tower Defense specific input handling is implemented in onStrategyTouch
    }

    override fun setupAudio() {
        super.setupAudio()
        // Tower Defense specific audio setup
    }

    override fun initializeGame() {
        super.initializeGame()
        startNextWave()
    }

    override fun updateGame(deltaTime: Float) {
        super.updateGame(deltaTime)

        // Update enemies
        updateEnemies(deltaTime)

        // Update projectiles
        updateProjectiles(deltaTime)

        // Check collisions
        checkCollisions()

        // Spawn enemies for current wave
        spawnEnemiesFromWave(deltaTime)
    }

    override fun updateGameProgress() {
        super.updateGameProgress()

        // Check if current wave is complete
        if (_enemies.value.isEmpty() && _currentWave.value < _waves.value.size) {
            val currentWaveData = _waves.value[_currentWave.value]
            if (currentWaveData.spawnedCount >= currentWaveData.enemyCount) {
                startNextWave()
            }
        }
    }

    override fun initializeResources(): Map<ResourceType, Int> {
        return mapOf(
            ResourceType.GOLD to 100,
            ResourceType.WOOD to 50
        )
    }

    override fun setupGameBoard() {
        // Tower Defense board is the path and tower placement areas
        Timber.d("Tower Defense game board setup complete")
    }

    override fun initializePlayerUnits() {
        // Player starts with base
        addBuilding(Building(
            id = "base",
            type = BuildingType.BASE,
            playerId = "player",
            x = 0f,
            y = 200f,
            width = 80f,
            height = 80f,
            health = 100f,
            maxHealth = 100f
        ))
    }

    override fun startStrategyGame() {
        Timber.d("Starting Tower Defense game")
    }

    override fun updateRealTime(deltaTime: Float) {
        // Real-time strategy updates for Tower Defense
    }

    override fun updateTurnBased(deltaTime: Float) {
        // Tower Defense is typically real-time
    }

    override fun updateStrategyGame(deltaTime: Float) {
        // Main strategy game updates
    }

    override fun onStrategyTouch(x: Float, y: Float) {
        // Handle tower placement or selection
        attemptTowerPlacement(x, y)
    }

    override fun onStrategyDrag(x: Float, y: Float) {
        // Handle dragging for tower placement preview
    }

    override fun processNextTurn() {
        // Tower Defense doesn't use traditional turns
    }

    override fun getGameViewModel(): StrategyGameViewModel {
        return TowerDefenseGameViewModel()
    }

    /**
     * Create the path that enemies will follow
     */
    private fun createGamePath(): List<PathPoint> {
        return listOf(
            PathPoint(0f, 100f),
            PathPoint(150f, 100f),
            PathPoint(150f, 200f),
            PathPoint(300f, 200f),
            PathPoint(300f, 300f),
            PathPoint(450f, 300f),
            PathPoint(450f, 200f),
            PathPoint(600f, 200f),
            PathPoint(600f, 100f),
            PathPoint(750f, 100f)
        )
    }

    /**
     * Generate enemy waves
     */
    private fun generateWaves(): List<EnemyWave> {
        return listOf(
            EnemyWave(0, 10, 2.0f, EnemyType.BASIC),
            EnemyWave(1, 15, 1.5f, EnemyType.BASIC),
            EnemyWave(2, 5, 1.0f, EnemyType.ARMORED),
            EnemyWave(3, 20, 1.2f, EnemyType.BASIC),
            EnemyWave(4, 8, 0.8f, EnemyType.FAST),
            EnemyWave(5, 3, 3.0f, EnemyType.BOSS)
        )
    }

    /**
     * Start next wave
     */
    private fun startNextWave() {
        if (_currentWave.value < _waves.value.size) {
            val wave = _waves.value[_currentWave.value]
            _currentWave.value++

            Timber.d("Starting wave ${_currentWave.value} with ${wave.enemyCount} enemies")

            // Reward player for completing previous wave
            if (_currentWave.value > 1) {
                addResource(ResourceType.GOLD, 50)
            }
        } else {
            // Game completed
            onGameOver()
        }
    }

    /**
     * Spawn enemies from current wave
     */
    private fun spawnEnemiesFromWave(deltaTime: Float) {
        if (_currentWave.value >= _waves.value.size) return

        val wave = _waves.value[_currentWave.value]
        val waveData = _waves.value.toMutableList()
        val updatedWave = wave.copy(spawnedCount = wave.spawnedCount + 1)
        waveData[_currentWave.value] = updatedWave
        _waves.value = waveData

        // Create enemy
        val enemy = Enemy(
            id = "enemy_${System.currentTimeMillis()}",
            type = wave.enemyType,
            x = _gamePath.value.first().x,
            y = _gamePath.value.first().y,
            pathProgress = 0f,
            health = getEnemyHealth(wave.enemyType),
            speed = getEnemySpeed(wave.enemyType),
            reward = getEnemyReward(wave.enemyType)
        )

        _enemies.value = _enemies.value + enemy
    }

    /**
     * Update enemies
     */
    private fun updateEnemies(deltaTime: Float) {
        val updatedEnemies = _enemies.value.mapNotNull { enemy ->
            val updatedEnemy = updateEnemyPosition(enemy, deltaTime)
            if (updatedEnemy != null && updatedEnemy.health > 0) {
                updatedEnemy
            } else {
                // Enemy reached the end or died
                if (updatedEnemy == null) {
                    // Enemy reached the end - player loses health
                    removeLives(1)
                } else {
                    // Enemy died - player gets reward
                    addResource(ResourceType.GOLD, updatedEnemy.reward)
                }
                null
            }
        }

        _enemies.value = updatedEnemies
    }

    /**
     * Update enemy position along path
     */
    private fun updateEnemyPosition(enemy: Enemy, deltaTime: Float): Enemy? {
        val pathLength = _gamePath.value.size
        val newProgress = enemy.pathProgress + (enemy.speed * deltaTime / 100f)

        if (newProgress >= pathLength - 1) {
            // Enemy reached the end
            return null
        }

        val currentIndex = enemy.pathProgress.toInt()
        val nextIndex = (enemy.pathProgress + 1f).toInt().coerceAtMost(pathLength - 1)

        val currentPoint = _gamePath.value[currentIndex]
        val nextPoint = _gamePath.value[nextIndex]

        val t = enemy.pathProgress - currentIndex
        val x = currentPoint.x + (nextPoint.x - currentPoint.x) * t
        val y = currentPoint.y + (nextPoint.y - currentPoint.y) * t

        return enemy.copy(
            x = x,
            y = y,
            pathProgress = newProgress
        )
    }

    /**
     * Update projectiles
     */
    private fun updateProjectiles(deltaTime: Float) {
        val updatedProjectiles = _projectiles.value.mapNotNull { projectile ->
            val updatedProjectile = updateProjectilePosition(projectile, deltaTime)
            if (updatedProjectile != null) {
                updatedProjectile
            } else {
                null // Remove projectile if it reached target or expired
            }
        }

        _projectiles.value = updatedProjectiles
    }

    /**
     * Update projectile position
     */
    private fun updateProjectilePosition(projectile: Projectile, deltaTime: Float): Projectile? {
        val target = _enemies.value.find { it.id == projectile.targetId }
        if (target == null) return null // Target destroyed

        val dx = target.x - projectile.x
        val dy = target.y - projectile.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance < 10f) {
            // Hit target
            damageEnemy(projectile.targetId, projectile.damage)
            return null
        }

        // Move towards target
        val speed = 200f // pixels per second
        val moveDistance = speed * deltaTime / 1000f

        val normalizedDx = dx / distance
        val normalizedDy = dy / distance

        return projectile.copy(
            x = projectile.x + normalizedDx * moveDistance,
            y = projectile.y + normalizedDy * moveDistance
        )
    }

    /**
     * Check collisions between projectiles and enemies
     */
    private fun checkCollisions() {
        // This is handled in updateProjectiles when projectiles reach targets
    }

    /**
     * Attempt to place a tower
     */
    private fun attemptTowerPlacement(x: Float, y: Float) {
        val towerCost = mapOf(ResourceType.GOLD to 50, ResourceType.WOOD to 20)

        if (canAfford(towerCost)) {
            spendResources(towerCost)

            val tower = Building(
                id = "tower_${System.currentTimeMillis()}",
                type = BuildingType.BARRACKS, // Using barracks as tower for now
                playerId = "player",
                x = x,
                y = y,
                width = 60f,
                height = 60f,
                cost = towerCost
            )

            addBuilding(tower)
            Timber.d("Placed tower at ($x, $y)")
        }
    }

    /**
     * Damage enemy
     */
    private fun damageEnemy(enemyId: String, damage: Float) {
        val updatedEnemies = _enemies.value.map { enemy ->
            if (enemy.id == enemyId) {
                enemy.copy(health = (enemy.health - damage).coerceAtLeast(0f))
            } else {
                enemy
            }
        }

        _enemies.value = updatedEnemies
    }

    /**
     * Get enemy health based on type
     */
    private fun getEnemyHealth(type: EnemyType): Float {
        return when (type) {
            EnemyType.BASIC -> 100f
            EnemyType.ARMORED -> 200f
            EnemyType.FAST -> 50f
            EnemyType.BOSS -> 1000f
        }
    }

    /**
     * Get enemy speed based on type
     */
    private fun getEnemySpeed(type: EnemyType): Float {
        return when (type) {
            EnemyType.BASIC -> 50f
            EnemyType.ARMORED -> 30f
            EnemyType.FAST -> 80f
            EnemyType.BOSS -> 20f
        }
    }

    /**
     * Get enemy reward based on type
     */
    private fun getEnemyReward(type: EnemyType): Int {
        return when (type) {
            EnemyType.BASIC -> 10
            EnemyType.ARMORED -> 25
            EnemyType.FAST -> 15
            EnemyType.BOSS -> 100
        }
    }

    @Composable
    override fun StrategyGameHeader(
        resources: Map<ResourceType, Int>,
        turnCount: Int,
        isPlayerTurn: Boolean,
        onEndTurn: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Resources
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                resources.forEach { (type, amount) ->
                    ResourceDisplay(
                        type = type,
                        amount = amount
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wave info
            Text(
                text = "Wave: ${_currentWave.value}/${_waves.value.size}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            // Lives and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lives: ${_lives.collectAsState().value}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Score: ${_score.collectAsState().value}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    @Composable
    override fun StrategyGameContent(
        units: List<GameUnit>,
        buildings: List<Building>,
        isPlayerTurn: Boolean
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Green.copy(alpha = 0.3f))
        ) {
            // Draw game path
            GamePath(path = _gamePath.collectAsState().value)

            // Draw buildings (towers)
            buildings.forEach { building ->
                Tower(building = building)
            }

            // Draw enemies
            _enemies.collectAsState().value.forEach { enemy ->
                EnemySprite(enemy = enemy)
            }

            // Draw projectiles
            _projectiles.collectAsState().value.forEach { projectile ->
                ProjectileSprite(projectile = projectile)
            }
        }
    }

    @Composable
    override fun TurnIndicator(
        isPlayerTurn: Boolean,
        turnCount: Int,
        modifier: Modifier
    ) {
        // Tower Defense doesn't use traditional turns
    }

    @Composable
    private fun ResourceDisplay(type: ResourceType, amount: Int) {
        Card(
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = "${type.icon} $amount",
                modifier = Modifier.padding(8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun GamePath(path: List<PathPoint>) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until path.size - 1) {
                val start = path[i]
                val end = path[i + 1]

                drawLine(
                    color = Color.Brown,
                    start = androidx.compose.ui.geometry.Offset(start.x, start.y),
                    end = androidx.compose.ui.geometry.Offset(end.x, end.y),
                    strokeWidth = 20f
                )
            }
        }
    }

    @Composable
    private fun Tower(building: Building) {
        Card(
            modifier = Modifier
                .offset(building.x.dp, building.y.dp)
                .size(building.width.dp, building.height.dp)
                .clickable {
                    // Select tower for upgrades
                },
            colors = CardDefaults.cardColors(
                containerColor = Color.Blue.copy(alpha = 0.8f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🗼",
                    fontSize = 24.sp
                )
            }
        }
    }

    @Composable
    private fun EnemySprite(enemy: Enemy) {
        Card(
            modifier = Modifier
                .offset(enemy.x.dp, enemy.y.dp)
                .size(30.dp, 30.dp),
            colors = CardDefaults.cardColors(
                containerColor = getEnemyColor(enemy.type)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = getEnemyEmoji(enemy.type),
                    fontSize = 16.sp
                )
            }
        }
    }

    @Composable
    private fun ProjectileSprite(projectile: Projectile) {
        Box(
            modifier = Modifier
                .offset(projectile.x.dp, projectile.y.dp)
                .size(10.dp)
                .background(Color.Yellow)
        )
    }

    private fun getEnemyColor(type: EnemyType): Color {
        return when (type) {
            EnemyType.BASIC -> Color.Red
            EnemyType.ARMORED -> Color.Gray
            EnemyType.FAST -> Color.Orange
            EnemyType.BOSS -> Color.Magenta
        }
    }

    private fun getEnemyEmoji(type: EnemyType): String {
        return when (type) {
            EnemyType.BASIC -> "👹"
            EnemyType.ARMORED -> "🤖"
            EnemyType.FAST -> "💨"
            EnemyType.BOSS -> "👑"
        }
    }
}

/**
 * Tower Defense game view model
 */
class TowerDefenseGameViewModel : StrategyGameViewModel() {

    private val _gamePath = MutableStateFlow<List<PathPoint>>(emptyList())
    val gamePath: StateFlow<List<PathPoint>> = _gamePath.asStateFlow()

    private val _currentWave = MutableStateFlow(0)
    val currentWave: StateFlow<Int> = _currentWave.asStateFlow()

    fun setGamePath(path: List<PathPoint>) {
        _gamePath.value = path
    }

    fun setCurrentWave(wave: Int) {
        _currentWave.value = wave
    }
}

/**
 * Path point for enemy movement
 */
data class PathPoint(
    val x: Float,
    val y: Float
)

/**
 * Enemy wave configuration
 */
data class EnemyWave(
    val waveNumber: Int,
    val enemyCount: Int,
    val spawnInterval: Float,
    val enemyType: EnemyType,
    var spawnedCount: Int = 0
)

/**
 * Enemy types
 */
enum class EnemyType {
    BASIC, ARMORED, FAST, BOSS
}

/**
 * Enemy data class
 */
data class Enemy(
    val id: String,
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val pathProgress: Float,
    val health: Float,
    val speed: Float,
    val reward: Int
)

/**
 * Projectile data class
 */
data class Projectile(
    val id: String,
    val x: Float,
    val y: Float,
    val targetId: String,
    val damage: Float,
    val speed: Float = 200f
)