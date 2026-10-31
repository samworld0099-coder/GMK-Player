package com.example.transfer

import com.example.data.model.MediaType

enum class TransferStatus {
    PENDING,
    CONNECTING,
    WAITING_APPROVAL,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TransferItem(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mediaType: MediaType = MediaType.OTHER,
    val sha256: String = "",
    var bytesTransferred: Long = 0L,
    var status: TransferStatus = TransferStatus.PENDING,
    var errorMessage: String? = null
)

data class TransferSession(
    val sessionId: String,
    val isSender: Boolean,
    val peerName: String,
    val peerAddress: String,
    val port: Int,
    val items: List<TransferItem>,
    val totalBytes: Long,
    var transferredBytes: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var etaSeconds: Long = 0L,
    var status: TransferStatus = TransferStatus.CONNECTING,
    var errorMessage: String? = null
)

data class ReceivedFileRecord(
    val name: String,
    val path: String,
    val size: Long,
    val timestamp: Long,
    val checksum: String = ""
)
