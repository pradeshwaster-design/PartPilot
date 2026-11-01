package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IbmPlexMonoFamily
import com.example.ui.theme.WorkshopBorder
import com.example.ui.theme.WorkshopText
import java.util.Random

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 180
) {
    val matrixSize = 21 // standard Version 1 QR code is 21x21 modules
    val matrix = remember(data) {
        generateQrMatrix(data, matrixSize)
    }

    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, WorkshopBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val moduleSize = size.width / matrixSize

            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (matrix[row][col]) {
                        drawRect(
                            color = WorkshopText,
                            topLeft = Offset(col * moduleSize, row * moduleSize),
                            size = Size(moduleSize, moduleSize)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = data,
            fontFamily = IbmPlexMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = WorkshopText
        )
    }
}

private fun generateQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    // Finder patterns in top-left, top-right, bottom-left
    fun placeFinder(r: Int, c: Int) {
        for (i in 0..6) {
            for (j in 0..6) {
                val isBorder = i == 0 || i == 6 || j == 0 || j == 6
                val isCenter = i in 2..4 && j in 2..4
                matrix[r + i][c + j] = isBorder || isCenter
            }
        }
    }

    placeFinder(0, 0)
    placeFinder(0, size - 7)
    placeFinder(size - 7, 0)

    // Timing patterns
    for (i in 8 until size - 8) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Deterministic data fill based on string hash
    val random = Random(data.hashCode().toLong())
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder1 = r < 8 && c < 8
            val inFinder2 = r < 8 && c >= size - 8
            val inFinder3 = r >= size - 8 && c < 8
            val inTiming = r == 6 || c == 6

            if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                matrix[r][c] = random.nextBoolean()
            }
        }
    }

    return matrix
}
