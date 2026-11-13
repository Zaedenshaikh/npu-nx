package com.example.nxformat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxformat.model.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: NxFormatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NxFormatTheme {
                NxFormatApp(viewModel)
            }
        }
    }
}

@Composable
fun NxFormatApp(viewModel: NxFormatViewModel) {
    var scanPath by remember { mutableStateOf("") }
    var selectedArchive by remember { mutableStateOf<File?>(null) }
    var showInspector by remember { mutableStateOf(false) }

    val events by viewModel.events.collectAsStateWithInitial(NxFormatEvent.Idle)
    val isBusy by viewModel.isBusy.collectAsStateWithInitial(false)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("NXFormat Android") })
        },
        floatingActionButton = if (!showInspector) {
            FloatingActionButton(
                onClick = { showInspector = true },
                icon = { Icon(Icons.Default.Info, contentDescription = "Inspect") }
            )
        } else null,
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Scan section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Scan for .nx archives", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = scanPath,
                                onValueChange = { scanPath = it },
                                label = { Text("Directory") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
                            )
                            Button(
                                onClick = {
                                    if (scanPath.isNotBlank()) {
                                        viewModel.scanDirectory(scanPath)
                                    }
                                },
                            ) { Text("Scan") }
                        }
                    }
                }

                // Results
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                when (events) {
                    is NxFormatEvent.Scanned -> {
                        if (events.archives.isNotEmpty()) {
                            Text("Found ${events.archives.size} archive(s)", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(events.archives) { archive ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        onClick = {
                                            selectedArchive = archive
                                            viewModel.inspectArchive(archive)
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(archive.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${archive.length / 1024 / 1024} MB",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is NxFormatEvent.Inspected -> {
                        val info = events.info
                        Text("Archive Inspected", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Snapshots: ${info.snapshotCount}")
                        Text("Format: NX ${info.header.majorVersion}.${info.header.minorVersion}")
                        Text("Generation: ${info.header.generation}")
                        Text("Check Size: ${info.header.checkSize} bytes")
                        info.catalog.applications.forEach { app ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("App: ${app.applicationId.toString(16).uppercase()} (${when(app.platform){1->"Switch1";2->"Switch2";else->"Unknown"})")
                            Text("  Snapshots: ${app.snapshotCount}")
                            app.snapshots.forEachIndexed { idx, snap ->
                                Text(
                                    "    [$idx] ${when(snap.snapshotType){0->"Base";1->"Update";2->"DLC";3->"Standalone";else->"Unknown"}} " +
                                    "v${snap.numericVersion} " +
                                    "identity: ${snap.snapshotIdentity.joinToString(""){"%02x".format(it)}}"
                                )
                            }
                        }
                    }
                    is NxFormatEvent.Error -> {
                        Text("Error: ${events.message}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        Text("Enter a directory to scan for .nx archives", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    )
}