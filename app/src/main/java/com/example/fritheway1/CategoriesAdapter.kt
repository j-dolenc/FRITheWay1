package com.example.fritheway1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class CategoriesAdapter(private val context: Context, private val categoriesList: List<String>) :BaseAdapter(){

    private var layoutInflater: LayoutInflater? = null
    private lateinit var categoryTextView: TextView


    override fun getCount(): Int {
        return categoriesList.size
    }

    override fun getItem(p0: Int): Any? {
        return null
    }

    override fun getItemId(p0: Int): Long {
        return 0;
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var convertView = p1

        if(layoutInflater == null){
            layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }
        if (convertView == null){
            convertView = layoutInflater!!.inflate(R.layout.grid_item,null)
        }


        categoryTextView = convertView!!.findViewById(R.id.grid_item_tv)
        categoryTextView
        categoryTextView.text = categoriesList[p0]

        return convertView
    }
}