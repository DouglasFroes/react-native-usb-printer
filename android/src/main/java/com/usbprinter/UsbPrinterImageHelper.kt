package com.usbprinter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.util.Log
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import android.net.Uri
import android.util.Base64
import java.net.URL
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import com.usbprinter.UtilsImage
import com.usbprinter.UsbConnectionHelper

object UsbPrinterImageHelper {
    private const val TAG = "UsbPrinterImageHelper"

    /**
     * Imprime uma imagem a partir de um base64 PNG/JPG.
     */
    fun printImageBase64(context: Context, options: com.facebook.react.bridge.ReadableMap, device: UsbDevice): WritableMap {
        return try {
            val base64Image = options.getString("base64Image") ?: ""
            val align = if (options.hasKey("align")) options.getString("align") else null
            val pageWidthMm = if (options.hasKey("pageWidth")) options.getInt("pageWidth") else null
            val pageWidthPx = pageWidthMm?.let { (it * 7.2).toInt() }
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null && pageWidthPx != null && bitmap.width != pageWidthPx) {
                val aspect = bitmap.height.toFloat() / bitmap.width
                val newHeight = (pageWidthPx * aspect).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, pageWidthPx, newHeight, true)
            }
            printBitmap(context, bitmap, device, align)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao decodificar imagem base64: ${e.localizedMessage}")
            result
        }
    }

    /**
     * Imprime uma imagem a partir de uma URI (content:// ou file://).
     */
    fun printImageUri(context: Context, options: com.facebook.react.bridge.ReadableMap, device: UsbDevice): WritableMap {
        return try {
            val imageUri = options.getString("imageUri") ?: ""
            val align = if (options.hasKey("align")) options.getString("align") else null
            val pageWidthMm = if (options.hasKey("pageWidth")) options.getInt("pageWidth") else null
            val pageWidthPx = pageWidthMm?.let { (it * 7.2).toInt() }
            val uri = Uri.parse(imageUri)
            val inputStream = when {
                imageUri.startsWith("https://") || imageUri.startsWith("http://") -> {
                    val url = URL(imageUri)
                    val connection = url.openConnection()
                    connection.connect()
                    val input = connection.getInputStream()
                    val tempFile = File.createTempFile("usbprinter_img", ".tmp", context.cacheDir)
                    val output = FileOutputStream(tempFile)
                    input.copyTo(output)
                    output.close()
                    input.close()
                    context.contentResolver.openInputStream(Uri.fromFile(tempFile))
                }
                else -> context.contentResolver.openInputStream(uri)
            }
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null && pageWidthPx != null && bitmap.width != pageWidthPx) {
                val aspect = bitmap.height.toFloat() / bitmap.width
                val newHeight = (pageWidthPx * aspect).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, pageWidthPx, newHeight, true)
            }
            printBitmap(context, bitmap, device, align)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao carregar imagem da URI: ${e.localizedMessage}")
            result
        }
    }

    /**
     * Converte o Bitmap para comandos ESC/POS e envia para a impressora.
     */
    fun printBitmap(context: Context, bitmap: Bitmap?, device: UsbDevice, align: String? = null): WritableMap {
        val result = Arguments.createMap()
        if (bitmap == null) {
            result.putBoolean("success", false)
            result.putString("message", "Bitmap inválido ou nulo.")
            return result
        }

        // Estabelece a conexão usando o Helper unificado
        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            result.putBoolean("success", false)
            result.putString("message", "Falha ao conectar com a impressora para imagem")
            return result
        }

        try {
            val commands = ByteArrayOutputStream()

            // Comando de inicialização ESC/POS (ESC @)
            commands.write(byteArrayOf(0x1B, 0x40))

            // Configuração de alinhamento
            val alignCommands = when (align) {
                "center" -> byteArrayOf(0x1B, 0x61, 0x01)
                "right" -> byteArrayOf(0x1B, 0x61, 0x02)
                else -> byteArrayOf(0x1B, 0x61, 0x00) // left
            }
            commands.write(alignCommands)

            // Configuração de espaçamento de linha (ESC 3 24)
            val setLineSpace24 = byteArrayOf(0x1B, 0x33, 24)
            commands.write(setLineSpace24)

            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            val pixels = UtilsImage.getPixelsSlow(bitmap, imageWidth, imageHeight)

            Log.d(TAG, "Compiling image ${imageWidth}x${imageHeight} into ESC/POS commands")

            // Para cada fatia vertical de 24 linhas
            for (y in 0 until imageHeight step 24) {
                // Comando ESC * para modo bit image
                val nL = (imageWidth and 0xFF).toByte()
                val nH = ((imageWidth shr 8) and 0xFF).toByte()
                val selectBitImageMode = byteArrayOf(0x1B, 0x2A, 33, nL, nH)
                commands.write(selectBitImageMode)

                // Para cada coluna da imagem, escreve o slice de 3 bytes
                for (x in 0 until imageWidth) {
                    val slice = UtilsImage.recollectSlice(y, x, pixels)
                    commands.write(slice)
                }

                // Line feed após cada fatia
                commands.write(0x0A)
            }

            // Restaura espaçamento de linha normal (ESC 3 32)
            val setLineSpace32 = byteArrayOf(0x1B, 0x33, 32)
            commands.write(setLineSpace32)
            commands.write(0x0A)

            // Envia o payload completo em chunks usando o Helper unificado
            Log.d(TAG, "Sending compiled image commands of size ${commands.size()} bytes")
            val success = UsbConnectionHelper.sendDataInChunks(
                connectionData.connection,
                connectionData.endpoint,
                commands.toByteArray()
            )

            if (success) {
                result.putBoolean("success", true)
                result.putString("message", "Imagem impressa com sucesso.")
            } else {
                result.putBoolean("success", false)
                result.putString("message", "Falha ao enviar os dados da imagem.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error printing image", e)
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir imagem: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
        return result
    }
}
