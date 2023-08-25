package com.example.fritheway1

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CategoryListViewAdapter(private val context: Context, private val roomList: List<RoomsItem>) :
    ArrayAdapter<RoomsItem>(context, R.layout.list_item, roomList) {
    private lateinit var roomName : TextView
    private lateinit var roomNum : TextView

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(context)

        val view = layoutInflater.inflate(R.layout.list_item,null)

        roomName = view.findViewById(R.id.room_name)
        roomNum = view.findViewById(R.id.room_num)
        if(roomList[0].category == "Kabinet"){
            roomName.text = roomList[position].people[0].name
        }
        else{
            roomName.text = roomList[position].title
        }
        roomNum.text = roomList[position].room

        return view
    }
}