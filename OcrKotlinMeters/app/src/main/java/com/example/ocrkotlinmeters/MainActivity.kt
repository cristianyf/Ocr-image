package com.example.ocrkotlinmeters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.coroutineContext

var downloadApkDeferred: CompletableDeferred<Boolean>? = null
internal suspend inline fun awaitDownloadApk(): Boolean {
    val deferred = CompletableDeferred<Boolean>(coroutineContext[Job])
    downloadApkDeferred = deferred
    return deferred.await()
}

class MainActivity : AppCompatActivity() {

    private val FILE_NAME = "urls.txt"
    private var sizeUrl = 0
    private var contador = 0
    private val sbuffer = StringBuffer()

    // Propiedad para el Job de las coroutines
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urls = readUrlsFromFile(FILE_NAME)
        sizeUrl = urls.size

        // Llamar a la función loadImageFromUrl utilizando launch para cada URL en la lista
        coroutineScope.launch {
            for (url in urls) {
                Log.d("TAG", "onCreate: " + contador++)
                // delay(500)
                 descargarImagen(url)?.let { processImageWithTextRecognizer(it, url) }
                descargarImagen(url)
                //    awaitDownloadApk()
            }
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

    private suspend fun loadImageFromUrl(url: String) {
        Glide.with(this@MainActivity)
            .asBitmap()
            .load(url)
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    coroutineScope.launch {
                        processImageWithTextRecognizer(resource, url)
                    }

                }
            })
    }

    fun descargarImagen(urlLogo: String): Bitmap? {
        val url = URL(urlLogo)
        try {
            val imageData = url.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            return bitmap
        } catch (e: IOException) {
            e.message
            return null
        }
    }

    private suspend fun processImageWithTextRecognizer(bitmap: Bitmap, url: String) =
        withContext(Dispatchers.IO) {
            try {
               // contador++
                // Crear el marco para la imagen
                val frame = Frame.Builder().setBitmap(bitmap).build()

                // Realizar la detección de texto
                val textRecognizer = TextRecognizer.Builder(this@MainActivity).build()
                val textBlocks = textRecognizer.detect(frame)

                // Recorrer los bloques de texto detectados y buscar el texto específico
                /*for (i in 0 until textBlocks.size()) {
                    val textBlock = textBlocks.valueAt(i)
                    val text = textBlock.value
                    if (sizeUrl == contador) {
                        Log.d("TAG", "processImageWithTextRecognizer: $contador")
                    }
                    // Verificar si el texto específico está contenido en el bloque de texto actual
                    if (text.contains("Ord", true) || text.contains("Qrden", true) ||
                        text.contains("Drden", true) || text.contains("Ocen", true) ||
                        text.contains("Oden", true) || text.contains("Or e", true)
                    ) {
                        // Hacer algo con el texto encontrado
                        // Por ejemplo, mostrarlo en un TextView
                        val extractedText = extractTextAfterOrd(text)
//                        sbuffer.append("${extractedText ?: "null"}  $url\n")
                        Log.d(
                            "TAG",
                            "processImageWithTextRecognizer: $sizeUrl $contador $extractedText"
                        )
                        break // Si ya se encontró el texto, salir del bucle
                    } else {
                        Log.d(
                            "TAG",
                            "processImageWithTextRecognizer: $sizeUrl $contador "
                        )
//                        sbuffer.append("NO PUDO OBTENER ORDEN $url\n")
                    }
                }*/
                //  downloadApkDeferred?.complete(true)
            } catch (e: Exception) {
                Log.e("Error ocr", "processImageWithTextRecognizer: " + e.message)
            }
        }


    companion object {
        fun extractTextAfterOrd(inputText: String): String? {
            var extractedText: String? = null
            val pattern: Pattern = Pattern.compile("(?i)Ord\\s*(.*)")
            //Pattern pattern = Pattern.compile("(O|Q|D)\\w{1,}[.:;]\\s*(\\d{6})")
            val matcher: Matcher = pattern.matcher(inputText)
            if (matcher.find()) {
                extractedText = matcher.group(1)
            }
            return extractedText
        }
    }
}

