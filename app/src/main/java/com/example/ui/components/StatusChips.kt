package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComputedPartStatus
import com.example.data.model.PartCondition
import com.example.ui.theme.*

@Composable
fun StatusChip(
    status: ComputedPartStatus,
    modifier: Modifier = Modifier
) {
    val (bg, text) = when (status) {
        ComputedPartStatus.HAVE -> Pair(ChipHaveBg, ChipHaveText)
        ComputedPartStatus.NOT_ENOUGH -> Pair(ChipLowBg, ChipLowText)
        ComputedPartStatus.NEED -> Pair(ChipNeedBg, ChipNeedText)
        ComputedPartStatus.IN_CART -> Pair(ChipCartBg, ChipCartText)
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.displayName.uppercase(),
            color = text,
            fontFamily = IbmPlexSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ConditionChip(
    condition: PartCondition,
    modifier: Modifier = Modifier
) {
    val (bg, text) = when (condition) {
        PartCondition.WORKING -> Pair(ChipHaveBg, ChipHaveText)
        PartCondition.NOT_TESTED, PartCondition.UNKNOWN -> Pair(WorkshopBorder.copy(alpha = 0.5f), WorkshopTextMuted)
        PartCondition.PARTIALLY_WORKING -> Pair(ChipLowBg, ChipLowText)
        PartCondition.FAULTY, PartCondition.DAMAGED -> Pair(ChipNeedBg, ChipNeedText)
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = condition.displayName,
            color = text,
            fontFamily = IbmPlexSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}

@Composable
fun LocationBadge(
    code: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(WorkshopBg, RoundedCornerShape(6.dp))
            .border(1.dp, WorkshopBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            color = WorkshopTealDark,
            fontFamily = IbmPlexMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun QuantityBadge(
    quantity: Int,
    freeStock: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(WorkshopBg, RoundedCornerShape(6.dp))
            .border(1.dp, WorkshopBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$quantity in stock",
            color = WorkshopText,
            fontFamily = IbmPlexMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        if (freeStock != null && freeStock < quantity) {
            Text(
                text = " ($freeStock free)",
                color = WorkshopTeal,
                fontFamily = IbmPlexMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}
