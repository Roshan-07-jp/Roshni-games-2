package com.roshni.games.gameengine.systems

import com.roshni.games.gameengine.core.GameSystem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import kotlin.math.*

/**
 * Physics system for handling basic physics calculations
 */
class PhysicsSystem : GameSystem() {

    private val _collisionEvents = MutableSharedFlow<CollisionEvent>(extraBufferCapacity = 50)
    val collisionEvents: SharedFlow<CollisionEvent> = _collisionEvents.asSharedFlow()

    // Physics constants
    private val gravity = 9.81f // m/s²
    private val airResistance = 0.99f // Air resistance coefficient

    // Physics objects
    private val physicsObjects = mutableMapOf<String, PhysicsObject>()
    private val collisionPairs = mutableSetOf<Pair<String, String>>()

    override fun update(deltaTime: Float) {
        val deltaSeconds = deltaTime / 1000f // Convert to seconds

        // Update all physics objects
        physicsObjects.values.forEach { obj ->
            updatePhysicsObject(obj, deltaSeconds)
        }

        // Check collisions
        checkCollisions()
    }

    override fun cleanup() {
        physicsObjects.clear()
        collisionPairs.clear()
    }

    /**
     * Add a physics object to the system
     */
    fun addPhysicsObject(id: String, obj: PhysicsObject) {
        physicsObjects[id] = obj
        Timber.d("Added physics object: $id")
    }

    /**
     * Remove a physics object from the system
     */
    fun removePhysicsObject(id: String) {
        physicsObjects.remove(id)
        // Remove from collision pairs
        collisionPairs.removeAll { it.first == id || it.second == id }
        Timber.d("Removed physics object: $id")
    }

    /**
     * Get a physics object by ID
     */
    fun getPhysicsObject(id: String): PhysicsObject? = physicsObjects[id]

    /**
     * Update physics object state
     */
    private fun updatePhysicsObject(obj: PhysicsObject, deltaTime: Float) {
        // Apply gravity if enabled
        if (obj.useGravity) {
            obj.velocityY += gravity * deltaTime
        }

        // Apply air resistance
        obj.velocityX *= airResistance
        obj.velocityY *= airResistance

        // Update position
        obj.x += obj.velocityX * deltaTime
        obj.y += obj.velocityY * deltaTime

        // Update rotation
        obj.rotation += obj.angularVelocity * deltaTime

        // Boundary checks
        handleBoundaryCollisions(obj)

        // Apply friction if on ground
        if (obj.isOnGround && abs(obj.velocityX) > 0.1f) {
            obj.velocityX *= obj.friction
        }
    }

    /**
     * Handle boundary collisions
     */
    private fun handleBoundaryCollisions(obj: PhysicsObject) {
        val margin = 0.1f // Small margin to prevent sticking

        // Left boundary
        if (obj.x < obj.radius - margin) {
            obj.x = obj.radius - margin
            obj.velocityX = abs(obj.velocityX) * obj.bounciness
        }

        // Right boundary
        if (obj.x > obj.worldWidth - obj.radius + margin) {
            obj.x = obj.worldWidth - obj.radius + margin
            obj.velocityX = -abs(obj.velocityX) * obj.bounciness
        }

        // Top boundary
        if (obj.y < obj.radius - margin) {
            obj.y = obj.radius - margin
            obj.velocityY = abs(obj.velocityY) * obj.bounciness
        }

        // Bottom boundary (ground)
        if (obj.y > obj.worldHeight - obj.radius + margin) {
            obj.y = obj.worldHeight - obj.radius + margin
            obj.velocityY = -abs(obj.velocityY) * obj.bounciness
            obj.isOnGround = true
        } else {
            obj.isOnGround = false
        }
    }

    /**
     * Check for collisions between physics objects
     */
    private fun checkCollisions() {
        val objects = physicsObjects.values.toList()

        for (i in objects.indices) {
            for (j in i + 1 until objects.size) {
                val obj1 = objects[i]
                val obj2 = objects[j]

                // Create collision pair key
                val pair = if (obj1.id < obj2.id) {
                    obj1.id to obj2.id
                } else {
                    obj2.id to obj1.id
                }

                // Skip if already processed this frame
                if (collisionPairs.contains(pair)) continue

                if (checkCollision(obj1, obj2)) {
                    handleCollision(obj1, obj2)
                    collisionPairs.add(pair)
                }
            }
        }

        // Clear collision pairs for next frame
        collisionPairs.clear()
    }

    /**
     * Check collision between two physics objects
     */
    private fun checkCollision(obj1: PhysicsObject, obj2: PhysicsObject): Boolean {
        val distance = distance(obj1.x, obj1.y, obj2.x, obj2.y)
        return distance < obj1.radius + obj2.radius
    }

    /**
     * Handle collision between two physics objects
     */
    private fun handleCollision(obj1: PhysicsObject, obj2: PhysicsObject) {
        // Calculate collision normal
        val dx = obj2.x - obj1.x
        val dy = obj2.y - obj1.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance == 0f) return // Avoid division by zero

        val nx = dx / distance
        val ny = dy / distance

        // Relative velocity
        val relativeVelocityX = obj2.velocityX - obj1.velocityX
        val relativeVelocityY = obj2.velocityY - obj1.velocityY

        // Relative velocity along collision normal
        val velocityAlongNormal = relativeVelocityX * nx + relativeVelocityY * ny

        // Don't resolve if velocities are separating
        if (velocityAlongNormal > 0) return

        // Calculate restitution (bounciness)
        val restitution = min(obj1.bounciness, obj2.bounciness)

        // Calculate impulse scalar
        val impulseScalar = -(1 + restitution) * velocityAlongNormal
        impulseScalar / (1 / obj1.mass + 1 / obj2.mass)

        // Apply impulse
        val impulseX = impulseScalar * nx
        val impulseY = impulseScalar * ny

        obj1.velocityX -= (impulseX / obj1.mass)
        obj1.velocityY -= (impulseY / obj1.mass)
        obj2.velocityX += (impulseX / obj2.mass)
        obj2.velocityY += (impulseY / obj2.mass)

        // Separate objects to avoid overlap
        val overlap = (obj1.radius + obj2.radius) - distance
        if (overlap > 0) {
            val separationX = nx * overlap * 0.5f
            val separationY = ny * overlap * 0.5f

            obj1.x -= separationX
            obj1.y -= separationY
            obj2.x += separationX
            obj2.y += separationY
        }

        // Emit collision event
        kotlinx.coroutines.GlobalScope.launch {
            _collisionEvents.emit(CollisionEvent(obj1.id, obj2.id, nx, ny))
        }

        Timber.d("Collision detected between ${obj1.id} and ${obj2.id}")
    }

    /**
     * Apply force to a physics object
     */
    fun applyForce(objectId: String, forceX: Float, forceY: Float) {
        physicsObjects[objectId]?.let { obj ->
            obj.velocityX += forceX / obj.mass
            obj.velocityY += forceY / obj.mass
        }
    }

    /**
     * Set velocity of a physics object
     */
    fun setVelocity(objectId: String, velocityX: Float, velocityY: Float) {
        physicsObjects[objectId]?.let { obj ->
            obj.velocityX = velocityX
            obj.velocityY = velocityY
        }
    }

    /**
     * Set position of a physics object
     */
    fun setPosition(objectId: String, x: Float, y: Float) {
        physicsObjects[objectId]?.let { obj ->
            obj.x = x
            obj.y = y
        }
    }

    /**
     * Calculate distance between two points
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Ray casting for collision detection
     */
    fun rayCast(startX: Float, startY: Float, endX: Float, endY: Float): List<RayCastHit> {
        val hits = mutableListOf<RayCastHit>()
        val directionX = endX - startX
        val directionY = endY - startY
        val length = sqrt(directionX * directionX + directionY * directionY)

        if (length == 0f) return hits

        val normalizedX = directionX / length
        val normalizedY = directionY / length

        physicsObjects.values.forEach { obj ->
            // Simple circle-ray intersection
            val toCenterX = obj.x - startX
            val toCenterY = obj.y - startY
            val dot = toCenterX * normalizedX + toCenterY * normalizedY

            if (dot > 0) {
                val distanceToLine = abs(toCenterX * normalizedY - toCenterY * normalizedX)
                if (distanceToLine <= obj.radius) {
                    val hitPoint = dot - sqrt(obj.radius * obj.radius - distanceToLine * distanceToLine)
                    if (hitPoint >= 0 && hitPoint <= length) {
                        hits.add(RayCastHit(obj.id, hitPoint, obj.x, obj.y))
                    }
                }
            }
        }

        return hits.sortedBy { it.distance }
    }
}

/**
 * Physics object data class
 */
data class PhysicsObject(
    val id: String,
    var x: Float,
    var y: Float,
    val radius: Float,
    val mass: Float = 1.0f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var angularVelocity: Float = 0f,
    var rotation: Float = 0f,
    val bounciness: Float = 0.8f,
    val friction: Float = 0.9f,
    val useGravity: Boolean = true,
    val worldWidth: Float = 1920f,
    val worldHeight: Float = 1080f,
    var isOnGround: Boolean = false
)

/**
 * Collision event data class
 */
data class CollisionEvent(
    val objectId1: String,
    val objectId2: String,
    val normalX: Float,
    val normalY: Float
)

/**
 * Ray cast hit data class
 */
data class RayCastHit(
    val objectId: String,
    val distance: Float,
    val hitX: Float,
    val hitY: Float
)