package com.cryptowallet.app.ui.components

import android.graphics.Color
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCode(data: String, size: Dp) {
    val imageBitmap = remember(data) {
        try {
            val writer = QRCodeWriter()
            val matrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    val black = matrix[x, y]
                    bitmap.setPixel(x, y, if (black) Color.BLACK else Color.WHITE)
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = "Código QR", modifier = Modifier.size(size))
    }
}
