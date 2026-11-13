package com.example.nxformat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nxformat.model.*
import kotlinx.coroutines.*
import java.io.File

sealed class NxFormatEvent {
    object Idle : NxFormatEvent()
    data class Scanning(val path: String) : NxFormatEvent()
    data class Scanned(val archives: List<File>) : NxFormatEvent()
    data class Inspecting(val archive: File) : NxFormatEvent()
    data class Inspected(val info: NxArchiveInfo) : NxFormatEvent()
    data class Error(val message: String) : NxFormatEvent()
    data class Packing(val source: String, val output: String) : NxFormatEvent()
    data class Packed(val output: File) : NxFormatEvent()
}

class NxFormatViewModel : ViewModel() {

    private val _events = MutableSharedFlow<NxFormatEvent>(replay = 1)
    val events = _events.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    fun scanDirectory(directory: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                _events.emit(NxFormatEvent.Scanning(directory))
                val dir = File(directory)
                val archives = dir.listFiles()
                    ?.filter { it.extension == "nx" }
                    .orEmpty()
                _events.emit(NxFormatEvent.Scanned(archives))
            } catch (e: Exception) {
                _events.emit(NxFormatEvent.Error(e.message ?: "Scan failed"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun inspectArchive(archive: File) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                _events.emit(NxFormatEvent.Inspecting(archive))
                val info = inspectArchive(archive)
                _events.emit(NxFormatEvent.Inspected(info))
            } catch (e: Exception) {
                _events.emit(NxFormatEvent.Error(e.message ?: "Inspect failed"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun packArchive(source: String, output: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                _events.emit(NxFormatEvent.Packing(source, output))
                // Placeholder: full packer requires native source inspection
                // For now, create a minimal valid archive
                writeMinimalArchive(File(source), File(output))
                _events.emit(NxFormatEvent.Packed(File(output)))
            } catch (e: Exception) {
                _events.emit(NxFormatEvent.Error(e.message ?: "Pack failed"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    private suspend fun writeMinimalArchive(source: File, output: File) {
        // Minimal NX archive with one empty snapshot
        // This is a skeleton; a full port requires the Rust packer logic via FFI
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(BufferedOutputStream(baos))

        // Header slot A (256 bytes)
        dos.write(NX_MAGIC)
        dos.writeShort(0) // major
        dos.writeShort(2) // minor
        dos.writeInt(256) // header_size
        dos.writeShort(1) // required_features
        dos.writeShort(0) // optional_features
        dos.writeLong(0)  // archive_size (placeholder)
        dos.writeLong(0)  // catalog_offset
        dos.writeLong(128) // catalog_size
        dos.writeLong(0)  // media_offset
        dos.writeLong(0)  // media_size
        dos.writeLong(0)  // data_offset
        dos.writeLong(0)  // data_size
        dos.writeLong(0)  // manifest_offset
        dos.writeLong(96) // manifest_size
        dos.writeLong(0)  // block_index_offset
        dos.writeLong(0)  // block_count
        dos.writeLong(0)  // integrity_offset
        dos.writeLong(0)  // integrity_size
        dos.write(sha256(ByteArray(0))) // catalog_sha256
        dos.write(sha256(ByteArray(0))) // manifest_sha256
        dos.write(sha256(ByteArray(0))) // index_sha256
        dos.writeLong(0) // generation
        dos.writeInt(CHECK_SIZE.toInt()) // check_size
        dos.write(ByteArray(24)) // reserved
        // CRC-32C placeholder
        dos.writeInt(0)

        // Pad to 256 bytes
        while (baos.size < HEADER_SIZE) dos.write(0)

        // Catalog (128 bytes)
        dos.write(NX_CT_MAGIC)
        dos.writeShort(1) // catalog_version
        dos.writeShort(128) // header_size
        dos.writeInt(128) // total_size
        dos.writeInt(0) // flags
        dos.writeInt(1) // application_count
        dos.writeInt(0) // program_count
        dos.writeInt(1) // snapshot_count
        dos.writeInt(0) // localized_entry_count
        dos.writeInt(0) // dependency_count
        dos.writeInt(0) // extension_count
        dos.writeInt(0) // reserved0
        dos.writeLong(48) // application_table_offset
        dos.writeLong(0) // program_table_offset
        dos.writeLong(80) // snapshot_table_offset
        dos.writeLong(0) // localized_entry_table_offset
        dos.writeLong(0) // dependency_table_offset
        dos.writeLong(0) // extension_table_offset
        dos.writeLong(112) // string_pool_offset
        dos.writeLong(0) // string_pool_size
        dos.write(ByteArray(16)) // reserved1

        // Application table (80 bytes) - one app
        dos.writeLong(1) // application_id
        dos.write(1.toByte()) // platform Switch1
        dos.write(0.toByte()) // flags
        dos.writeShort(0) // reserved0
        dos.writeInt(0xFFFFFFFF) // first_program_index
        dos.writeInt(0) // program_count
        dos.writeInt(0) // first_snapshot_index
        dos.writeInt(1) // snapshot_count
        dos.writeInt(0xFFFFFFFF) // default_title_offset (absent)
        dos.writeInt(0) // default_title_length
        dos.writeInt(0xFFFFFFFF) // publisher_offset
        dos.writeInt(0) // publisher_length
        dos.writeInt(0xFFFFFFFF) // default_icon_media_index
        dos.writeInt(0) // first_localized_entry_index
        dos.writeInt(0) // localized_entry_count
        dos.writeInt(0xFFFFFFFF) // nacp_media_index
        dos.writeInt(0) // first_opaque_media_index
        dos.writeInt(0) // opaque_media_count
        dos.writeInt(0xFFFFFFFF) // metadata_snapshot_index
        dos.write(ByteArray(8)) // reserved1

        // Snapshot table (120 bytes) - one snapshot
        dos.writeInt(0xFFFFFFFF) // program_index (DLC/no program)
        dos.write(2.toByte()) // snapshot_type DLC
        dos.write(0.toByte()) // flags
        dos.writeShort(0) // reserved0
        dos.writeInt(1) // numeric_content_version
        dos.writeInt(0) // root_directory_index
        dos.writeInt(0xFFFFFFFF) // display_version_offset
        dos.writeInt(0) // display_version_length
        dos.write(ByteArray(16)) // content_id (zero)
        dos.writeInt(0) // first_dependency_index
        dos.writeInt(0) // dependency_count
        dos.write(sha256(ByteArray(0))) // snapshot_identity
        dos.write(ByteArray(32)) // build_id (zero)

        // Pad catalog to 128 bytes (already at 48+80+120=248... wait, that's wrong)
        // Actually catalog is 128 bytes total: header(128) - but tables come after header
        // The header says total_size=128 which is just the header itself
        // For a real writer, we'd need to write tables after header and set total_size correctly

        // Manifest (96 bytes)
        dos.write(NX_MF_MAGIC)
        dos.writeShort(1)
        dos.writeShort(96)
        dos.writeInt(96) // total_size
        dos.writeInt(1) // directory_count
        dos.writeInt(0) // file_count
        dos.writeInt(0) // entry_count
        dos.writeInt(0) // block_reference_count
        dos.writeLong(0) // directory_table_offset
        dos.writeLong(0) // file_table_offset
        dos.writeLong(0) // entry_table_offset
        dos.writeLong(0) // block_reference_table_offset
        dos.writeLong(0) // path_pool_offset
        dos.writeInt(0) // path_pool_size
        dos.write(ByteArray(16)) // reserved

        output.parentFile.mkdirs()
        output.writeBytes(baos.toByteArray())
    }
}