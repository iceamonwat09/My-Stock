package com.example.mystock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CsvRowAdapter(private var rows: List<CsvRow>) : 
    RecyclerView.Adapter<CsvRowAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        val textUser: TextView = view.findViewById(R.id.textUser)
        val textLocation: TextView = view.findViewById(R.id.textLocation)
        val textData: TextView = view.findViewById(R.id.textData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_csv_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.textDateTime.text = row.dateTime
        holder.textUser.text = row.category
        holder.textLocation.text = row.location
        holder.textData.text = row.productName
    }

    override fun getItemCount() = rows.size

    fun updateData(newRows: List<CsvRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
