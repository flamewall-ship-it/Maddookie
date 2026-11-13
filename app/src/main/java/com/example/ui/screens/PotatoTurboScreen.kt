package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MemorySnapshot
import com.example.data.model.PotatoTurboConfig
import com.example.ui.theme.TurboLime
import kotlin.math.roundToInt

@Composable
fun PotatoTurboScreen(
    config: PotatoTurboConfig,
    memory: MemorySnapshot,
    onUpdateConfig: ((PotatoTurboConfig) -> PotatoTurboConfig) -> Unit,
    onRefreshTelemetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("potato_turbo_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = TurboLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Potato-Turbo Engine", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Low-RAM & Zero-Blur GPU Optimization",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshTelemetry,
                    modifier = Modifier.testTag("refresh_telemetry_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh memory telemetry")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, TurboLime.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = TurboLime)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Memory Telemetry", style = MaterialTheme.typography.titleMedium)
                    }

                    val heapRatio = (memory.appUsedHeapMb.toFloat() / memory.appMaxHeapMb.toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "App Heap: ${memory.appUsedHeapMb} MB / ${memory.appMaxHeapMb} MB",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { heapRatio },
                        color = TurboLime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    val sysUsed = (memory.deviceTotalMb - memory.deviceAvailableMb).coerceAtLeast(0L)
                    val sysRatio = (sysUsed.toFloat() / memory.deviceTotalMb.toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "Device RAM: $sysUsed MB used / ${memory.deviceTotalMb} MB total" +
                            if (memory.isLowRamDevice) " (Low-RAM Tier)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { sysRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TurboToggleRow(
                        title = "RGB_565 Bitmap Decoding",
                        subtitle = "Cuts poster & banner memory footprint by 50% in Coil image loader.",
                        checked = config.rgb565Enabled,
                        testTag = "turbo_rgb565_switch",
                        onCheckedChange = { v -> onUpdateConfig { it.copy(rgb565Enabled = v) } }
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Heap Memory Cache Cap", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${config.heapCapPercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = TurboLime
                            )
                        }
                        Text(
                            "Caps Coil in-memory bitmap cache at ${config.heapCapPercent}% of app heap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = config.heapCapPercent.toFloat(),
                            onValueChange = { raw ->
                                val stepped = raw.roundToInt().coerceIn(6, 25)
                                onUpdateConfig { it.copy(heapCapPercent = stepped) }
                            },
                            valueRange = 6f..25f,
                            modifier = Modifier.testTag("turbo_heap_slider")
                        )
                    }

                    TurboToggleRow(
                        title = "Deferred Background WebView Init",
                        subtitle = "Prevents Chromium WebView cold-start stalls until fallback providers require it.",
                        checked = config.deferredWebViewEnabled,
                        testTag = "turbo_webview_switch",
                        onCheckedChange = { v -> onUpdateConfig { it.copy(deferredWebViewEnabled = v) } }
                    )

                    TurboToggleRow(
                        title = "Zero-Blur GPU Fast Path",
                        subtitle = "Replaces render-effect blurs and crossfades with solid obsidian layers for zero-jank scrolling.",
                        checked = config.zeroBlurFastPath,
                        testTag = "turbo_zeroblur_switch",
                        onCheckedChange = { v -> onUpdateConfig { it.copy(zeroBlurFastPath = v) } }
                    )

                    TurboToggleRow(
                        title = "Dark Obsidian Theme",
                        subtitle = "OLED-friendly midnight surfaces for night viewing.",
                        checked = config.darkTheme,
                        testTag = "turbo_darktheme_switch",
                        onCheckedChange = { v -> onUpdateConfig { it.copy(darkTheme = v) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun TurboToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
