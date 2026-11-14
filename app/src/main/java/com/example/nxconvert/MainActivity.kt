package com.example.nxconvert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var outputText: TextView
    private lateinit var argsInput: EditText
    private lateinit var runButton: Button
    private val handler = Handler(Looper.getMainLooper())
    companion object {
        const val TAG = "NXConvert"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            outputText = findViewById(R.id.output_text)
            argsInput = findViewById(R.id.args_input)
            runButton = findViewById(R.id.run_button)
            outputText.movementMethod = ScrollingMovementMethod()

            // Log device architecture for debugging
            Log.d(TAG, "Device arch: ${android.os.Build.CPU_ABI}, ${android.os.Build.CPU_ABI2}")

            runButton.setOnClickListener {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                    )
                    return@setOnClickListener
                }
                val raw = argsInput.text.toString().trim()
                val args = if (raw.isEmpty()) emptyList() else raw.split(" ").filter { it.isNotEmpty() }
                runNxconvert(args.toTypedArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate crashed", e)
            Toast.makeText(this, "Fatal error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runNxconvert(args: Array<String>) {
        runButton.isEnabled = false
        outputText.text = "Extracting nxconvert binary...\n"

        val thread = Thread {
            try {
                Log.d(TAG, "Starting nxconvert execution")

                val binaryFile = File(filesDir, "nxconvert")
                Log.d(TAG, "Binary path: ${binaryFile.absolutePath}, exists: ${binaryFile.exists()}")

                if (!binaryFile.exists()) {
                    Log.d(TAG, "Binary not found in filesDir, copying from assets")
                    copyAssetToFiles("nxconvert")
                }

                if (!binaryFile.exists()) {
                    throw RuntimeException("nxconvert binary not found in assets or filesDir")
                }

                Log.d(TAG, "Binary size: ${binaryFile.length()} bytes")
                binaryFile.setExecutable(true, false)

                val cmd = ArrayList<String>()
                cmd.add(binaryFile.absolutePath)
                cmd.addAll(args.toList())

                outputText.post {
                    outputText.text = "Running: ${cmd.joinToString(" ")}\n\n"
                }

                Log.d(TAG, "Command: ${cmd.joinToString(" ")}")

                val processBuilder = ProcessBuilder(cmd)
                processBuilder.directory(filesDir)
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()

                val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                process.waitFor()

                Log.d(TAG, "Process exit code: ${process.exitValue()}")
                Log.d(TAG, "Output: $output")

                handler.post {
                    outputText.text = "Exit code: ${process.exitValue()}\n\nOUTPUT:\n$output"
                    runButton.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "nxconvert execution failed", e)
                handler.post {
                    outputText.text = "Error: ${e.message}\n\n${e.stackTrace.joinToString("\n") { it.toString() }}"
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    runButton.isEnabled = true
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun copyAssetToFiles(assetName: String) {
        Log.d(TAG, "Copying asset: $assetName")
        val dest = File(filesDir, assetName)
        try {
            assets.open(assetName).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Asset copied successfully, dest size: ${dest.length()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset: $assetName", e)
            throw e
        }
    }
}
