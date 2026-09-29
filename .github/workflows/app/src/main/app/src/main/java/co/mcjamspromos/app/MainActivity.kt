package co.mcjamspromos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WPPost(
    val id: Int,
    val title: RenderedText,
    val date: String,
    val link: String,
    @SerializedName("_embedded") val embedded: EmbeddedData?
)

data class RenderedText(val rendered: String)
data class EmbeddedData(@SerializedName("wp:featuredmedia") val media: List<MediaItem>?)
data class MediaItem(@SerializedName("source_url") val sourceUrl: String)

interface WPApiService {
    @GET("wp/v2/posts?_embed=true")
    suspend fun getPosts(
        @Query("search") search: String? = null
    ): List<WPPost>
}

object ApiClient {
    val service: WPApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://mcjamspromos.co/wp-json/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WPApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MCJamsApp()
        }
    }
}

@Composable
fun MCJamsApp() {
    val darkPurple = Color(0xFF130121)
    val deepBlack = Color(0xFF09000E)
    val accentGlow = Color(0xFF8B2FC9)

    var posts by remember { mutableStateOf<List<WPPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(searchQuery) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                val search = if (searchQuery.isBlank()) null else searchQuery
                posts = ApiClient.service.getPosts(search = search)
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Error connecting to site."
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(darkPurple, deepBlack)))
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MC JAMS PROMOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Official App", color = Color.Gray, fontSize = 12.sp)
                }
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = accentGlow, modifier = Modifier.size(32.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs, news...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentGlow,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentGlow)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = Color.Red)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(posts) { post ->
                        val imageUrl = post.embedded?.media?.firstOrNull()?.sourceUrl ?: ""
                        val title = post.title.rendered.replace("&#8211;", "-").replace("&#8217;", "'")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                                    context.startActivity(intent)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(post.date.take(10), color = Color.Gray, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = accentGlow, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
