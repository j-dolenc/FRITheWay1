package com.example.fritheway1

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.AdapterView
import android.widget.Toast
import com.example.fritheway1.databinding.ActivityCategoriesBinding

class CategoriesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "com.example.fritheway1.FULL_NAME"
    }

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var categoriesList: List<String>


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        categoriesList  = listOf(
            "Laboratories",
            "Faculty Offices",
            "Lecture Halls",
            "Classrooms",
            "Computer Rooms",
            "Other",
        )

       /* binding.gridView.setOnTouchListener { _, event ->
            event.action == MotionEvent.ACTION_MOVE
        }
*/
        val gridViewAdapter =CategoriesAdapter(applicationContext,categoriesList)

        binding.gridView.adapter = gridViewAdapter

        binding.gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            //Toast.makeText(applicationContext,categoriesList[i], Toast.LENGTH_SHORT).show()

            val intent = Intent(applicationContext, CategoryActivity::class.java)
            intent.putExtra(EXTRA_NAME, categoriesList[i])
            startActivity(intent)

        }
    }
}