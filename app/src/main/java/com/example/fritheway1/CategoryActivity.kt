package com.example.fritheway1

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
import com.example.fritheway1.databinding.ActivityCategoryBinding
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CategoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "com.example.fritheway1.EXTRA_TITLE"
        const val EXTRA_NUM = "com.example.fritheway1.EXTRA_NUM"
        const val EXTRA_DESCRIPTION = "com.example.fritheway1.EXTRA_DESCRIPTION"
        const val EXTRA_CATEGORY = "com.example.fritheway1.EXTRA_CATEGORY"
        const val EXTRA_PEOPLE = "com.example.fritheway1.EXTRA_PEOPLE"
    }

    private lateinit var categoryName: String
    private lateinit var binding: ActivityCategoryBinding

    private val categories: Map<String, String> = mapOf(
        "Laboratories" to "Lab",
        "Faculty Offices" to "Kabinet",
        "Lecture Halls" to "",
        "Classrooms" to "",
        "Computer Classrooms" to "",
        "Other" to "Drugo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCategoryBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        val intent = intent
        categoryName = intent.getStringExtra(CategoriesActivity.EXTRA_NAME) as String

        binding.categoryTv.text = categoryName
        fetchRooms().start()

    }

    @SuppressLint("SetTextI18n")
    private fun fetchRooms(): Thread {
        return Thread{
            val url = URL("http://fritheway-env.eba-jpwsbatf.eu-central-1.elasticbeanstalk.com/api/cabinets")
            val url1 = URL("http://10.0.2.2:8080/api/cabinets")
            val connection = url.openConnection() as HttpURLConnection

            if(connection.responseCode == 200){
                val inputSystem = connection.inputStream

                val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                val req = Gson().fromJson(inputStreamReader,Rooms::class.java)

                updateUI(req)

                inputStreamReader.close()
                inputSystem.close()
            }
            else{
                //setContentView()
                binding.categoryTv.text = "Failed Connection"
            }
        }
    }

    private fun updateUI(req: Rooms) {
        runOnUiThread{
            kotlin.run {
                //binding.lastUpdated.text = req[0].room


                val list =req.filter{ s-> s.category == categories[categoryName]}
                val listAdapter = CategoryListViewAdapter(applicationContext,list)
                binding.categoryListView.adapter = listAdapter


                binding.categoryListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
                    //Toast.makeText(applicationContext, list[i].title + " najs", Toast.LENGTH_SHORT).show()

                    val intent = Intent(applicationContext, RoomActivity::class.java)
                    var peopleString = ""

                    for(person in list[i].people) peopleString += "• ${person.name} \n"

                    val extras = Bundle()

                    extras.putString(EXTRA_TITLE, list[i].title)
                    extras.putString(EXTRA_NUM,list[i].room)
                    extras.putString(EXTRA_DESCRIPTION,list[i].description)
                    extras.putString(EXTRA_CATEGORY,list[i].category)
                    extras.putString(EXTRA_PEOPLE, peopleString)

                    intent.putExtras(extras)

                    startActivity(intent)
                }

            }
        }
    }

}