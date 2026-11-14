package com.example.nxconvert

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var outputText: TextView
    private lateinit var argsInput: EditText
    private lateinit var runButton: Button
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputText = findViewById(R.id.output_text)
        argsInput = findViewById(R.id.args_input)
        runButton = findViewById(R.id.run_button)
        outputText.movementMethod = ScrollingMovementMethod()

        runButton.setOnClickListener {
            val raw = argsInput.text.toString().trim()
            val args = if (raw.isEmpty()) emptyList() else raw.split(" ").filter { it.isNotEmpty() }
            runNxconvert(args.toTypedArray())
        }
    }

    private fun runNxconvert(args: Array<String>) {
        runButton.isEnabled = false
        outputText.text = "Extracting nxconvert binary...\n"

        val thread = Thread {
            try {
                val binaryFile = File(filesDir, "nxconvert")
                if (!binaryFile.exists()) {
                    copyAssetToFiles("nxconvert")
                }
                binaryFile.setExecutable(true)

                val cmd = ArrayList<String>()
                cmd.add(binaryFile.absolutePath)
                cmd.addAll(args.toList())

                outputText.post {
                    outputText.text = "Running: ${cmd.joinToString(" ")}\n\n"
                }

                val processBuilder = ProcessBuilder(cmd)
                processBuilder.directory(filesDir)
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()

                val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                process.waitFor()

                handler.post {
                    outputText.text = "Exit code: ${process.exitValue()}\n\nOUTPUT:\n$output"
                    runButton.isEnabled = true
                }
            } catch (e: Exception) {
                handler.post {
                    outputText.text = "Error: ${e.message}\n\n${e.stackTrace.joinToString("\n") { it.toString() }}"
                    runButton.isEnabled = true
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun copyAssetToFiles(assetName: String) {
        val dest = File(filesDir, assetName)
        assets.open(assetName).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        const val ARG_HELP = "--help"
        const val ARG_VERSION = "--version"
    }
}
