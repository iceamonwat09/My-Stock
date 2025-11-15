package com.example.mystock

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.NumberFormat
import java.util.*

class CsvRowAdapterNew(
    private var rows: List<CsvRow>,
    private val onEdit: (CsvRow) -> Unit,
    private val onDelete: (CsvRow) -> Unit
) : RecyclerView.Adapter<CsvRowAdapterNew.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProduct: ImageView = view.findViewById(R.id.imageProduct)
        val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textLocation: TextView = view.findViewById(R.id.textLocation)
        val textQuantity: TextView = view.findViewById(R.id.textQuantity)
        val textPrice: TextView = view.findViewById(R.id.textPrice)
        val textTotalValue: TextView = view.findViewById(R.id.textTotalValue)
        val textTransactionType: TextView = view.findViewById(R.id.textTransactionType)
        val buttonEdit: Button = view.findViewById(R.id.buttonEdit)
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_csv_row_new, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        val context = holder.itemView.context

        // Set image
        if (row.imagePath.isNotEmpty() && File(row.imagePath).exists()) {
            holder.imageProduct.setImageURI(Uri.fromFile(File(row.imagePath)))
            holder.imageProduct.visibility = View.VISIBLE
        } else {
            holder.imageProduct.visibility = View.GONE
        }

        // Set text fields
        holder.textDateTime.text = row.dateTime
        holder.textProductName.text = row.productName
        holder.textCategory.text = if (row.category.isEmpty()) holder.itemView.context.getString(R.string.unspecified_item) else row.category
        holder.textLocation.text = "📍 ${row.location}"
        holder.textQuantity.text = "Qty: ${row.quantity}"

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
        holder.textPrice.text = "${currencyFormat.format(row.pricePerUnit)}/unit"
        holder.textTotalValue.text = "Total: ${currencyFormat.format(row.getTotalValue())}"

        // Transaction type badge
        when (row.transactionType) {
            "IN" -> {
                holder.textTransactionType.text = "IN ➕"
                holder.textTransactionType.setBackgroundColor(0xFF4CAF50.toInt())
            }
            "OUT" -> {
                holder.textTransactionType.text = "OUT ➖"
                holder.textTransactionType.setBackgroundColor(0xFFF44336.toInt())
            }
            else -> {
                holder.textTransactionType.visibility = View.GONE
            }
        }

        // Low stock warning
        if (row.isLowStock()) {
            holder.textQuantity.setTextColor(0xFFF44336.toInt())
            holder.textQuantity.text = "⚠️ Qty: ${row.quantity} (Min: ${row.minStock})"
        } else {
            holder.textQuantity.setTextColor(0xFF212121.toInt())
        }

        // Edit and Delete buttons
        holder.buttonEdit.setOnClickListener {
            onEdit(row)
        }

        holder.buttonDelete.setOnClickListener {
            onDelete(row)
        }
    }

    override fun getItemCount() = rows.size

    fun updateData(newRows: List<CsvRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
