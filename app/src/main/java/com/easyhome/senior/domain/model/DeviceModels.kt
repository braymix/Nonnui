package com.easyhome.senior.domain.model

data class BatteryStatus(val percent: Int, val isCharging: Boolean) {
    val isLow: Boolean get() = percent < LOW_THRESHOLD && !isCharging

    companion object {
        const val LOW_THRESHOLD = 20
    }
}

data class InstalledApp(val packageName: String, val label: String)

data class PickedContact(val contactId: Long, val name: String, val phoneNumber: String)
