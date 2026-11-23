package com.synapse.gaming

import android.os.Build

object SocInfo {
    val model: String get() = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else Build.HARDWARE
    val isSnapdragon: Boolean get() =
        Build.HARDWARE.equals("qcom", true) ||
        (Build.VERSION.SDK_INT >= 31 && Build.SOC_MANUFACTURER.equals("Qualcomm", true))
}
