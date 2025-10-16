package com.roshni.games.gameengine.systems

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.roshni.games.gameengine.core.GameSystem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import kotlin.math.abs

/**
 * Input system for handling touch, gestures, and hardware input
 */
class InputSystem(
    private val context: Context,
    private val gameView: View
) : GameSystem() {

    private val _touchEvents = MutableSharedFlow<TouchEvent>(extraBufferCapacity = 100)
    val touchEvents: SharedFlow<TouchEvent> = _touchEvents.asSharedFlow()

    private val _gestureEvents = MutableSharedFlow<GestureEvent>(extraBufferCapacity = 50)
    val gestureEvents: SharedFlow<GestureEvent> = _gestureEvents.asSharedFlow()

    private val _keyEvents = MutableSharedFlow<KeyEvent>(extraBufferCapacity = 20)
    val keyEvents: SharedFlow<KeyEvent> = _keyEvents.asSharedFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    private var touchListener: View.OnTouchListener? = null
    private var keyListener: View.OnKeyListener? = null

    override fun initialize() {
        Timber.d("Initializing input system")

        // Setup gesture detector
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                mainHandler.post {
                    _gestureEvents.tryEmit(GestureEvent.SingleTap(e.x, e.y))
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                mainHandler.post {
                    _gestureEvents.tryEmit(GestureEvent.LongPress(e.x, e.y))
                }
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                mainHandler.post {
                    _gestureEvents.tryEmit(GestureEvent.Scroll(e2.x, e2.y, distanceX, distanceY))
                }
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                mainHandler.post {
                    _gestureEvents.tryEmit(GestureEvent.Fling(e2.x, e2.y, velocityX, velocityY))
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                mainHandler.post {
                    _gestureEvents.tryEmit(GestureEvent.DoubleTap(e.x, e.y))
                }
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })

        // Setup touch listener
        touchListener = View.OnTouchListener { _, event ->
            handleTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        // Setup key listener
        keyListener = View.OnKeyListener { _, keyCode, event ->
            handleKeyEvent(keyCode, event)
            true
        }

        // Attach listeners to game view
        gameView.setOnTouchListener(touchListener)
        gameView.setOnKeyListener(keyListener)
        gameView.isFocusable = true
        gameView.isFocusableInTouchMode = true

        Timber.d("Input system initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up input system")

        // Remove listeners
        gameView.setOnTouchListener(null)
        gameView.setOnKeyListener(null)

        touchListener = null
        keyListener = null
    }

    private fun handleTouchEvent(event: MotionEvent) {
        val touchEvent = when (event.action) {
            MotionEvent.ACTION_DOWN -> TouchEvent.Down(event.x, event.y, event.pointerCount)
            MotionEvent.ACTION_MOVE -> TouchEvent.Move(event.x, event.y, event.pointerCount)
            MotionEvent.ACTION_UP -> TouchEvent.Up(event.x, event.y, event.pointerCount)
            MotionEvent.ACTION_CANCEL -> TouchEvent.Cancel(event.x, event.y, event.pointerCount)
            else -> return
        }

        mainHandler.post {
            _touchEvents.tryEmit(touchEvent)
        }
    }

    private fun handleKeyEvent(keyCode: Int, event: android.view.KeyEvent) {
        val keyEvent = when (event.action) {
            android.view.KeyEvent.ACTION_DOWN -> KeyEvent.Down(keyCode)
            android.view.KeyEvent.ACTION_UP -> KeyEvent.Up(keyCode)
            else -> return
        }

        mainHandler.post {
            _keyEvents.tryEmit(keyEvent)
        }
    }

    /**
     * Check if a point is within a rectangular area
     */
    fun isPointInRect(x: Float, y: Float, rectX: Float, rectY: Float, rectWidth: Float, rectHeight: Float): Boolean {
        return x >= rectX && x <= rectX + rectWidth && y >= rectY && y <= rectY + rectHeight
    }

    /**
     * Check if a point is within a circular area
     */
    fun isPointInCircle(x: Float, y: Float, centerX: Float, centerY: Float, radius: Float): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        return dx * dx + dy * dy <= radius * radius
    }

    /**
     * Calculate distance between two points
     */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Get touch position normalized to 0-1 range
     */
    fun normalizeTouchPosition(x: Float, y: Float): Pair<Float, Float> {
        val normalizedX = x / gameView.width
        val normalizedY = y / gameView.height
        return Pair(normalizedX, normalizedY)
    }
}

/**
 * Touch event types
 */
sealed class TouchEvent {
    data class Down(val x: Float, val y: Float, val pointerCount: Int) : TouchEvent()
    data class Move(val x: Float, val y: Float, val pointerCount: Int) : TouchEvent()
    data class Up(val x: Float, val y: Float, val pointerCount: Int) : TouchEvent()
    data class Cancel(val x: Float, val y: Float, val pointerCount: Int) : TouchEvent()
}

/**
 * Gesture event types
 */
sealed class GestureEvent {
    data class SingleTap(val x: Float, val y: Float) : GestureEvent()
    data class DoubleTap(val x: Float, val y: Float) : GestureEvent()
    data class LongPress(val x: Float, val y: Float) : GestureEvent()
    data class Scroll(val x: Float, val y: Float, val distanceX: Float, val distanceY: Float) : GestureEvent()
    data class Fling(val x: Float, val y: Float, val velocityX: Float, val velocityY: Float) : GestureEvent()
}

/**
 * Key event types
 */
sealed class KeyEvent {
    data class Down(val keyCode: Int) : KeyEvent()
    data class Up(val keyCode: Int) : KeyEvent()
}