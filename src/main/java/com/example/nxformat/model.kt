package com.example.nxformat.model

enum class Platform(val id: Int) {
    Unknown(0), Switch1(1), Switch2(2)
}

enum class SnapshotType(val id: Int) {
    Base(0), Update(1), Dlc(2), StandaloneUpdate(3), Unknown(4)
}

enum class MediaType(val id: Int) {
    PngIcon(1), JpegIcon(2), Npdm(3), Nacp(4), Opaque(5)
}

data class FileInput(
    val path: String,
    val size: Long,
    val sha256: ByteArray? = null,
)

data class DirectoryInput(
    val name: String,
    val entries: List<EntryInput>,
)

sealed class EntryInput {
    data class Directory(val dir: DirectoryInput) : EntryInput()
    data class File(val file: FileInput) : EntryInput()
}

data class SnapshotInput(
    val snapshotType: SnapshotType,
    val numericVersion: Int,
    val contentId: ByteArray?,
    val addonTitleId: Long?,
    val buildId: ByteArray?,
    val dependencies: List<DependencyInput>,
    val title: String?,
    val publisher: String?,
    val root: DirectoryInput,
)

data class DependencyInput(
    val kind: Int, // 1=required base, 2=requires, 3=recommends
    val targetSnapshotIdentity: ByteArray,
)

data class ProgramInput(
    val programId: Long,
    val kind: Int,
    val contentId: ByteArray?,
)

data class ApplicationInput(
    val applicationId: Long,
    val platform: Platform,
    val programs: List<ProgramInput>,
    val snapshots: List<SnapshotInput>,
)

data class ArchiveInput(
    val applications: List<ApplicationInput>,
)

data class SnapshotSummary(
    val key: String,
    val applicationId: Long,
    val platform: String,
    val snapshotType: String,
    val version: Int,
    val identity: String, // hex
)

data class ArchiveSummary(
    val applications: List<ApplicationSummary>,
    val snapshotCount: Int,
    val totalSize: Long,
)

data class ApplicationSummary(
    val applicationId: Long,
    val platform: String,
    val snapshots: List<SnapshotSummary>,
)

data class NxApplication(
    val applicationId: Long,
    val platform: Int,
    val flags: Int,
    val firstProgramIndex: Int,
    val programCount: Int,
    val firstSnapshotIndex: Int,
    val snapshotCount: Int,
    val defaultTitle: String?,
    val publisher: String?,
    val metadataSnapshotIndex: Int,
)

data class NxSnapshot(
    val programIndex: Int,
    val snapshotType: Int,
    val numericVersion: Int,
    val contentId: ByteArray?,
    val buildId: ByteArray?,
    val snapshotIdentity: ByteArray,
)

data class NxCatalog(
    val applications: List<NxApplication>,
    val snapshots: List<NxSnapshot>,
)

data class NxFile(
    val logicalSize: Long,
    val chunkSizeLog2: Int,
    val contentSha256: ByteArray,
)

data class NxManifest(
    val directoryCount: Int,
    val fileCount: Int,
    val files: List<NxFile>,
)

data class NxHeader(
    val magic: ByteArray,
    val majorVersion: Int,
    val minorVersion: Int,
    val archiveSize: Long,
    val catalogOffset: Long,
    val catalogSize: Long,
    val mediaOffset: Long,
    val mediaSize: Long,
    val dataOffset: Long,
    val dataSize: Long,
    val manifestOffset: Long,
    val manifestSize: Long,
    val blockIndexOffset: Long,
    val blockCount: Long,
    val integrityOffset: Long,
    val integritySize: Long,
    val catalogSha256: ByteArray,
    val manifestSha256: ByteArray,
    val indexSha256: ByteArray,
    val generation: Long,
    val checkSize: Int,
    val headerCrc32c: Int,
)

data class NxArchiveInfo(
    val header: NxHeader,
    val catalog: NxCatalog,
    val manifest: NxManifest,
    val snapshotCount: Int,
)