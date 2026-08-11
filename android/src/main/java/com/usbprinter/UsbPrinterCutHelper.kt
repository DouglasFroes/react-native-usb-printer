package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.facebook.react.bridge.WritableMap

object UsbPrinterCutHelper {
    private const val TAG = "UsbPrinterCutHelper"

    fun printCut(context: Context, tailingLine: Boolean, beep: Boolean, device: UsbDevice): WritableMap {
        Log.d(TAG, "Iniciando operação de corte (tailingLine: $tailingLine, beep: $beep)")

        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            Logger.logError(context, TAG, "Falha ao conectar com a impressora para corte")
            return UsbConnectionHelper.createErrorResponse("Falha ao conectar com a impressora para corte")
        }

        try {
            // Inicializa a impressora
            if (!UsbConnectionHelper.initializePrinter(connectionData.connection, connectionData.endpoint)) {
                return UsbConnectionHelper.createErrorResponse("Falha na inicialização da impressora para corte")
            }

            val commands = mutableListOf<Byte>()

            // Adiciona beep se solicitado
            if (beep) {
                Log.d(TAG, "Adicionando comando de beep")
                commands.addAll(listOf(0x1B, 0x42, 0x03, 0x01).map { it.toByte() }) // ESC B (beep)
            }

            // Alimenta papel suficiente e adiciona o comando de corte
            Log.d(TAG, "Adicionando alimentação de papel e comando de corte")
            UsbConnectionHelper.appendCutCommand(commands, extraFeed = tailingLine)

            Log.i(TAG, "Enviando ${commands.size} bytes para operação de corte")
            val success = UsbConnectionHelper.sendDataInChunks(
                connectionData.connection,
                connectionData.endpoint,
                commands.toByteArray()
            )

            return if (success) {
                UsbConnectionHelper.awaitCutMechanism()
                UsbConnectionHelper.createSuccessResponse("Corte realizado com sucesso")
            } else {
                UsbConnectionHelper.createErrorResponse("Falha ao enviar comando de corte")
            }

        } catch (e: Exception) {
            Logger.logError(context, TAG, "Erro ao realizar corte", e)
            return UsbConnectionHelper.createErrorResponse("Erro ao cortar: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
    }
}
