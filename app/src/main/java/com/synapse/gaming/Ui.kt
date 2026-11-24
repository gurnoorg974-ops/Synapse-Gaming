package com.synapse.gaming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Floating pill navigation: the selected tab expands and lights up
@Composable
fun FloatingNav(current: Tab, onTab: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xF2151722),
            border = BorderStroke(1.dp, Color(0x26FFFFFF))
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tab.entries.forEach { t ->
                    val sel = t == current
                    Box(
                        Modifier.weight(if (sel) 1.9f else 1f).height(48.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(if (sel) SynYellow else Color.Transparent)
                            .clickable { onTab(t) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(t.icon, t.label, tint = if (sel) Color.Black else SynMuted, modifier = Modifier.size(22.dp))
                            if (sel) {
                                Spacer(Modifier.width(6.dp))
                                Text(t.label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
