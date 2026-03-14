package com.videorelay.app.domain.model

data class Profile(
    val pubkey: String,
    val name: String = "",
    val displayName: String = "",
    val picture: String = "",
    val banner: String = "",
    val about: String = "",
    val lud16: String = "",
    val lud06: String = "",
    val nip05: String = "",
    val fetchedAt: Long = System.currentTimeMillis() / 1000,
) {
    val bestName: String
        get() = displayName.ifBlank { name.ifBlank { pubkey.take(8) + "..." + pubkey.takeLast(4) } }
}
