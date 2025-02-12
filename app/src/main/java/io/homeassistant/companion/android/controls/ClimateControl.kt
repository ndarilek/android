package io.homeassistant.companion.android.controls

import android.content.Context
import android.os.Build
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.RangeTemplate
import androidx.annotation.RequiresApi
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import kotlinx.coroutines.runBlocking
import io.homeassistant.companion.android.common.R as commonR

@RequiresApi(Build.VERSION_CODES.R)
class ClimateControl {
    companion object : HaControl {
        override fun provideControlFeatures(
            context: Context,
            control: Control.StatefulBuilder,
            entity: Entity<Map<String, Any>>,
            area: AreaRegistryResponse?
        ): Control.StatefulBuilder {
            control.setStatusText(
                when (entity.state) {
                    "auto" -> context.getString(commonR.string.state_auto)
                    "cool" -> context.getString(commonR.string.state_cool)
                    "dry" -> context.getString(commonR.string.state_dry)
                    "fan_only" -> context.getString(commonR.string.state_fan_only)
                    "heat" -> context.getString(commonR.string.state_heat)
                    "heat_cool" -> context.getString(commonR.string.state_heat_cool)
                    "off" -> context.getString(commonR.string.state_off)
                    "unavailable" -> context.getString(commonR.string.state_unavailable)
                    else -> entity.state
                }
            )
            val minValue = (entity.attributes["min_temp"] as? Number)?.toFloat() ?: 0f
            val maxValue = (entity.attributes["max_temp"] as? Number)?.toFloat() ?: 100f
            var currentValue = (entity.attributes["temperature"] as? Number)?.toFloat() ?: (
                entity.attributes["current_temperature"] as? Number
                )?.toFloat() ?: 0f
            // Ensure the current value is never lower than the minimum or higher than the maximum
            if (currentValue < minValue)
                currentValue = minValue
            if (currentValue > maxValue)
                currentValue = maxValue
            control.setControlTemplate(
                RangeTemplate(
                    entity.entityId,
                    minValue,
                    maxValue,
                    currentValue,
                    .5f,
                    "%.0f"
                )

            )
            return control
        }

        override fun getDeviceType(entity: Entity<Map<String, Any>>): Int =
            DeviceTypes.TYPE_AC_HEATER

        override fun getDomainString(context: Context, entity: Entity<Map<String, Any>>): String =
            context.getString(commonR.string.domain_climate)

        override fun performAction(
            integrationRepository: IntegrationRepository,
            action: ControlAction
        ): Boolean {
            return runBlocking {
                integrationRepository.callService(
                    action.templateId.split(".")[0],
                    "set_temperature",
                    hashMapOf(
                        "entity_id" to action.templateId,
                        "temperature" to (action as? FloatAction)?.newValue.toString()
                    )
                )
                return@runBlocking true
            }
        }
    }
}
