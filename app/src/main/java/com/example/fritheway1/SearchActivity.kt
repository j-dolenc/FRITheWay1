package com.example.fritheway1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fritheway1.databinding.ActivitySearchBinding
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var roomList: Rooms
    private lateinit var tempList: Rooms
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tempList = Rooms()

        fetchRooms().start()

        binding.searchBar.setOnQueryTextListener(object :SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                tempList.clear()

                val text = p0!!.lowercase(Locale.getDefault())

                if(text.isNotEmpty()){
                    for(room in roomList){
                        var roomAdded= false
                        if(room.room.lowercase(Locale.getDefault()).contains(text) ||
                            room.title.lowercase(Locale.getDefault()).contains(text)||
                            room.description.lowercase(Locale.getDefault()).contains(text)||
                            room.category.lowercase(Locale.getDefault()).contains(text)){
                            tempList.add(room)
                            roomAdded = true
                        }
                        for(person in room.people){
                            if(person.name.lowercase(Locale.getDefault()).contains(text) && !roomAdded){
                                tempList.add(room)
                            }
                        }
                    }

                    recyclerView.adapter!!.notifyDataSetChanged()
                }
                else{
                    tempList.clear()
                    tempList.addAll(roomList)
                    recyclerView.adapter!!.notifyDataSetChanged()
                }




                return false
            }

        })
    }


    private fun fetchRooms(): Thread {
        return Thread{
            val url = URL("http://fritheway-env.eba-jpwsbatf.eu-central-1.elasticbeanstalk.com/api/cabinets")
            val url1 = URL("http://10.0.2.2:8080/api/cabinets")
            val connection = url.openConnection() as HttpURLConnection

            if(connection.responseCode == 200){
                val inputSystem = connection.inputStream

                val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                roomList = Gson().fromJson(inputStreamReader,Rooms::class.java)
                tempList.addAll(roomList)
                updateUI(tempList)

                inputStreamReader.close()
                inputSystem.close()
            }
            else{
                //setContentView()
                //Toast.makeText(applicationContext,"Failed fetching cabinets",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(list: Rooms) {
        runOnUiThread{
            kotlin.run {
                //binding.lastUpdated.text = req[0].room
                recyclerView = binding.searchList
                recyclerView.layoutManager = LinearLayoutManager(this)

                val adapter = SearchViewAdapter(applicationContext,list)
                recyclerView.adapter = adapter

                /*
                binding.searchList.onCli= AdapterView.OnItemClickListener { _, _, i, _ ->
                    Toast.makeText(applicationContext, list[i].title + " najs", Toast.LENGTH_SHORT).show()

                    val intent = Intent(applicationContext, RoomActivity::class.java)
                    var peopleString = ""

                    for(person in list[i].people) peopleString += "• ${person.name} \n"

                    val extras = Bundle()

                    extras.putString(CategoryActivity.EXTRA_TITLE, list[i].title)
                    extras.putString(CategoryActivity.EXTRA_NUM,list[i].room)
                    extras.putString(CategoryActivity.EXTRA_DESCRIPTION,list[i].description)
                    extras.putString(CategoryActivity.EXTRA_CATEGORY,list[i].category)
                    extras.putString(CategoryActivity.EXTRA_PEOPLE, peopleString)

                    intent.putExtras(extras)

                    startActivity(intent)
                }*/

            }
        }
    }
}