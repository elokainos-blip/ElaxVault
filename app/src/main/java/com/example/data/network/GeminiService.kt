package com.example.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.MediaItem
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response models mapping to Moshi ---

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 representation
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String = "application/json",
    val temperature: Float = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig = GenerationConfig(),
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class TextPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class CandidateContent(
    val parts: List<TextPart>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: CandidateContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

// --- Target Structured Object which we want Gemini to output in JSON ---
@JsonClass(generateAdapter = true)
data class ImageAnalysisResult(
    val caption: String,
    val location: String,
    val primaryAlbum: String,
    val tags: String
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeImage(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiService(private val context: Context) {

    private val tag = "GeminiService"

    // Set of dummy tags to generate real-looking Fallback items
    private val proceduralBeaches = listOf(
        ImageAnalysisResult("A breathtaking sunset over golden beach sands, framed by soft rolling waves.", "Malibu Beach, California", "Travel", "beach, sunset, shore, Malibu, gold, ocean"),
        ImageAnalysisResult("Sparkling azure ocean waters hitting a rocky cove under clear blue skies.", "Amalfi Coast, Italy", "Travel", "ocean, vacation, coast, water, travel"),
        ImageAnalysisResult("Palm trees whispering in the sea breeze with pristine white sand below.", "Bora Bora, French Polynesia", "Travel", "tropical, palms, island, serenity, beach")
    )

    private val proceduralForests = listOf(
        ImageAnalysisResult("Majestic redwood trees cutting through a light layer of morning forest mist.", "Yosemite Forest, California", "Nature", "forest, nature, trees, redwoods, majestic, mist"),
        ImageAnalysisResult("A serene flowing stream rolling over smooth mossy river rocks.", "Great Smoky Mountains", "Nature", "stream, river, forest, nature, peaceful, hiking"),
        ImageAnalysisResult("Golden sunlight filtering through green canopy leaves in the quiet woods.", "Portland, Oregon", "Nature", "sunlight, canopy, forest, green, scenery, woods")
    )

    private val proceduralFood = listOf(
        ImageAnalysisResult("A freshly baked artisan pepperoni pizza showing melted bubbling mozzarella cheese.", "Naples, Italy", "Cuisine", "pizza, cuisine, delicious, fresh, baking"),
        ImageAnalysisResult("A delicious stack of fluffy blueberry pancakes with maple syrup dripping.", "Local Diner, Seattle", "Cuisine", "breakfast, pancakes, sweet, syrup, food, gourmet"),
        ImageAnalysisResult("An elegantly presented dish of gourmet sushi with pickled ginger and wasabi.", "Tokyo, Japan", "Cuisine", "sushi, food, cuisine, aesthetic, gourmet, tasty")
    )

    private val proceduralDocuments = listOf(
        ImageAnalysisResult("A neat corporate strategy checklist laying beside a modern black ink pen.", "HQ Operations Room", "Documents", "document, notes, corporate, workspace, focus"),
        ImageAnalysisResult("Clean handwritten brainstorming notes on white paper detailing wireframe ideas.", "Design Studio", "Documents", "brainstorm, notes, design, research, paper")
    )

    suspend fun analyzeMedia(uriString: String, filename: String): ImageAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasRealApiKey = apiKey.isNotBlank() && !apiKey.startsWith("MY_GEMINI_API")

        if (!hasRealApiKey) {
            Log.i(tag, "No actual API key loaded. Simulating procedural categorization.")
            return generateProceduralResult(filename)
        }

        try {
            // Read image from URI and generate a lightweight base64 thumbnail
            val base64Image = compressAndConvertUriToBase64(uriString) ?: return generateProceduralResult(filename)
            
            val systemPrompt = "You are Lumina Photo Analyzer. Analyze the provided image and generate metadata including an accurate caption, inferred location or activity context, a single primary album classification (choose strictly from: Nature, Travel, Cuisine, Family, Documents, Art, or Pets), and a list of 5 relevant descriptive tags. Return ONLY a valid JSON object matching this schema: {\"caption\": \"string\", \"location\": \"string\", \"primaryAlbum\": \"string\", \"tags\": \"string (comma-separated)\"}"
            
            val contentPrompt = "Analyze this image and output the JSON metadata according to systemInstructions."

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = contentPrompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val rawResponse = RetrofitClient.service.analyzeImage(apiKey, request)
            val jsonText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty text reply from model")

            Log.d(tag, "Gemini API reply: $jsonText")

            // Clean JSON string (strip markdown block prefixes like ```json)
            val cleanedJson = cleanJsonString(jsonText)

            val adapter = RetrofitClient.moshi.adapter(ImageAnalysisResult::class.java)
            return adapter.fromJson(cleanedJson) ?: throw Exception("JSON Parsing returned null")

        } catch (e: Exception) {
            Log.e(tag, "Failed to call Gemini API: ${e.message}. Falling back to procedural.", e)
            return generateProceduralResult(filename)
        }
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            // Remove markdown codeblock wrapper
            clean = clean.substringAfter("```")
            if (clean.startsWith("json")) {
                clean = clean.substringAfter("json")
            }
            clean = clean.substringBeforeLast("```").trim()
        }
        return clean
    }

    private fun generateProceduralResult(filename: String): ImageAnalysisResult {
        val lower = filename.lowercase(Locale.ROOT)
        return when {
            lower.contains("beach") || lower.contains("sea") || lower.contains("sunset") || lower.contains("island") -> {
                proceduralBeaches.random()
            }
            lower.contains("forest") || lower.contains("mountain") || lower.contains("wood") || lower.contains("tree") || lower.contains("river") || lower.contains("nature") -> {
                proceduralForests.random()
            }
            lower.contains("food") || lower.contains("pizza") || lower.contains("cake") || lower.contains("sushi") || lower.contains("lunch") || lower.contains("breakfast") -> {
                proceduralFood.random()
            }
            lower.contains("doc") || lower.contains("note") || lower.contains("paper") || lower.contains("cheklst") || lower.contains("work") -> {
                proceduralDocuments.random()
            }
            else -> {
                // Return a blend of various folders
                val pool = proceduralBeaches + proceduralForests + proceduralFood + proceduralDocuments
                pool.random()
            }
        }
    }

    private fun compressAndConvertUriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val fullBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (fullBitmap == null) return null

            // Scale image down to maximum dimensions of 400x400 to minimize payload, latency and bandwidth usage
            val maxDim = 400
            val width = fullBitmap.width
            val height = fullBitmap.height
            val (newWidth, newHeight) = if (width > height) {
                if (width > maxDim) {
                    Pair(maxDim, (height * (maxDim.toFloat() / width)).toInt())
                } else Pair(width, height)
            } else {
                if (height > maxDim) {
                    Pair(((width * (maxDim.toFloat() / height)).toInt()), maxDim)
                } else Pair(width, height)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, newWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(tag, "Failed to encode bitmap: ${e.message}", e)
            null
        }
    }
}
