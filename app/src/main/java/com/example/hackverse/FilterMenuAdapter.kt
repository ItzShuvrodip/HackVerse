package com.example.hackverse

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView

class FilterMenuAdapter(context: Context, private val items: List<String>) :
    ArrayAdapter<String>(context, android.R.layout.simple_list_item_multiple_choice, items) {

    private val selectedItems = mutableSetOf<String>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as CheckedTextView
        val item = getItem(position) ?: ""
        view.text = item
        view.isChecked = selectedItems.contains(item)
        return view
    }

    fun toggleSelection(item: String) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<String> {
        return selectedItems.toList()
    }
}