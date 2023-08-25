package com.example.fritheway1

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.example.fritheway1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private val permCodes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val neededPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.INTERNET
        )
        ActivityCompat.requestPermissions(this, neededPermissions, permCodes)

        binding.btnSearch.setOnClickListener {
            val intent = Intent(applicationContext,SearchActivity::class.java)
            startActivity(intent)
        }

        binding.btnCategories.setOnClickListener {
            val intent = Intent(applicationContext,CategoriesActivity::class.java)
            startActivity(intent)
        }
    }
}