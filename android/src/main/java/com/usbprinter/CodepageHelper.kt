package com.usbprinter

import android.util.Log
import java.nio.charset.Charset

/**
 * Mapeia os números de página de código do comando ESC t (Select character code table),
 * conforme a tabela padrão Epson adotada pela grande maioria dos clones ESC/POS
 * (Gertec, Bematech, Elgin, Daruma, etc.), para o Charset Java/Android correspondente.
 */
object CodepageHelper {
    private const val TAG = "CodepageHelper"

    private val CODEPAGE_CHARSETS = mapOf(
        0 to "Cp437", // PC437 [USA: Standard Europe]
        2 to "Cp850", // PC850 [Multilingual]
        3 to "Cp860", // PC860 [Portuguese]
        4 to "Cp863", // PC863 [Canadian-French]
        5 to "Cp865", // PC865 [Nordic]
        16 to "windows-1252", // WPC1252
        17 to "Cp866", // PC866 [Cyrillic #2]
        18 to "Cp852", // PC852 [Latin 2]
        19 to "Cp858" // PC858
    )

    /**
     * Comando ESC t n para selecionar a página de código na impressora.
     */
    fun selectCommand(codepage: Int): ByteArray {
        return byteArrayOf(0x1B, 0x74, codepage.toByte())
    }

    /**
     * Codifica o texto usando o charset correspondente à página de código informada.
     * Cai para ISO-8859-1 se a página for desconhecida ou não suportada pelo dispositivo.
     */
    fun encode(text: String, codepage: Int): ByteArray {
        val charsetName = CODEPAGE_CHARSETS[codepage]
        if (charsetName == null) {
            Log.w(TAG, "Codepage $codepage desconhecida, usando ISO-8859-1 como fallback")
            return text.toByteArray(Charsets.ISO_8859_1)
        }
        return try {
            text.toByteArray(Charset.forName(charsetName))
        } catch (e: Exception) {
            Log.w(TAG, "Charset $charsetName (codepage $codepage) indisponível neste dispositivo, usando ISO-8859-1 como fallback", e)
            text.toByteArray(Charsets.ISO_8859_1)
        }
    }
}
