package com.example.mystock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.*

/**
 * Data class สำหรับแสดงสรุปรายการของแต่ละสินค้า
 */
data class ProductReportSummary(
    val productName: String,
    val transactionCount: Int,      // จำนวนครั้งที่มีการทำรายการ
    val totalQuantity: Int,          // จำนวนรวมทั้งหมด
    val totalAmount: Double,         // มูลค่ารวม (quantity × price)
    val transactionType: TransactionType  // IN หรือ OUT
)

/**
 * Adapter สำหรับแสดงรายงาน Stock IN/OUT แบบ Group by Product
 */
class ProductReportAdapter(
    private val items: List<ProductReportSummary>
) : RecyclerView.Adapter<ProductReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textTransactionCount: TextView = view.findViewById(R.id.textTransactionCount)
        val textTotalQuantity: TextView = view.findViewById(R.id.textTotalQuantity)
        val textTotalAmount: TextView = view.findViewById(R.id.textTotalAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        // ชื่อสินค้า
        holder.textProductName.text = item.productName

        // จำนวนครั้งที่ทำรายการ
        val transactionText = when (item.transactionType) {
            TransactionType.IN -> context.getString(R.string.received, item.transactionCount)
            TransactionType.OUT -> context.getString(R.string.withdrawn, item.transactionCount)
        }
        holder.textTransactionCount.text = transactionText

        // จำนวนรวม
        holder.textTotalQuantity.text = "${item.totalQuantity} ${context.getString(R.string.units)}"

        // มูลค่ารวม
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
        holder.textTotalAmount.text = currencyFormat.format(item.totalAmount)
    }

    override fun getItemCount() = items.size
}
