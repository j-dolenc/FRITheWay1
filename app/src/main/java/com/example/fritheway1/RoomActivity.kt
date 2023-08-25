package com.example.fritheway1

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fritheway1.databinding.ActivityRoomBinding
import com.example.fritheway1.models.Poi
import com.example.fritheway1.models.VenueData
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class RoomActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_LAT = "com.example.fritheway1.EXTRA_LAT"
        const val EXTRA_LONG = "com.example.fritheway1.EXTRA_LONG"
        const val EXTRA_FLOOR = "com.example.fritheway1.EXTRA_FLOOR"
        const val TAG = "RoomActivity"
    }
    private val venueId = "14e12900-389f-11ee-b50b-d1abd7ae06ba"
    private lateinit var title: TextView
    private lateinit var roomNum: TextView
    private lateinit var description: TextView
    private lateinit var people: TextView
    private lateinit var category: String

    private lateinit var binding: ActivityRoomBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        title = binding.roomTitle
        roomNum = binding.roomNumber
        description = binding.roomDescription
        people = binding.roomPeople

        title.text = extras!!.getString(CategoryActivity.EXTRA_TITLE).toString()
        roomNum.text = extras.getString(CategoryActivity.EXTRA_NUM).toString()
        description.text = extras.getString(CategoryActivity.EXTRA_DESCRIPTION).toString().ifEmpty { "The description for this room is empty. :(" }
        people.text = extras.getString(CategoryActivity.EXTRA_PEOPLE).toString().ifEmpty { "There are no people listed for this room. :(" }
        category = extras.getString(CategoryActivity.EXTRA_CATEGORY).toString()


        val btnGo = binding.btnStart

        btnGo.setOnClickListener {

            fetchAndCheckRooms().start()
        }
    }


    private fun fetchAndCheckRooms():Thread {
        return Thread{
            val key = getMetadata("com.indooratlas.android.sdk.API_KEY")

            val url = URL("https://positioning-api.indooratlas.com/v1/venues/${venueId}?key=$key")
            val connection = url.openConnection() as HttpsURLConnection

            if(connection.responseCode == 200){
                Log.d(TAG, "Response code from indoorAtlas is 200")

                val inputSystem = connection.inputStream

                val inputStreamReader = InputStreamReader(inputSystem,"UTF-8")

                val request = Gson().fromJson(inputStreamReader, VenueData::class.java)

                //check if there is a room like this in IndoorAtlas, if there is start navigation with putextra position
                checkAndStart(request)

                inputStreamReader.close()
                inputSystem.close()
            }
            else{
                Log.d(TAG,"Failed connection to IndoorAtlas")
            }
        }
    }

    private fun checkAndStart(request: VenueData) {
        val allRooms = request.pois
        var foundRoom = false
        for(room in allRooms){
            if(room.name.lowercase().contains(roomNum.text.toString().lowercase())){
                Log.d(TAG, "Room found, starting navigation")
                foundRoom = true
                startNavigation(room)
                break
            }
        }
        if(!foundRoom) {
            runOnUiThread {
                kotlin.run {
                    Toast.makeText(
                        applicationContext,
                        "This room(${roomNum.text}) hasn't been uploaded to the map yet, sorry",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun getMetadata(key: String?): String? {
        try {
            return applicationInfo.metaData.getString(key)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    private fun startNavigation(room: Poi) {
        //runOnUiThread { kotlin.run {
            val intent = Intent(applicationContext, NavigationActivity::class.java)
            //val intent = Intent(applicationContext, NavActivity::class.java)

            val extras = Bundle()

            extras.putString(EXTRA_LAT, room.coordinates.lat.toString())
            extras.putString(EXTRA_LONG, room.coordinates.lon.toString())
            extras.putString(EXTRA_FLOOR, room.floorNumber.toString())
            Log.d(TAG,extras.toString())
            intent.putExtras(extras)
            startActivity(intent)
        //}}
    }

}