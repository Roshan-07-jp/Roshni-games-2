package com.roshni.games.metadata.localization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * Localization system for multiple language support
 */
class LocalizationManager {

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Localization data for all supported languages
    private val localizationData = mutableMapOf<String, MutableMap<String, String>>()

    // Supported languages
    private val supportedLanguages = mapOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "pt" to "Português",
        "ru" to "Русский",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文"
    )

    init {
        initializeLocalizationData()
    }

    /**
     * Set current language
     */
    fun setCurrentLanguage(language: String) {
        if (supportedLanguages.containsKey(language)) {
            _currentLanguage.value = language
            Timber.d("Language set to: ${supportedLanguages[language]}")
        } else {
            Timber.w("Unsupported language: $language")
        }
    }

    /**
     * Get localized string
     */
    fun getString(key: String, language: String? = null): String {
        val lang = language ?: _currentLanguage.value
        return localizationData[lang]?.get(key) ?: localizationData["en"]?.get(key) ?: key
    }

    /**
     * Get localized string with parameters
     */
    fun getString(key: String, vararg params: Any, language: String? = null): String {
        val template = getString(key, language)
        return String.format(template, *params)
    }

    /**
     * Add localization entry
     */
    fun addLocalization(language: String, key: String, value: String) {
        localizationData.getOrPut(language) { mutableMapOf() }[key] = value
    }

    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): Map<String, String> {
        return supportedLanguages.toMap()
    }

    /**
     * Check if language is supported
     */
    fun isLanguageSupported(language: String): Boolean {
        return supportedLanguages.containsKey(language)
    }

    /**
     * Get current language name
     */
    fun getCurrentLanguageName(): String {
        return supportedLanguages[_currentLanguage.value] ?: "English"
    }

    private fun initializeLocalizationData() {
        // Initialize common game strings for all languages
        val commonKeys = mapOf(
            // Menu strings
            "menu_play" to mapOf(
                "en" to "Play",
                "es" to "Jugar",
                "fr" to "Jouer",
                "de" to "Spielen",
                "it" to "Gioca",
                "pt" to "Jogar",
                "ru" to "Играть",
                "ja" to "プレイ",
                "ko" to "플레이",
                "zh" to "玩"
            ),
            "menu_settings" to mapOf(
                "en" to "Settings",
                "es" to "Configuración",
                "fr" to "Paramètres",
                "de" to "Einstellungen",
                "it" to "Impostazioni",
                "pt" to "Configurações",
                "ru" to "Настройки",
                "ja" to "設定",
                "ko" to "설정",
                "zh" to "设置"
            ),
            "menu_achievements" to mapOf(
                "en" to "Achievements",
                "es" to "Logros",
                "fr" to "Succès",
                "de" to "Erfolge",
                "it" to "Risultati",
                "pt" to "Conquistas",
                "ru" to "Достижения",
                "ja" to "実績",
                "ko" to "업적",
                "zh" to "成就"
            ),
            "menu_leaderboard" to mapOf(
                "en" to "Leaderboard",
                "es" to "Tabla de clasificación",
                "fr" to "Classement",
                "de" to "Bestenliste",
                "it" to "Classifica",
                "pt" to "Placar",
                "ru" to "Таблица лидеров",
                "ja" to "リーダーボード",
                "ko" to "리더보드",
                "zh" to "排行榜"
            ),

            // Game strings
            "game_score" to mapOf(
                "en" to "Score",
                "es" to "Puntuación",
                "fr" to "Score",
                "de" to "Punktzahl",
                "it" to "Punteggio",
                "pt" to "Pontuação",
                "ru" to "Счёт",
                "ja" to "スコア",
                "ko" to "점수",
                "zh" to "分数"
            ),
            "game_level" to mapOf(
                "en" to "Level",
                "es" to "Nivel",
                "fr" to "Niveau",
                "de" to "Level",
                "it" to "Livello",
                "pt" to "Nível",
                "ru" to "Уровень",
                "ja" to "レベル",
                "ko" to "레벨",
                "zh" to "等级"
            ),
            "game_lives" to mapOf(
                "en" to "Lives",
                "es" to "Vidas",
                "fr" to "Vies",
                "de" to "Leben",
                "it" to "Vite",
                "pt" to "Vidas",
                "ru" to "Жизни",
                "ja" to "ライフ",
                "ko" to "목숨",
                "zh" to "生命"
            ),
            "game_pause" to mapOf(
                "en" to "Pause",
                "es" to "Pausa",
                "fr" to "Pause",
                "de" to "Pause",
                "it" to "Pausa",
                "pt" to "Pausa",
                "ru" to "Пауза",
                "ja" to "ポーズ",
                "ko" to "일시정지",
                "zh" to "暂停"
            ),

            // Achievement strings
            "achievement_unlocked" to mapOf(
                "en" to "Achievement Unlocked!",
                "es" to "¡Logro desbloqueado!",
                "fr" to "Succès débloqué!",
                "de" to "Erfolg freigeschaltet!",
                "it" to "Risultato sbloccato!",
                "pt" to "Conquista desbloqueada!",
                "ru" to "Достижение разблокировано!",
                "ja" to "実績を解除しました！",
                "ko" to "업적이 해제되었습니다!",
                "zh" to "成就解锁！"
            ),
            "achievement_progress" to mapOf(
                "en" to "Progress",
                "es" to "Progreso",
                "fr" to "Progrès",
                "de" to "Fortschritt",
                "it" to "Progresso",
                "pt" to "Progresso",
                "ru" to "Прогресс",
                "ja" to "進捗",
                "ko" to "진행도",
                "zh" to "进度"
            ),

            // Error messages
            "error_network" to mapOf(
                "en" to "Network error. Please check your connection.",
                "es" to "Error de red. Verifique su conexión.",
                "fr" to "Erreur de réseau. Vérifiez votre connexion.",
                "de" to "Netzwerkfehler. Bitte überprüfen Sie Ihre Verbindung.",
                "it" to "Errore di rete. Controlla la tua connessione.",
                "pt" to "Erro de rede. Verifique sua conexão.",
                "ru" to "Ошибка сети. Проверьте подключение.",
                "ja" to "ネットワークエラー。接続を確認してください。",
                "ko" to "네트워크 오류. 연결을 확인하세요.",
                "zh" to "网络错误。请检查您的连接。"
            ),
            "error_loading" to mapOf(
                "en" to "Loading error. Please try again.",
                "es" to "Error de carga. Inténtelo de nuevo.",
                "fr" to "Erreur de chargement. Veuillez réessayer.",
                "de" to "Ladefehler. Bitte versuchen Sie es erneut.",
                "it" to "Errore di caricamento. Riprova.",
                "pt" to "Erro de carregamento. Tente novamente.",
                "ru" to "Ошибка загрузки. Попробуйте еще раз.",
                "ja" to "読み込みエラー。もう一度お試しください。",
                "ko" to "로드 오류. 다시 시도하세요.",
                "zh" to "加载错误。请重试。"
            )
        )

        // Initialize localization data for all languages
        supportedLanguages.keys.forEach { language ->
            localizationData[language] = mutableMapOf()

            commonKeys.forEach { (key, translations) ->
                localizationData[language]?.put(key, translations[language] ?: translations["en"] ?: key)
            }
        }

        // Add game-specific strings
        initializeGameSpecificStrings()

        Timber.d("Initialized localization data for ${supportedLanguages.size} languages")
    }

    private fun initializeGameSpecificStrings() {
        // Puzzle game strings
        addGameStrings("puzzle", mapOf(
            "hint" to mapOf(
                "en" to "Hint",
                "es" to "Pista",
                "fr" to "Indice",
                "de" to "Hinweis",
                "it" to "Suggerimento",
                "pt" to "Dica",
                "ru" to "Подсказка",
                "ja" to "ヒント",
                "ko" to "힌트",
                "zh" to "提示"
            ),
            "moves" to mapOf(
                "en" to "Moves",
                "es" to "Movimientos",
                "fr" to "Mouvements",
                "de" to "Züge",
                "it" to "Mosse",
                "pt" to "Movimentos",
                "ru" to "Ходы",
                "ja" to "ムーブ",
                "ko" to "움직임",
                "zh" to "移动"
            )
        ))

        // Action game strings
        addGameStrings("action", mapOf(
            "combo" to mapOf(
                "en" to "Combo",
                "es" to "Combo",
                "fr" to "Combo",
                "de" to "Kombo",
                "it" to "Combo",
                "pt" to "Combo",
                "ru" to "Комбо",
                "ja" to "コンボ",
                "ko" to "콤보",
                "zh" to "连击"
            ),
            "power_up" to mapOf(
                "en" to "Power Up",
                "es" to "Potenciador",
                "fr" to "Bonus",
                "de" to "Power-Up",
                "it" to "Potenziamento",
                "pt" to "Power Up",
                "ru" to "Усиление",
                "ja" to "パワーアップ",
                "ko" to "파워업",
                "zh" to "强化"
            )
        ))

        // Strategy game strings
        addGameStrings("strategy", mapOf(
            "resources" to mapOf(
                "en" to "Resources",
                "es" to "Recursos",
                "fr" to "Ressources",
                "de" to "Ressourcen",
                "it" to "Risorse",
                "pt" to "Recursos",
                "ru" to "Ресурсы",
                "ja" to "リソース",
                "ko" to "자원",
                "zh" to "资源"
            ),
            "units" to mapOf(
                "en" to "Units",
                "es" to "Unidades",
                "fr" to "Unités",
                "de" to "Einheiten",
                "it" to "Unità",
                "pt" to "Unidades",
                "ru" to "Юниты",
                "ja" to "ユニット",
                "ko" to "유닛",
                "zh" to "单位"
            )
        ))
    }

    private fun addGameStrings(gameType: String, strings: Map<String, Map<String, String>>) {
        strings.forEach { (key, translations) ->
            supportedLanguages.keys.forEach { language ->
                val localizedKey = "${gameType}_$key"
                val value = translations[language] ?: translations["en"] ?: key
                localizationData[language]?.put(localizedKey, value)
            }
        }
    }

    /**
     * Get localization statistics
     */
    fun getLocalizationStats(): LocalizationStats {
        val totalKeys = localizationData.values.firstOrNull()?.size ?: 0
        val completionRates = supportedLanguages.keys.associateWith { language ->
            val languageData = localizationData[language]
            if (languageData != null && totalKeys > 0) {
                languageData.size.toFloat() / totalKeys
            } else {
                0f
            }
        }

        return LocalizationStats(
            supportedLanguages = supportedLanguages.size,
            totalKeys = totalKeys,
            completionRates = completionRates,
            currentLanguage = _currentLanguage.value
        )
    }

    /**
     * Export localization data for external translation
     */
    fun exportLocalizationData(): Map<String, Map<String, String>> {
        return localizationData.toMap()
    }

    /**
     * Import localization data
     */
    fun importLocalizationData(language: String, data: Map<String, String>) {
        localizationData[language] = data.toMutableMap()
        Timber.d("Imported localization data for language: $language")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        localizationData.clear()
    }
}

/**
 * Localization statistics
 */
data class LocalizationStats(
    val supportedLanguages: Int,
    val totalKeys: Int,
    val completionRates: Map<String, Float>,
    val currentLanguage: String
)

/**
 * Language information
 */
@Serializable
data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeName: String,
    val isRTL: Boolean = false
)