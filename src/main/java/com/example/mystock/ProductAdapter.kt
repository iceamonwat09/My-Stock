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

/**
 * ProductAdapter - Adapter สำหรับแสดงรายการสินค้าพร้อมสต๊อกปัจจุบัน
 */
class ProductAdapter(
    private var products: List<Product>,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onViewHistory: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProduct: ImageView = view.findViewById(R.id.imageProduct)
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textLocation: TextView = view.findViewById(R.id.textLocation)
        val textCurrentStock: TextView = view.findViewById(R.id.textCurrentStock)
        val textPrice: TextView = view.findViewById(R.id.textPrice)
        val textTotalValue: TextView = view.findViewById(R.id.textTotalValue)
        val textMinStock: TextView = view.findViewById(R.id.textMinStock)
        val buttonEdit: Button = view.findViewById(R.id.buttonEdit)
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
        val buttonHistory: Button = view.findViewById(R.id.buttonHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        val context = holder.itemView.context

        // Set image
        if (product.imagePath.isNotEmpty() && File(product.imagePath).exists()) {
            holder.imageProduct.setImageURI(Uri.fromFile(File(product.imagePath)))
            holder.imageProduct.visibility = View.VISIBLE
        } else {
            holder.imageProduct.visibility = View.GONE
        }

        // Set text fields
        holder.textProductName.text = product.productName
        holder.textCategory.text = if (product.category.isEmpty()) holder.itemView.context.getString(R.string.unspecified_item) else product.category
        holder.textLocation.text = "📍 ${product.location}"

        // Current Stock - with color coding
        holder.textCurrentStock.text = "สต๊อก: ${product.currentStock} ชิ้น"
        if (product.isLowStock()) {
            holder.textCurrentStock.setTextColor(0xFFF44336.toInt())
            holder.textCurrentStock.text = "⚠️ สต๊อก: ${product.currentStock} ชิ้น (ต่ำ!)"
        } else {
            holder.textCurrentStock.setTextColor(0xFF4CAF50.toInt())
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
        holder.textPrice.text = "${currencyFormat.format(product.pricePerUnit)}/หน่วย"
        holder.textTotalValue.text = "มูลค่า: ${currencyFormat.format(product.getTotalValue())}"

        if (product.minStock > 0) {
            holder.textMinStock.text = "ขั้นต่ำ: ${product.minStock}"
            holder.textMinStock.visibility = View.VISIBLE
        } else {
            holder.textMinStock.visibility = View.GONE
        }

        // Buttons
        holder.buttonEdit.setOnClickListener { onEdit(product) }
        holder.buttonDelete.setOnClickListener { onDelete(product) }
        holder.buttonHistory.setOnClickListener { onViewHistory(product) }
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
