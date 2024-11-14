import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

// Data class для обробки відповіді
data class CatFact(
    val fact: String,
    val length: Int
)

// Інтерфейс для API
interface CatFactApiService {
    @GET("fact")
    suspend fun getCatFact(): CatFact
}

// Singleton для налаштування Retrofit з Moshi
object RetrofitInstance {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://catfact.ninja/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: CatFactApiService by lazy {
        retrofit.create(CatFactApiService::class.java)
    }
}

// Основна Activity
class MainActivity : AppCompatActivity() {

    private lateinit var factTextView: TextView
    private lateinit var fetchFactButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        factTextView = findViewById(R.id.factTextView)
        fetchFactButton = findViewById(R.id.fetchFactButton)

        fetchFactButton.setOnClickListener {
            fetchCatFact()
        }
    }

    private fun fetchCatFact() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getCatFact()
                withContext(Dispatchers.Main) {
                    factTextView.text = response.fact
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Помилка завантаження даних", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
