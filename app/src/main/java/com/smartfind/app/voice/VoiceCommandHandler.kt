package com.smartfind.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice command handler that processes speech input and executes corresponding actions.
 *
 * Features:
 * - Speech recognition (Android SpeechRecognizer)
 * - Text-to-speech feedback (Android TTS)
 * - Command parsing and interpretation
 * - Action callbacks for navigation and features
 * - Multi-language support
 *
 * Supported Commands:
 * - "Find my [object]" - Search for object in history
 * - "Take photo" / "Capture" - Open camera
 * - "Show history" - Navigate to history screen
 * - "Set reminder for [object]" - Create reminder
 * - "Show statistics" - Navigate to stats
 * - "Search for [object]" - Search in history
 * - "Delete [object]" - Delete from history
 * - "Settings" - Open settings
 * - "Help" - Show help
 *
 * Usage:
 * ```
 * val voiceHandler = VoiceCommandHandler(context)
 * voiceHandler.setCommandListener(object : VoiceCommandListener {
 *     override fun onCommandRecognized(command: VoiceCommand) {
 *         // Handle command
 *     }
 * })
 * voiceHandler.startListening()
 * ```
 */
@Singleton
class VoiceCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {

    private val TAG = "VoiceCommandHandler"

    // Speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    // State management
    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    private val _recognizedText = MutableLiveData<String>()
    val recognizedText: LiveData<String> = _recognizedText

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Command listener
    private var commandListener: VoiceCommandListener? = null

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }

    /**
     * Initialize speech recognizer
     */
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            _error.postValue("Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceCommandHandler)
        }
    }

    /**
     * Initialize text-to-speech engine
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                isTtsReady = true
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS completed: $utteranceId")
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error: $utteranceId")
            }
        })
    }

    /**
     * Start listening for voice commands
     */
    fun startListening() {
        if (_isListening.value == true) {
            Log.w(TAG, "Already listening")
            return
        }

        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.postValue(true)
            _error.postValue(null)
            Log.d(TAG, "Started listening for voice commands")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _error.postValue("Failed to start voice recognition: ${e.message}")
            _isListening.postValue(false)
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.postValue(false)
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }

    /**
     * Cancel current recognition
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _isListening.postValue(false)
            Log.d(TAG, "Cancelled voice recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }

    /**
     * Speak text using TTS
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready")
            return
        }

        try {
            val utteranceId = UUID.randomUUID().toString()
            textToSpeech?.speak(text, queueMode, null, utteranceId)
            Log.d(TAG, "Speaking: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
        }
    }

    /**
     * Set command listener
     */
    fun setCommandListener(listener: VoiceCommandListener) {
        this.commandListener = listener
    }

    /**
     * Remove command listener
     */
    fun removeCommandListener() {
        this.commandListener = null
    }

    // RecognitionListener implementation

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
        _error.postValue(null)
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech started")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio level changed - can be used for visualization
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "Speech ended")
        _isListening.postValue(false)
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }

        Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
        _error.postValue(errorMessage)
        _isListening.postValue(false)

        // Auto-retry on certain errors
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            // Don't show error for these common cases
            _error.postValue(null)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        if (matches.isNullOrEmpty()) {
            Log.w(TAG, "No results received")
            _error.postValue("No voice input detected")
            return
        }

        val recognizedText = matches[0]
        Log.d(TAG, "Recognized: $recognizedText")
        _recognizedText.postValue(recognizedText)

        // Parse and execute command
        parseAndExecuteCommand(recognizedText)

        _isListening.postValue(false)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            Log.d(TAG, "Partial result: ${matches[0]}")
            _recognizedText.postValue(matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Recognition event: $eventType")
    }

    /**
     * Parse recognized text and execute corresponding command
     */
    private fun parseAndExecuteCommand(text: String) {
        val lowercaseText = text.lowercase(Locale.getDefault()).trim()

        val command = when {
            // Find object
            lowercaseText.startsWith("find") || lowercaseText.startsWith("where is") ||
            lowercaseText.startsWith("locate") -> {
                val objectName = extractObjectName(lowercaseText, listOf("find", "where is", "locate", "my"))
                VoiceCommand.FindObject(objectName)
            }

            // Take photo / Capture
            lowercaseText.contains("take photo") || lowercaseText.contains("take picture") ||
            lowercaseText.contains("capture") || lowercaseText.contains("camera") -> {
                VoiceCommand.TakePhoto
            }

            // Show history
            lowercaseText.contains("show history") || lowercaseText.contains("view history") ||
            lowercaseText.contains("my detections") -> {
                VoiceCommand.ShowHistory
            }

            // Set reminder
            lowercaseText.contains("set reminder") || lowercaseText.contains("remind me") ||
            lowercaseText.contains("create reminder") -> {
                val objectName = extractObjectName(lowercaseText, listOf("set reminder for", "remind me about", "create reminder for"))
                VoiceCommand.SetReminder(objectName)
            }

            // Show statistics
            lowercaseText.contains("statistics") || lowercaseText.contains("stats") ||
            lowercaseText.contains("insights") || lowercaseText.contains("analytics") -> {
                VoiceCommand.ShowStatistics
            }

            // Search
            lowercaseText.startsWith("search") -> {
                val query = extractObjectName(lowercaseText, listOf("search for", "search"))
                VoiceCommand.Search(query)
            }

            // Delete
            lowercaseText.contains("delete") || lowercaseText.contains("remove") -> {
                val objectName = extractObjectName(lowercaseText, listOf("delete", "remove"))
                VoiceCommand.Delete(objectName)
            }

            // Settings
            lowercaseText.contains("settings") || lowercaseText.contains("preferences") ||
            lowercaseText.contains("options") -> {
                VoiceCommand.OpenSettings
            }

            // Help
            lowercaseText.contains("help") || lowercaseText.contains("what can you do") ||
            lowercaseText.contains("commands") -> {
                VoiceCommand.ShowHelp
            }

            // Start detection
            lowercaseText.contains("start detection") || lowercaseText.contains("begin detection") -> {
                VoiceCommand.StartDetection
            }

            // Stop detection
            lowercaseText.contains("stop detection") || lowercaseText.contains("pause detection") -> {
                VoiceCommand.StopDetection
            }

            // Export data
            lowercaseText.contains("export") || lowercaseText.contains("download data") -> {
                VoiceCommand.ExportData
            }

            // Switch camera
            lowercaseText.contains("switch camera") || lowercaseText.contains("flip camera") -> {
                VoiceCommand.SwitchCamera
            }

            // Toggle flash
            lowercaseText.contains("flash") || lowercaseText.contains("flashlight") -> {
                VoiceCommand.ToggleFlash
            }

            // Unknown command
            else -> VoiceCommand.Unknown(text)
        }

        Log.d(TAG, "Parsed command: $command")

        // Notify listener
        commandListener?.onCommandRecognized(command)

        // Provide voice feedback
        provideVoiceFeedback(command)
    }

    /**
     * Extract object name from command text
     */
    private fun extractObjectName(text: String, prefixes: List<String>): String {
        var result = text

        // Remove prefixes
        for (prefix in prefixes) {
            if (result.contains(prefix)) {
                result = result.substringAfter(prefix).trim()
            }
        }

        // Remove common words
        val wordsToRemove = listOf("the", "a", "an", "my", "for", "about")
        for (word in wordsToRemove) {
            result = result.replace("\\b$word\\b".toRegex(), "").trim()
        }

        return result.ifEmpty { "object" }
    }

    /**
     * Provide voice feedback for executed command
     */
    private fun provideVoiceFeedback(command: VoiceCommand) {
        val feedback = when (command) {
            is VoiceCommand.FindObject -> "Searching for ${command.objectName}"
            is VoiceCommand.TakePhoto -> "Opening camera"
            is VoiceCommand.ShowHistory -> "Showing detection history"
            is VoiceCommand.SetReminder -> "Setting reminder for ${command.objectName}"
            is VoiceCommand.ShowStatistics -> "Showing statistics"
            is VoiceCommand.Search -> "Searching for ${command.query}"
            is VoiceCommand.Delete -> "Deleting ${command.objectName}"
            is VoiceCommand.OpenSettings -> "Opening settings"
            is VoiceCommand.ShowHelp -> "Here are the available commands"
            is VoiceCommand.StartDetection -> "Starting detection"
            is VoiceCommand.StopDetection -> "Stopping detection"
            is VoiceCommand.ExportData -> "Exporting data"
            is VoiceCommand.SwitchCamera -> "Switching camera"
            is VoiceCommand.ToggleFlash -> "Toggling flash"
            is VoiceCommand.Unknown -> "I didn't understand that. Say 'help' for available commands."
        }

        speak(feedback)
    }

    /**
     * Get available commands list
     */
    fun getAvailableCommands(): List<String> {
        return listOf(
            "Find my [object]",
            "Where is my [object]",
            "Take photo",
            "Show history",
            "Set reminder for [object]",
            "Show statistics",
            "Search for [object]",
            "Delete [object]",
            "Settings",
            "Help",
            "Start detection",
            "Stop detection",
            "Export data",
            "Switch camera",
            "Toggle flash"
        )
    }

    /**
     * Release resources
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null

            scope.cancel()

            Log.d(TAG, "VoiceCommandHandler destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying VoiceCommandHandler", e)
        }
    }
}

/**
 * Sealed class representing different voice commands
 */
sealed class VoiceCommand {
    data class FindObject(val objectName: String) : VoiceCommand()
    object TakePhoto : VoiceCommand()
    object ShowHistory : VoiceCommand()
    data class SetReminder(val objectName: String) : VoiceCommand()
    object ShowStatistics : VoiceCommand()
    data class Search(val query: String) : VoiceCommand()
    data class Delete(val objectName: String) : VoiceCommand()
    object OpenSettings : VoiceCommand()
    object ShowHelp : VoiceCommand()
    object StartDetection : VoiceCommand()
    object StopDetection : VoiceCommand()
    object ExportData : VoiceCommand()
    object SwitchCamera : VoiceCommand()
    object ToggleFlash : VoiceCommand()
    data class Unknown(val text: String) : VoiceCommand()
}

/**
 * Listener interface for voice command events
 */
interface VoiceCommandListener {
    /**
     * Called when a voice command is recognized and parsed
     */
    fun onCommandRecognized(command: VoiceCommand)

    /**
     * Called when voice recognition encounters an error
     */
    fun onError(error: String) {}

    /**
     * Called when listening state changes
     */
    fun onListeningStateChanged(isListening: Boolean) {}
}
