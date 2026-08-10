package com.cyebrcina.pos.core.util

import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.data.remote.model.DeviceOrderType

data class StatusPresentation(val label: String, val tone: BadgeTone)

fun DeviceOrderType.toBadge(): StatusPresentation = when (this) {
    DeviceOrderType.DELIVERY -> StatusPresentation("Delivery", BadgeTone.INFO)
    DeviceOrderType.COLLECTION -> StatusPresentation("Collection", BadgeTone.PENDING)
    DeviceOrderType.DINE_IN -> StatusPresentation("Dine In", BadgeTone.SUCCESS)
}
