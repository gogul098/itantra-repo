package com.itantra

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.itantra.asr.AsrEngine
import com.itantra.audio.AudioCaptureService
import com.itantra.audio.AudioPlaybackService
import com.itantra.audio.VadProcessor
import com.itantra.cache.CosineCache
import com.itantra.cache.VectorCacheDb
import com.itantra.pipeline.ReceiverPipeline
import com.itantra.pipeline.TransmitterPipeline
import com.itantra.semantic.SonarDecoder
import com.itantra.semantic.SonarEncoder
import com.itantra.transport.TransportManager
import com.itantra.tts.PiperTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var transmitterPipeline: TransmitterPipeline
    private lateinit var receiverPipeline: ReceiverPipeline
    private lateinit var asrEngine: AsrEngine

    private val supportedLanguages = listOf(
        "eng_Latn" to "English",
        "hin_Deva" to "Hindi",
        "guj_Gujr" to "Gujarati",
        "mar_Deva" to "Marathi",
        "kan_Knda" to "Kannada",
        "mal_Mlym" to "Malayalam",
        "tam_Taml" to "Tamil",
        "tel_Telu" to "Telugu",
        "ory_Orya" to "Odia",
        "ben_Beng" to "Bengali"
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        setContentView(R.layout.activity_main)

        setupPermissions()
        setupUI()
        
        // Initialize Core Components
        // In a real app, this would be managed by Dagger/Hilt or a DI framework
        CoroutineScope(Dispatchers.IO).launch {
            try {
                initializeApp()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize app")
            }
        }
    }

    private fun setupPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val missing = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun setupUI() {
        val displayNames = supportedLanguages.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        findViewById<Spinner>(R.id.sourceLangSpinner).adapter = adapter
        findViewById<Spinner>(R.id.targetLangSpinner).adapter = adapter

        // Set defaults: English -> Hindi
        findViewById<Spinner>(R.id.sourceLangSpinner).setSelection(0)
        findViewById<Spinner>(R.id.targetLangSpinner).setSelection(1)

        val pttButton = findViewById<Button>(R.id.pttButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        pttButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    statusText.text = "Status: TRANSMITTING"
                    statusText.setTextColor(android.graphics.Color.RED)
                    val sourceCode = supportedLanguages[findViewById<Spinner>(R.id.sourceLangSpinner).selectedItemPosition].first
                    val targetCode = supportedLanguages[findViewById<Spinner>(R.id.targetLangSpinner).selectedItemPosition].first
                    transmitterPipeline.startTransmission(sourceCode, targetCode)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    statusText.text = "Status: IDLE"
                    statusText.setTextColor(android.graphics.Color.BLACK)
                    transmitterPipeline.stopTransmission()
                    true
                }
                else -> false
            }
        }

        val confSeek = findViewById<SeekBar>(R.id.confidenceSeekBar)
        confSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                asrEngine.setConfidenceThreshold(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initializeApp() {
        Timber.i("Initializing iTantra engines...")
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        // Note: For actual P2P we'd need a connected device. Mocking the device init here.
        val dummyDevice = adapter?.bondedDevices?.firstOrNull() 
        val transportManager = dummyDevice?.let { TransportManager(it) } ?: throw Exception("No paired Bluetooth devices found")

        // In real world we would also start the servers here using transportManager

        // Audio components
        val audioCapture = AudioCaptureService()
        val vadProcessor = VadProcessor(this)
        asrEngine = AsrEngine(this)
        val audioPlayback = AudioPlaybackService(this)

        // Semantic components
        val sonarEncoder = SonarEncoder(this)
        val sonarDecoder = SonarDecoder(this)

        // TTS & Cache
        val piperTts = PiperTtsEngine(this)
        val db = androidx.room.Room.databaseBuilder(applicationContext, VectorCacheDb::class.java, "vector-cache").build()
        val cosineCache = CosineCache(db)

        transmitterPipeline = TransmitterPipeline(
            audioCapture, vadProcessor, asrEngine, sonarEncoder, transportManager
        )

        receiverPipeline = ReceiverPipeline(
            transportManager, cosineCache, sonarDecoder, piperTts, audioPlayback
        )

        // Auto-start receiving in the background for the chosen target language
        val targetCode = supportedLanguages[1].first // Default Hindi
        receiverPipeline.startReceiving(targetCode)

        runOnUiThread {
            findViewById<TextView>(R.id.logText).append("All engines initialized successfully.\n")
            findViewById<TextView>(R.id.connectionText).text = "Connection: BLE L2CAP READY"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        receiverPipeline.stopReceiving()
        transmitterPipeline.stopTransmission()
        asrEngine.close()
    }
}
