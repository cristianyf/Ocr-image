package com.example.ocrkotlinmeters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {

    private val FILE_NAME = "urls.txt"
    private var sizeUrl = 0
    private var contador = 0
    private val sbuffer = StringBuffer()
    private val sbufferFallidas = StringBuffer()
    lateinit var textRecognizer: TextRecognizer

    // Propiedad para el Job de las coroutines
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urls = readUrlsFromFile(FILE_NAME)
        sizeUrl = urls.size
        textRecognizer = TextRecognizer.Builder(applicationContext).build()

        // Llamar a la función loadImageFromUrl utilizando launch para cada URL en la lista
        /*coroutineScope.launch {
            for (url in urls) {
                Log.d("TAG", "onCreate: " + ++contador)
                val parts = url.split(", ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val part2 = parts[1]

                descargarImagen(part2)?.let { processImageWithTextRecognizer(it, part2) }
            }
            saveToFileInInternalStorage(sbuffer.toString(), "mi_archivo.txt", applicationContext)
            saveToFileInInternalStorage(
                sbufferFallidas.toString(),
                "mi_archivo_fallidas.txt",
                applicationContext
            )
        }*/

        // Llamar a la función loadImageFromUrl utilizando launch para cada URL en la lista
        coroutineScope.launch {
            for (url in urls) {
                Log.d("TAG", "onCreate: " + ++contador)
                // delay(500)
                descargarImagen(url)?.let { processImageWithTextRecognizer(it, url) }
                //descargarImagen(url)
                //    awaitDownloadApk()
            }
            saveToFileInInternalStorage(sbuffer.toString(), "mi_archivo.txt", applicationContext)
            saveToFileInInternalStorage(
                sbufferFallidas.toString(),
                "mi_archivo_fallidas.txt",
                applicationContext
            )
        }
    }

    // Resto del código de la actividad...
    override fun onDestroy() {
        super.onDestroy()
        // Cancelar el Job en el onDestroy para evitar memory leaks
        coroutineScope.coroutineContext.cancelChildren()
    }

    // Resto del código de la actividad...
    private fun readUrlsFromFile(fileName: String): List<String> {
        val urls = ArrayList<String>()
        try {
            val inputStream: InputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                urls.add(line!!)
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return urls
    }

    fun descargarImagen(urlLogo: String): Bitmap? {
        val url = URL(urlLogo)
        try {
            val imageData = url.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            return bitmap
        } catch (e: IOException) {
            Log.e("IOException", "descargarImagen: " + e.message + " ," + urlLogo)
            sbufferFallidas.append("                  " + "\n" + url + "\n" + "------------------------------------------------------" + "\n" + "\n")
            e.message
            return null
        }
    }

    private fun processImageWithTextRecognizer(bitmap: Bitmap, url: String) {
        var text = ""
        try {
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val textBlocks = textRecognizer.detect(frame)
            for (i in 0 until textBlocks.size()) {
                val textBlock = textBlocks.valueAt(i)
                text = textBlock.value
                if (sizeUrl == contador) {
                    Log.d("TAG", "processImageWithTextRecognizer: $contador")
                }
                /*if (text.contains(
                        "en",
                        ignoreCase = true
                    )
                ) {*/
                val extractedText = extractTextAfterOrd(text)
                //if (extractedText != null && extractedText.length == 6) {
                if (extractedText != null) {
                    sbuffer.append("${extractedText}, $url\n")
                } else {
                    sbufferFallidas.append("${extractedText}, $url\n")
                }
                Log.d(
                    "TAG",
                    "processImageWithTextRecognizer: $sizeUrl $contador $extractedText"
                )
                break // Si ya se encontró el texto, salir del bucle
                /*} else {
                    Log.d(
                        "TAG",
                        "processImageWithTextRecognizer: $sizeUrl $contador "
                    )
                    // sbuffer.append("                  " + "\n" + text + "\n" + url + "\n" + "------------------------------------------------------" + "\n" + "\n")
                    sbufferFallidas.append("NO PUDO OBTENER ORDEN, $url\n")
                }*/
            }
        } catch (e: Exception) {
            sbufferFallidas.append("                  " + "\n" + text + "\n" + url + "\n" + "------------------------------------------------------" + "\n" + "\n")
            Log.e("Exception", "processImageWithTextRecognizer: " + e.message + " , " + url)
        }
    }

    //MEDIDOR

    companion object {
        fun extractTextAfterOrd(inputText: String): String? {
            var extractedText: String? = null
            val pattern: Pattern = Pattern.compile("(?i)\\bMedidor\\b\\s+([A-Z0-9-]+)")
            val matcher: Matcher = pattern.matcher(inputText)
            if (matcher.find()) {
                extractedText = matcher.group(1)
            }
            return extractedText
        }
    }

    /*companion object {
        fun extractTextAfterOrd(inputText: String): String? {
            var extractedText: String? = null
            val pattern: Pattern = Pattern.compile("(?i)en:\\s*(\\d+)")
            val matcher: Matcher = pattern.matcher(inputText)
            if (matcher.find()) {
                val group1 = matcher.group(1)
                extractedText = group1?.filter { it.isDigit() }
            } else {
                val pattern: Pattern = Pattern.compile("(?i)Ord\\s*(.*)")
                val matcher: Matcher = pattern.matcher(inputText)
                if (matcher.find()) {
                    val group1 = matcher.group(1)
                    extractedText = group1?.filter { it.isDigit() }
                }
            }
            if (extractedText != null && extractedText.length != 6) {
                val pattern: Pattern = Pattern.compile("(?i)Ord\\s*(.*)")
                val matcher: Matcher = pattern.matcher(inputText)
                if (matcher.find()) {
                    val group1 = matcher.group(1)
                    extractedText = group1?.filter { it.isDigit() }
                }
            }
            return extractedText
        }
    }*/

    fun saveToFileInInternalStorage(content: String, fileName: String, context: Context) {
        try {
            val file = File(context.filesDir, fileName)
            val writer = BufferedWriter(FileWriter(file))
            writer.write(content)
            writer.close()
            Log.d("TAG", "Archivo guardado correctamente en el directorio de archivos internos.")
        } catch (e: IOException) {
            Log.e("TAG", "Error al guardar el archivo: ${e.message}")
        }
    }

}