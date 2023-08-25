package com.example.fritheway1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import androidx.recyclerview.widget.RecyclerView

class SearchViewAdapter(private val context:Context, private val roomsList: List<RoomsItem>):
    RecyclerView.Adapter<SearchViewAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchViewAdapter.MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item,parent,false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewAdapter.MyViewHolder, position: Int) {

        holder.roomNum.text = roomsList[position].room
        holder.roomTitle.text = roomsList[position].title

        holder.itemView.setOnClickListener {
            //Toast.makeText(context,roomsList[position].title,Toast.LENGTH_SHORT).show()

            val intent = Intent(context, RoomActivity::class.java)
            var peopleString = ""

            for(person in roomsList[position].people) peopleString += "• ${person.name} \n"

            val extras = Bundle()

            extras.putString(CategoryActivity.EXTRA_TITLE, roomsList[position].title)
            extras.putString(CategoryActivity.EXTRA_NUM,roomsList[position].room)
            extras.putString(CategoryActivity.EXTRA_DESCRIPTION,roomsList[position].description)
            extras.putString(CategoryActivity.EXTRA_CATEGORY,roomsList[position].category)
            extras.putString(CategoryActivity.EXTRA_PEOPLE, peopleString)

            intent.putExtras(extras)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return roomsList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomTitle: TextView = itemView.findViewById(R.id.room_title)
        val roomNum: TextView = itemView.findViewById(R.id.room_num)
    }
}