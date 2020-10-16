package io.homeassistant.companion.android.controls

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.ToggleRangeTemplate
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.webview.WebViewActivity
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.R)
class FanControl {
    companion object : HaControl {
        override fun createControl(
            context: Context,
            entity: Entity<Map<String, Any>>
        ): Control {
            val speeds = entity.attributes["speed_list"].toString()
                .removeSurrounding("[", "]")
                .split(", ")
            val currentSpeed: Int? = entity.attributes["speed"].toString().toIntOrNull()

            val control = Control.StatefulBuilder(
                entity.entityId,
                PendingIntent.getActivity(
                    context,
                    0,
                    WebViewActivity.newInstance(context),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
            control.setTitle(entity.attributes["friendly_name"].toString())
            control.setDeviceType(DeviceTypes.TYPE_FAN)
            control.setStatus(Control.STATUS_OK)
            if (currentSpeed != null) {
                control.setControlTemplate(
                    ToggleRangeTemplate(
                        entity.entityId,
                        entity.state != "off",
                        "",
                        RangeTemplate(
                            entity.entityId,
                            0f,
                            speeds.size.toFloat() - 1,
                            speeds.indexOf(currentSpeed.toString()).toFloat(),
                            1f,
                            ""
                        )
                    )
                )
            } else {
                control.setControlTemplate(
                    ToggleTemplate(
                        entity.entityId,
                        ControlButton(
                            entity.state != "off",
                            ""
                        )
                    )
                )
            }
            return control.build()
        }

        override fun performAction(
            integrationRepository: IntegrationRepository,
            action: ControlAction
        ): Boolean {
            return runBlocking {
                when (action) {
                    is BooleanAction -> {
                        integrationRepository.callService(
                            action.templateId.split(".")[0],
                            if (action.newState) "turn_on" else "turn_off",
                            hashMapOf("entity_id" to action.templateId)
                        )
                    }
                    is FloatAction -> {
                        val speeds = integrationRepository.getEntity(action.templateId)
                            .attributes["speed_list"].toString()
                            .removeSurrounding("[", "]")
                            .split(", ")
                        integrationRepository.callService(
                            action.templateId.split(".")[0],
                            "set_speed",
                            hashMapOf(
                                "entity_id" to action.templateId,
                                "speed" to speeds[action.newValue.toInt()]
                            )
                        )
                    }
                }
                return@runBlocking true
            }
        }
    }
}
