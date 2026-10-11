// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.support

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import com.arinara.fotara.R

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isExiting by remember { mutableStateOf(false) }
    val safeBack = {
        if (!isExiting) {
            isExiting = true
            onBack()
        }
    }
    val qrisBitmap = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.qris_code)
            ?: generateQrisBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Support Fotara",
                    color = TabCream,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = safeBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // QRIS Image Card
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, AccentGold, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrisBitmap.asImageBitmap(),
                    contentDescription = "QRIS Donation Code",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Download Button
            Button(
                onClick = {
                    saveQrisToGallery(context, qrisBitmap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Download QRIS",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun generateQrisBitmap(): Bitmap {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawColor(AndroidColor.WHITE)

    // Header QRIS text
    paint.color = AndroidColor.RED
    paint.textSize = 32f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("QRIS", size / 2f, 50f, paint)

    paint.color = AndroidColor.DKGRAY
    paint.textSize = 14f
    paint.isFakeBoldText = false
    canvas.drawText("NATIONAL STANDARD QR CODE", size / 2f, 75f, paint)

    // QR grid pattern
    paint.color = AndroidColor.BLACK
    val margin = 90f
    val qrSize = size - 2 * margin
    val cells = 25
    val cellSize = qrSize / cells

    val seededPattern = listOf(
        "11111110010101001111111",
        "10000010101100101000001",
        "10111010011010101011101",
        "10111010100101001011101",
        "10111010011110101011101",
        "10000010101001101000001",
        "11111110101010101111111",
        "00000000110100100000000",
        "11011011001011011011011",
        "00100100111100100100100",
        "11101011010101110101110",
        "01010100101010010101010",
        "10110101110101101101011",
        "00000000110101000000000",
        "11111110101101001111111",
        "10000010010100101000001",
        "10111010111010101011101",
        "10111010001101001011101",
        "10111010110101101011101",
        "10000010001001001000001",
        "11111110111010101111111"
    )

    for (r in seededPattern.indices) {
        val row = seededPattern[r]
        for (c in row.indices) {
            if (row[c] == '1') {
                val left = margin + c * cellSize
                val top = margin + r * cellSize
                canvas.drawRect(left, top, left + cellSize, top + cellSize, paint)
            }
        }
    }

    // Merchant text footer
    paint.color = AndroidColor.BLACK
    paint.textSize = 16f
    paint.isFakeBoldText = true
    canvas.drawText("ARINARA / FOTARA SUPPORT", size / 2f, size - 30f, paint)

    return bitmap
}

private fun saveQrisToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "Fotara_QRIS_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Fotara")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, "QRIS saved to Pictures/Fotara", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val fotaraDir = File(picturesDir, "Fotara").apply { if (!exists()) mkdirs() }
        val file = File(fotaraDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Toast.makeText(context, "QRIS saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save QRIS: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
