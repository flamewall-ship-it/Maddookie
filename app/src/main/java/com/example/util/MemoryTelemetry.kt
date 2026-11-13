package com.example.util

import android.app.ActivityManager
import android.content.Context
import com.example.data.model.MemorySnapshot

object MemoryTelemetry {
    fun capture(context: Context): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
        val maxHeapMb = runtime.maxMemory() / (1024L * 1024L)

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        return MemorySnapshot(
            appUsedHeapMb = usedHeapMb.coerceAtLeast(1L),
            appMaxHeapMb = maxHeapMb.coerceAtLeast(1L),
            deviceAvailableMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L),
            deviceTotalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L),
            isLowRamDevice = am?.isLowRamDevice == true
        )
    }
}
