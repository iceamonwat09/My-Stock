package com.example.mystock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.*

/**
 * TransactionAdapter - Adapter สำหรับแสดงประวัติการเคลื่อนไหวสต๊อก
 */
class TransactionAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textType: TextView = view.findViewById(R.id.textType)
        val textQuantity: TextView = view.findViewById(R.id.textQuantity)
        val textPrice: TextView = view.findViewById(R.id.textPrice)
        val textStockChange: TextView = view.findViewById(R.id.textStockChange)
        val textNotes: TextView = view.findViewById(R.id.textNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context

        holder.textDateTime.text = transaction.dateTime
        holder.textProductName.text = transaction.productName

        // Transaction Type Badge
        when (transaction.transactionType) {
            TransactionType.IN -> {
                holder.textType.text = "IN ➕"
                holder.textType.setBackgroundColor(0xFF4CAF50.toInt())
                holder.textType.setTextColor(0xFFFFFFFF.toInt())
            }
            TransactionType.OUT -> {
                holder.textType.text = "OUT ➖"
                holder.textType.setBackgroundColor(0xFFF44336.toInt())
                holder.textType.setTextColor(0xFFFFFFFF.toInt())
            }
        }

        // Quantity
        holder.textQuantity.text = "จำนวน: ${transaction.quantity}"

        // Price
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
        holder.textPrice.text = currencyFormat.format(transaction.pricePerUnit)

        // Stock Change
        val change = "${transaction.stockBefore} → ${transaction.stockAfter}"
        holder.textStockChange.text = "สต๊อก: $change"

        // Notes
        if (transaction.notes.isNotEmpty()) {
            holder.textNotes.text = "หมายเหตุ: ${transaction.notes}"
            holder.textNotes.visibility = View.VISIBLE
        } else {
            holder.textNotes.visibility = View.GONE
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
