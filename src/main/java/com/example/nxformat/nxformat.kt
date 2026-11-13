package com.example.nxformat

import com.example.nxformat.model.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.CRC32

// NX magic and constants
private const val NX_MAGIC = byteArrayOf(0x4E, 0x58, 0x41, 0x52) // "NXAR"
private const val NX_CT_MAGIC = byteArrayOf(0x4E, 0x58, 0x43, 0x54) // "NXCT"
private const val NX_MF_MAGIC = byteArrayOf(0x4E, 0x58, 0x4D, 0x46) // "NXMF"
private const val NX_MD_MAGIC = byteArrayOf(0x4E, 0x58, 0x4D, 0x44) // "NXMD"

private const val HEADER_SIZE = 256L
private const val HEADER_SLOT_B_OFFSET = 4096L
private const val DATA_OFFSET = 8192L
private const val ALIGN = 16L
private const val CHECK_SIZE = 16384L

// CRC-32C (Castagnoli) - table-based implementation
object Crc32c {
    private val table = IntArray(256)
    private const val POLY = 0x82F63B78

    init {
        for (i in 0 until 256) {
            var crc = i
            for (j in 0 until 8) {
                crc = if ((crc and 1) != 0) (crc shr 1) xor POLY else crc shr 1
            }
            table[i] = crc
        }
    }

    fun compute(data: ByteArray, offset: Int = 0, len: Int = data.size): Int {
        var crc = 0xFFFFFFFF
        for (i in offset until offset + len) {
            crc = (crc shr 8) xor table[(crc xor (data[i].toInt() and 0xFF)).and(0xFF)]
        }
        return crc.inv()
    }
}

// --- Little-endian readers ---
inline fun ByteArray.readU16(offset: Int): Int =
    (this[offset] and 0xFF) or ((this[offset + 1] and 0xFF) shl 8)

inline fun ByteArray.readU32(offset: Int): Int =
    (this[offset] and 0xFF) or
        ((this[offset + 1] and 0xFF) shl 8) or
        ((this[offset + 2] and 0xFF) shl 16) or
        ((this[offset + 3] and 0xFF) shl 24)

inline fun ByteArray.readU64(offset: Int): Long {
    var result = 0L
    for (i in 7 downTo 0) {
        result = result shl 8 or (this[offset + i].toInt() and 0xFF)
    }
    return result
}

inline fun ByteArray.readBytes(offset: Int, length: Int): ByteArray =
    copyOfRange(offset, offset + length)

inline fun ByteArray.slice(offset: Int, length: Int): ByteArray =
    copyOfRange(offset, offset + length)

// --- SHA-256 ---
fun sha256(data: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(data)

fun sha256Hex(data: ByteArray): String =
    data.joinToString("") { "%02x".format(it) }

// --- NX Header (256 bytes) ---
fun parseHeader(data: ByteArray, offset: Long = 0): NxHeader {
    val o = offset.toInt()
    require(data.slice(o, 4).contentEquals(NX_MAGIC)) { "Invalid NX magic" }
    require(data.readU16(o + 4) == 0) { "Unsupported major version" }
    val minor = data.readU16(o + 6)
    require(minor == 2 || minor == 3) { "Unsupported minor version: $minor" }
    require(data.readU32(o + 12) == 256) { "Invalid header size" }
    require(data.readU16(o + 16) == 1) { "Required feature bit 0 missing" }

    val archiveSize = data.readU64(o + 24)
    val catalogOffset = data.readU64(o + 32)
    val catalogSize = data.readU64(o + 40)
    val mediaOffset = data.readU64(o + 48)
    val mediaSize = data.readU64(o + 56)
    val dataOffset = data.readU64(o + 64)
    val dataSize = data.readU64(o + 72)
    val manifestOffset = data.readU64(o + 80)
    val manifestSize = data.readU64(o + 88)
    val blockIndexOffset = data.readU64(o + 96)
    val blockCount = data.readU64(o + 104)
    val integrityOffset = data.readU64(o + 112)
    val integritySize = data.readU64(o + 116)
    val catalogSha256 = data.slice(o + 120, 32)
    val manifestSha256 = data.slice(o + 152, 32)
    val indexSha256 = data.slice(o + 184, 32)
    val generation = data.readU64(o + 216)
    val checkSize = data.readU32(o + 222).toInt()
    val headerCrc32c = data.readU32(o + 252)

    // Verify CRC-32C over bytes 0..251
    val crc = Crc32c.compute(data, o, 252)
    require(crc == headerCrc32c) { "Header CRC-32C mismatch: expected ${"%08x".format(headerCrc32c)}, got ${"%08x".format(crc)}" }

    return NxHeader(
        magic = data.slice(o, 4),
        majorVersion = 0,
        minorVersion = minor,
        archiveSize = archiveSize,
        catalogOffset = catalogOffset,
        catalogSize = catalogSize,
        mediaOffset = mediaOffset,
        mediaSize = mediaSize,
        dataOffset = dataOffset,
        dataSize = dataSize,
        manifestOffset = manifestOffset,
        manifestSize = manifestSize,
        blockIndexOffset = blockIndexOffset,
        blockCount = blockCount,
        integrityOffset = integrityOffset,
        integritySize = integritySize,
        catalogSha256 = catalogSha256,
        manifestSha256 = manifestSha256,
        indexSha256 = indexSha256,
        generation = generation,
        checkSize = checkSize,
        headerCrc32c = headerCrc32c,
    )
}

fun mountArchive(file: File): Pair<NxHeader, ByteArray> {
    val data = file.readBytes()
    require(data.size >= HEADER_SLOT_B_OFFSET + HEADER_SIZE) { "File too small" }

    // Try slot A (offset 0)
    var header: NxHeader? = null
    try {
        header = parseHeader(data, 0)
    } catch (_: Exception) { /* ignore */ }

    // Try slot B (offset 4096)
    var headerB: NxHeader? = null
    try {
        headerB = parseHeader(data, HEADER_SLOT_B_OFFSET)
    } catch (_: Exception) { /* ignore */ }

    // Select valid slot with highest generation
    val selected = when {
        header != null && headerB != null ->
            if (header.generation >= headerB.generation) header else headerB
        header != null -> header
        headerB != null -> headerB
        else -> throw IllegalArgumentException("No valid header slot found")
    }

    return selected to data
}

// --- Catalog parsing ---
fun parseCatalog(data: ByteArray, offset: Long, size: Long): NxCatalog {
    val o = offset.toInt()
    require(data.slice(o, 4).contentEquals(NX_CT_MAGIC)) { "Invalid catalog magic" }
    val appCount = data.readU32(o + 20).toInt()
    val progCount = data.readU32(o + 24).toInt()
    val snapCount = data.readU32(o + 28).toInt()
    val appTableOffset = data.readU64(o + 48).toInt()
    val snapTableOffset = data.readU64(o + 64).toInt()
    val stringPoolOffset = data.readU64(o + 96).toInt()
    val stringPoolSize = data.readU64(o + 104).toInt()

    val stringPool = data.slice(stringPoolOffset, stringPoolSize.toInt())

    fun readStringRef(off: Int, len: Int): String? {
        if (off == 0xFFFFFFFF || len == 0) return null
        val bytes = data.slice(stringPoolOffset + off, len)
        return String(bytes, StandardCharsets.UTF_8)
    }

    val applications = (0 until appCount).map { i ->
        val appO = appTableOffset + i * 80
        NxApplication(
            applicationId = data.readU64(appO).toInt().toLong() and 0xFFFFFFFFL,
            platform = data[appO + 8].toInt() and 0xFF,
            flags = data[appO + 9].toInt() and 0xFF,
            firstProgramIndex = data.readU32(appO + 12),
            programCount = data.readU32(appO + 16),
            firstSnapshotIndex = data.readU32(appO + 20),
            snapshotCount = data.readU32(appO + 24),
            defaultTitle = readStringRef(data.readU32(appO + 28), data.readU32(appO + 32)),
            publisher = readStringRef(data.readU32(appO + 36), data.readU32(appO + 40)),
            metadataSnapshotIndex = data.readU32(appO + 68),
        )
    }

    val snapshots = (0 until snapCount).map { i ->
        val snapO = snapTableOffset + i * 120
        NxSnapshot(
            programIndex = data.readU32(snapO),
            snapshotType = data[snapO + 4].toInt() and 0xFF,
            numericVersion = data.readU32(snapO + 8),
            contentId = if (data.slice(snapO + 32, 16).all { it == 0 }) null else data.slice(snapO + 32, 16),
            buildId = if (data.slice(snapO + 88, 32).all { it == 0 }) null else data.slice(snapO + 88, 32),
            snapshotIdentity = data.slice(snapO + 56, 32),
        )
    }

    return NxCatalog(applications, snapshots)
}

// --- Manifest parsing ---
fun parseManifest(data: ByteArray, offset: Long, size: Long): NxManifest {
    val o = offset.toInt()
    require(data.slice(o, 4).contentEquals(NX_MF_MAGIC)) { "Invalid manifest magic" }
    val dirCount = data.readU32(o + 16).toInt()
    val fileCount = data.readU32(o + 20).toInt()
    val fileTableOffset = data.readU64(o + 40).toInt()

    val files = (0 until fileCount).map { i ->
        val fileO = fileTableOffset + i * 64
        NxFile(
            logicalSize = data.readU64(fileO),
            chunkSizeLog2 = data[fileO + 16].toInt() and 0xFF,
            contentSha256 = data.slice(fileO + 20, 32),
        )
    }

    return NxManifest(dirCount, fileCount, files)
}

// --- High-level archive info ---
fun inspectArchive(file: File): NxArchiveInfo {
    val (header, data) = mountArchive(file)
    val catalog = parseCatalog(data, header.catalogOffset, header.catalogSize)
    val manifest = if (header.manifestOffset != 0L && header.manifestSize != 0L) {
        parseManifest(data, header.manifestOffset, header.manifestSize)
    } else {
        NxManifest(0, 0, emptyList())
    }
    return NxArchiveInfo(header, catalog, manifest, catalog.snapshots.size)
}