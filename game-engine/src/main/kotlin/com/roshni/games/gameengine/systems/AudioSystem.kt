package com.roshni.games.gameengine.systems

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.roshni.games.gameengine.core.GameSystem
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Audio system for managing background music, sound effects, and audio playback
 */
class AudioSystem(
    private val context: Context
) : GameSystem() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Media players for background music
    private val mediaPlayers = ConcurrentHashMap<String, MediaPlayer>()
    private var currentMusicPlayer: MediaPlayer? = null

    // Sound pool for sound effects
    private lateinit var soundPool: SoundPool
    private val soundPoolMap = ConcurrentHashMap<String, Int>()
    private val soundVolumes = ConcurrentHashMap<Int, Float>()

    // Audio settings
    private var masterVolume = 1.0f
    private var musicVolume = 1.0f
    private var sfxVolume = 1.0f
    private var isMuted = false

    // Audio focus
    private var audioManager: AudioManager? = null
    private var hasAudioFocus = false

    override fun initialize() {
        Timber.d("Initializing audio system")

        // Initialize audio manager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Setup sound pool for sound effects
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // Request audio focus
        requestAudioFocus()

        Timber.d("Audio system initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up audio system")

        // Stop all music
        stopAllMusic()

        // Release all media players
        mediaPlayers.values.forEach { it.release() }
        mediaPlayers.clear()

        // Release sound pool
        soundPool.release()

        // Abandon audio focus
        abandonAudioFocus()

        scope.cancel()
    }

    override fun pause() {
        Timber.d("Pausing audio system")

        // Pause all music
        mediaPlayers.values.forEach { it.pause() }
        currentMusicPlayer?.pause()
    }

    override fun resume() {
        Timber.d("Resuming audio system")

        // Resume music if we had focus
        if (hasAudioFocus) {
            currentMusicPlayer?.start()
        }
    }

    /**
     * Load background music from assets
     */
    fun loadMusic(musicId: String, assetPath: String): Boolean {
        return try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(context.assets.openFd(assetPath))
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                prepare()
                setVolume(musicVolume * masterVolume, musicVolume * masterVolume)
                isLooping = true
            }

            mediaPlayers[musicId] = mediaPlayer
            Timber.d("Loaded music: $musicId from $assetPath")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load music: $musicId")
            false
        }
    }

    /**
     * Load sound effect from assets
     */
    fun loadSoundEffect(soundId: String, assetPath: String, volume: Float = 1.0f): Boolean {
        return try {
            val soundFd = context.assets.openFd(assetPath)
            val soundIdLoaded = soundPool.load(soundFd, 1)
            soundPoolMap[soundId] = soundIdLoaded
            soundVolumes[soundIdLoaded] = volume

            soundFd.close()
            Timber.d("Loaded sound effect: $soundId from $assetPath")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load sound effect: $soundId")
            false
        }
    }

    /**
     * Play background music
     */
    fun playMusic(musicId: String, fadeIn: Boolean = false) {
        if (isMuted) return

        val mediaPlayer = mediaPlayers[musicId] ?: run {
            Timber.w("Music not loaded: $musicId")
            return
        }

        // Stop current music
        currentMusicPlayer?.apply {
            if (isPlaying) {
                if (fadeIn) {
                    fadeOutAndStop()
                } else {
                    stop()
                }
            }
        }

        currentMusicPlayer = mediaPlayer

        if (hasAudioFocus) {
            mediaPlayer.start()
            if (fadeIn) {
                fadeInMusic()
            }
        }

        Timber.d("Playing music: $musicId")
    }

    /**
     * Stop background music
     */
    fun stopMusic(fadeOut: Boolean = false) {
        currentMusicPlayer?.let { player ->
            if (player.isPlaying) {
                if (fadeOut) {
                    fadeOutAndStop()
                } else {
                    player.pause()
                    player.seekTo(0)
                }
            }
        }
        currentMusicPlayer = null
    }

    /**
     * Stop all music
     */
    fun stopAllMusic() {
        mediaPlayers.values.forEach { player ->
            if (player.isPlaying) {
                player.pause()
                player.seekTo(0)
            }
        }
        currentMusicPlayer = null
    }

    /**
     * Play sound effect
     */
    fun playSoundEffect(soundId: String, rate: Float = 1.0f): Int {
        if (isMuted) return -1

        val soundPoolId = soundPoolMap[soundId] ?: run {
            Timber.w("Sound effect not loaded: $soundId")
            return -1
        }

        val volume = soundVolumes[soundPoolId] ?: 1.0f
        val adjustedVolume = volume * sfxVolume * masterVolume

        return soundPool.play(soundPoolId, adjustedVolume, adjustedVolume, 1, 0, rate)
    }

    /**
     * Stop sound effect by stream ID
     */
    fun stopSoundEffect(streamId: Int) {
        soundPool.stop(streamId)
    }

    /**
     * Set master volume
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0.0f, 1.0f)
        updateAllVolumes()
    }

    /**
     * Set music volume
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        updateMusicVolumes()
    }

    /**
     * Set sound effects volume
     */
    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0.0f, 1.0f)
        // SFX volumes are updated when playing
    }

    /**
     * Mute/unmute all audio
     */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopAllMusic()
        } else if (hasAudioFocus) {
            currentMusicPlayer?.start()
        }
    }

    /**
     * Check if music is playing
     */
    fun isMusicPlaying(): Boolean {
        return currentMusicPlayer?.isPlaying == true
    }

    /**
     * Get current music ID
     */
    fun getCurrentMusicId(): String? {
        return mediaPlayers.entries.find { it.value == currentMusicPlayer }?.key
    }

    private fun updateAllVolumes() {
        updateMusicVolumes()
        // SFX volumes are updated when playing
    }

    private fun updateMusicVolumes() {
        mediaPlayers.values.forEach { player ->
            val volume = musicVolume * masterVolume
            player.setVolume(volume, volume)
        }
    }

    private fun fadeInMusic() {
        currentMusicPlayer?.let { player ->
            scope.launch {
                val fadeSteps = 20
                val fadeDelay = 50L

                for (i in 1..fadeSteps) {
                    if (!player.isPlaying) break

                    val volume = (i.toFloat() / fadeSteps) * musicVolume * masterVolume
                    player.setVolume(volume, volume)
                    delay(fadeDelay)
                }
            }
        }
    }

    private fun fadeOutAndStop() {
        currentMusicPlayer?.let { player ->
            scope.launch {
                val fadeSteps = 20
                val fadeDelay = 50L
                val currentVolume = FloatArray(1)
                player.getVolume(currentVolume)

                for (i in fadeSteps downTo 1) {
                    if (!player.isPlaying) break

                    val volume = (i.toFloat() / fadeSteps) * currentVolume[0]
                    player.setVolume(volume, volume)
                    delay(fadeDelay)
                }

                player.pause()
                player.seekTo(0)
            }
        }
    }

    private fun requestAudioFocus() {
        try {
            // Note: Audio focus request would need AudioFocusRequest for API 26+
            // For simplicity, we'll assume we have focus
            hasAudioFocus = true
            Timber.d("Audio focus requested")
        } catch (e: Exception) {
            Timber.e(e, "Failed to request audio focus")
            hasAudioFocus = false
        }
    }

    private fun abandonAudioFocus() {
        try {
            hasAudioFocus = false
            Timber.d("Audio focus abandoned")
        } catch (e: Exception) {
            Timber.e(e, "Failed to abandon audio focus")
        }
    }
}