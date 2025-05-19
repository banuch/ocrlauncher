// MainActivity.kt
package com.example.ocrlauncher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val tag = "OCRLauncher"

    // UI components
    private lateinit var serviceIdEditText: EditText
    private lateinit var valueTypeEditText: EditText
    private lateinit var launchButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var adapter: ReadingHistoryAdapter

    // Reading history
    private val readingHistory = mutableListOf<ReadingEntry>()

    // Activity result launcher
    private val ocrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 666) {
            result.data?.let { data ->
                // Extract metadata from the returned intent
                val metadataStr = data.getStringExtra("metadata")
                if (!metadataStr.isNullOrEmpty()) {
                    try {
                        // Parse JSON metadata
                        val metadata = JSONObject(metadataStr)
                        val meterReading = metadata.optString("meterReading", "No reading")
                        val filename = metadata.optString("filename", "Unknown")
                        val isEdited = metadata.optBoolean("isEdited", false)

                        // Display the result
                        val resultText = """
                            Meter Reading: $meterReading
                            Image Path: $filename
                            Manually Edited: $isEdited
                        """.trimIndent()
                        resultTextView.text = resultText

                        // Add to history
                        val serviceId = serviceIdEditText.text.toString()
                        val valueType = valueTypeEditText.text.toString()
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date())

                        val entry = ReadingEntry(
                            serviceId = serviceId,
                            valueType = valueType,
                            reading = meterReading,
                            timestamp = timestamp,
                            imagePath = filename,
                            isEdited = isEdited
                        )
                        readingHistory.add(0, entry)  // Add to beginning of list
                        adapter.notifyItemInserted(0)
                        historyRecyclerView.scrollToPosition(0)

                        // Save history to preferences
                        saveHistoryToPreferences()
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing metadata: ${e.message}")
                        resultTextView.text = "Error processing result: ${e.message}"
                    }
                } else {
                    resultTextView.text = "No metadata returned"
                }
            }
        } else {
            resultTextView.text = "OCR scan cancelled"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        serviceIdEditText = findViewById(R.id.serviceIdEditText)
        valueTypeEditText = findViewById(R.id.valueTypeEditText)
        launchButton = findViewById(R.id.launchButton)
        resultTextView = findViewById(R.id.resultTextView)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)

        // Set up RecyclerView
        adapter = ReadingHistoryAdapter(readingHistory)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = adapter

        // Load history from preferences
        loadHistoryFromPreferences()

        // Set up button click listener
        launchButton.setOnClickListener {
            launchOCRApp()
        }
    }

    private fun launchOCRApp() {
        val serviceId = serviceIdEditText.text.toString()
        val valueType = valueTypeEditText.text.toString()

        if (serviceId.isBlank() || valueType.isBlank()) {
            resultTextView.text = "Please enter Service ID and Value Type"
            return
        }

        // Create an intent to launch the OCR app
        val intent = Intent().apply {
            setClassName("com.example.cameraxapp", "com.example.cameraxapp.MainActivity")
            putExtra("serviceId", serviceId)
            putExtra("valType", valueType)
        }

        try {
            // Launch OCR app
            ocrLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error launching OCR app: ${e.message}")
            resultTextView.text = "Error: OCR app not found. Make sure it's installed."
        }
    }

    private fun saveHistoryToPreferences() {
        val sharedPrefs = getSharedPreferences("OCRLauncherPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Convert history to JSON array
        val historyJson = JSONObject()
        val entriesArray = JSONObject()

        readingHistory.forEachIndexed { index, entry ->
            val entryJson = JSONObject().apply {
                put("serviceId", entry.serviceId)
                put("valueType", entry.valueType)
                put("reading", entry.reading)
                put("timestamp", entry.timestamp)
                put("imagePath", entry.imagePath)
                put("isEdited", entry.isEdited)
            }
            entriesArray.put(index.toString(), entryJson)
        }

        historyJson.put("entries", entriesArray)
        editor.putString("readingHistory", historyJson.toString())
        editor.apply()
    }

    private fun loadHistoryFromPreferences() {
        val sharedPrefs = getSharedPreferences("OCRLauncherPrefs", MODE_PRIVATE)
        val historyStr = sharedPrefs.getString("readingHistory", null)

        if (!historyStr.isNullOrEmpty()) {
            try {
                val historyJson = JSONObject(historyStr)
                val entriesArray = historyJson.optJSONObject("entries")

                if (entriesArray != null) {
                    readingHistory.clear()
                    val keys = entriesArray.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        val entryJson = entriesArray.getJSONObject(key)

                        val entry = ReadingEntry(
                            serviceId = entryJson.optString("serviceId", ""),
                            valueType = entryJson.optString("valueType", ""),
                            reading = entryJson.optString("reading", ""),
                            timestamp = entryJson.optString("timestamp", ""),
                            imagePath = entryJson.optString("imagePath", ""),
                            isEdited = entryJson.optBoolean("isEdited", false)
                        )
                        readingHistory.add(entry)
                    }

                    // Sort by timestamp (newest first)
                    readingHistory.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading history: ${e.message}")
            }
        }
    }
}

