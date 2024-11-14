import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private var counter = 0
    private lateinit var counterTextView: TextView
    private lateinit var incrementButton: Button
    private lateinit var sortButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterTextView = findViewById(R.id.counterTextView)
        incrementButton = findViewById(R.id.incrementButton)
        sortButton = findViewById(R.id.sortButton)

        incrementButton.setOnClickListener {
            counter++
            counterTextView.text = "Counter: $counter"
        }

        sortButton.setOnClickListener {
            startSorting()
        }
    }

    private fun startSorting() {
        val array = (1..1000).shuffled().toIntArray()  // Генеруємо масив для сортування
        val startTime = System.currentTimeMillis()

        // Запускаємо сортування в корутині
        CoroutineScope(Dispatchers.Default).launch {
            bubbleSort(array)
            val endTime = System.currentTimeMillis()

            // Повертаємося в основний потік для відображення Toast
            withContext(Dispatchers.Main) {
                val duration = endTime - startTime
                Toast.makeText(this@MainActivity, "Сортування зайняло $duration мс", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bubbleSort(array: IntArray) {
        for (i in array.indices) {
            for (j in 0 until array.size - i - 1) {
                if (array[j] > array[j + 1]) {
                    val temp = array[j]
                    array[j] = array[j + 1]
                    array[j + 1] = temp
                }
            }
        }
    }
}