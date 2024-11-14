import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Entity(tableName = "group_table")
data class Group(
    @PrimaryKey(autoGenerate = true) val groupId: Long = 0,
    val groupName: String
)

@Entity(
    tableName = "student_table",
    foreignKeys = [ForeignKey(
        entity = Group::class,
        parentColumns = ["groupId"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val studentId: Long = 0,
    val name: String,
    val surname: String,
    val age: Int,
    val groupId: Long
)

@Dao
interface SchoolDao {
    @Insert suspend fun insertGroup(group: Group)
    @Insert suspend fun insertStudent(student: Student)
    @Query("SELECT * FROM student_table") suspend fun getAllStudents(): List<Student>
    @Query("SELECT * FROM group_table") suspend fun getAllGroups(): List<Group>
    @Query("SELECT * FROM student_table WHERE groupId = :groupId") suspend fun getStudentsByGroup(groupId: Long): List<Student>
    @Delete suspend fun deleteStudent(student: Student)
}

@Database(entities = [Student::class, Group::class], version = 1)
abstract class SchoolDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    companion object {
        @Volatile private var INSTANCE: SchoolDatabase? = null
        fun getDatabase(context: android.content.Context): SchoolDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, SchoolDatabase::class.java, "school_database").build().also { INSTANCE = it }
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var groupNameInput: EditText
    private lateinit var addGroupButton: Button
    private lateinit var nameInput: EditText
    private lateinit var surnameInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var groupSpinner: Spinner
    private lateinit var addStudentButton: Button
    private lateinit var studentsListView: ListView
    private lateinit var filterSpinner: Spinner
    private val db by lazy { SchoolDatabase.getDatabase(this).schoolDao() }
    private lateinit var groupAdapter: ArrayAdapter<Group>
    private lateinit var studentAdapter: ArrayAdapter<Student>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        groupNameInput = findViewById(R.id.groupNameInput)
        addGroupButton = findViewById(R.id.addGroupButton)
        nameInput = findViewById(R.id.nameInput)
        surnameInput = findViewById(R.id.surnameInput)
        ageInput = findViewById(R.id.ageInput)
        groupSpinner = findViewById(R.id.groupSpinner)
        addStudentButton = findViewById(R.id.addStudentButton)
        studentsListView = findViewById(R.id.studentsListView)
        filterSpinner = findViewById(R.id.filterSpinner)

        // Адаптери для Spinner та ListView
        groupAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        groupSpinner.adapter = groupAdapter
        filterSpinner.adapter = groupAdapter

        studentAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        studentsListView.adapter = studentAdapter

        // Додавання групи
        addGroupButton.setOnClickListener {
            val groupName = groupNameInput.text.toString()
            if (groupName.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.insertGroup(Group(groupName = groupName))
                    loadGroups()
                    withContext(Dispatchers.Main) {
                        groupNameInput.text.clear()
                        Toast.makeText(this@MainActivity, "Групу додано", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Додавання студента
        addStudentButton.setOnClickListener {
            val name = nameInput.text.toString()
            val surname = surnameInput.text.toString()
            val age = ageInput.text.toString().toIntOrNull()
            val selectedGroup = groupSpinner.selectedItem as? Group
            if (name.isNotEmpty() && surname.isNotEmpty() && age != null && selectedGroup != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.insertStudent(Student(name = name, surname = surname, age = age, groupId = selectedGroup.groupId))
                    loadStudents()
                    withContext(Dispatchers.Main) {
                        nameInput.text.clear()
                        surnameInput.text.clear()
                        ageInput.text.clear()
                        Toast.makeText(this@MainActivity, "Студента додано", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Відображення студентів з вибраної групи
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                val selectedGroup = filterSpinner.selectedItem as Group
                loadStudentsByGroup(selectedGroup.groupId)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Видалення студента по LongClick
        studentsListView.setOnItemLongClickListener { parent, view, position, id ->
            val student = parent.getItemAtPosition(position) as Student
            lifecycleScope.launch(Dispatchers.IO) {
                db.deleteStudent(student)
                loadStudents()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Студента видалено", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        // Завантаження груп та студентів
        loadGroups()
        loadStudents()
    }

    private fun loadGroups() {
        lifecycleScope.launch(Dispatchers.IO) {
            val groups = db.getAllGroups()
            withContext(Dispatchers.Main) {
                groupAdapter.clear()
                groupAdapter.addAll(groups)
                groupAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch(Dispatchers.IO) {
            val students = db.getAllStudents()
            withContext(Dispatchers.Main) {
                studentAdapter.clear()
                studentAdapter.addAll(students)
                studentAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadStudentsByGroup(groupId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val students = db.getStudentsByGroup(groupId)
            withContext(Dispatchers.Main) {
                studentAdapter.clear()
                studentAdapter.addAll(students)
                studentAdapter.notifyDataSetChanged()
            }
        }
    }
}
