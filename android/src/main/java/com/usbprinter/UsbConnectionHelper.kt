package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

object UsbConnectionHelper {
    private const val TAG = "UsbConnectionHelper"

    data class ConnectionData(
        val connection: UsbDeviceConnection,
        val endpoint: UsbEndpoint,
        val usbInterface: UsbInterface
    )

    /**
     * Estabelece conexão robusta com impressora térmica USB.
     * Tenta todas as interfaces e endpoints OUT disponíveis, com até 3 tentativas.
     */
    fun establishPrinterConnection(context: Context, device: UsbDevice): ConnectionData? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        var connection: UsbDeviceConnection? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                connection = usbManager.openDevice(device)
                if (connection == null) {
                    Log.w(TAG, "Failed to open USB device (attempt $attempt/$maxRetries)")
                    if (attempt < maxRetries) {
                        Thread.sleep(150)
                        continue
                    }
                    return null
                }

                Log.d(TAG, "Attempting to connect to thermal printer with ${device.interfaceCount} interfaces (attempt $attempt)")

                // Tenta várias interfaces para máxima compatibilidade
                for (interfaceIndex in 0 until device.interfaceCount) {
                    val usbInterface = device.getInterface(interfaceIndex)
                    Log.d(TAG, "Trying interface $interfaceIndex with ${usbInterface.endpointCount} endpoints")

                    // Procura endpoint OUT para impressão
                    for (endpointIndex in 0 until usbInterface.endpointCount) {
                        val endpoint = usbInterface.getEndpoint(endpointIndex)
                        if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                            Log.d(TAG, "Found OUT endpoint at interface $interfaceIndex")

                            if (connection.claimInterface(usbInterface, true)) {
                                Log.i(TAG, "Successfully claimed interface for thermal printer on attempt $attempt")

                                // Envia comando de inicialização ESC @
                                initializePrinter(connection, endpoint)

                                return ConnectionData(connection, endpoint, usbInterface)
                            } else {
                                Log.w(TAG, "Failed to claim interface $interfaceIndex")
                            }
                        }
                    }
                }

                // Se falhou em encontrar ou fazer claim
                Log.e(TAG, "No suitable interface found or claimed (attempt $attempt/$maxRetries)")
                connection.close()
                connection = null
                if (attempt < maxRetries) {
                    Thread.sleep(150)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error establishing connection to thermal printer (attempt $attempt/$maxRetries)", e)
                try {
                    connection?.close()
                } catch (_: Exception) {}
                connection = null
                if (attempt < maxRetries) {
                    Thread.sleep(150)
                }
            }
        }
        return null
    }

    /**
     * Inicializa a impressora com comandos de recuperação e ESC @ (reset).
     * Envia comandos de tempo real para destravar a impressora caso ela esteja aguardando dados.
     */
    fun initializePrinter(connection: UsbDeviceConnection, endpoint: UsbEndpoint): Boolean {
        try {
            Log.d(TAG, "Initializing thermal printer with unfreeze sequence and ESC @")
            
            // Sequência de destravamento de hardware:
            // 1. DLE ENQ 2 (0x10, 0x05, 0x02) -> Recuperação em tempo real e limpa buffer
            // 2. DLE ENQ 1 (0x10, 0x05, 0x01) -> Recuperação em tempo real de erro
            // 3. CAN (0x18) -> Cancela dados de impressão anteriores (evita que o parser fique preso)
            // 4. ESC @ (0x1B, 0x40) -> Inicializa a impressora (Reset padrão)
            val initCommand = byteArrayOf(
                0x10, 0x05, 0x02,
                0x10, 0x05, 0x01,
                0x18,
                0x1B, 0x40
            )
            
            val bytesTransferred = connection.bulkTransfer(endpoint, initCommand, initCommand.size, 1500)
            if (bytesTransferred >= 0) {
                Thread.sleep(150) // Pequeno delay adicional para processar a sequência
                Log.d(TAG, "Thermal printer initialized and unfrozen successfully")
                return true
            } else {
                Log.w(TAG, "Failed to send initialization/unfreeze command to thermal printer")
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing thermal printer", e)
            return false
        }
    }

    /**
     * Envia dados em chunks pequenos para máxima compatibilidade.
     */
    fun sendDataInChunks(connection: UsbDeviceConnection, endpoint: UsbEndpoint, data: ByteArray): Boolean {
        try {
            val chunkSize = 1024 // Aumentado de 64 para 1024 para melhor desempenho
            var offset = 0

            Log.d(TAG, "Sending ${data.size} bytes to thermal printer in chunks of $chunkSize")

            while (offset < data.size) {
                val remainingBytes = data.size - offset
                val currentChunkSize = minOf(chunkSize, remainingBytes)
                val chunk = data.copyOfRange(offset, offset + currentChunkSize)

                val bytesTransferred = connection.bulkTransfer(endpoint, chunk, chunk.size, 2000) // Reduzido de 5000 para 2000ms
                if (bytesTransferred < 0) {
                    Log.e(TAG, "Failed to transfer chunk at offset $offset to thermal printer")
                    return false
                }

                Log.v(TAG, "Sent chunk of $currentChunkSize bytes (offset: $offset)")
                offset += currentChunkSize

                // Delay curto entre chunks para impressoras térmicas
                if (offset < data.size) {
                    Thread.sleep(5)
                }
            }

            Log.d(TAG, "Successfully sent all data chunks to thermal printer")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data chunks to thermal printer", e)
            return false
        }
    }

    /**
     * Fecha a conexão de forma segura.
     */
    fun closeConnection(connectionData: ConnectionData?) {
        connectionData?.let {
            try {
                Log.d(TAG, "Releasing interface for thermal printer")
                it.connection.releaseInterface(it.usbInterface)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing interface for thermal printer", e)
            }

            try {
                Log.d(TAG, "Closing connection to thermal printer")
                it.connection.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing connection to thermal printer", e)
            }
        }
    }

    /**
     * Cria resposta de sucesso padronizada.
     */
    fun createSuccessResponse(message: String): WritableMap {
        val result = Arguments.createMap()
        result.putBoolean("success", true)
        result.putString("message", message)
        return result
    }

    /**
     * Cria resposta de erro padronizada.
     */
    fun createErrorResponse(message: String): WritableMap {
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("message", message)
        return result
    }
}
