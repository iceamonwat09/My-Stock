package com.example.mystock

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.NumberFormat
import java.util.*

class DashboardActivity : BaseActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File
    private lateinit var textTotalItems: TextView
    private lateinit var textTotalValue: TextView
    private lateinit var textLowStockCount: TextView
    private lateinit var textTotalQuantity: TextView
    private lateinit var recyclerLowStock: RecyclerView
    private lateinit var recyclerCategories: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Get CSV files
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        productsFile = File(myFolder, "products.csv")
        transactionsFile = File(myFolder, "transactions.csv")

        // Initialize views
        textTotalItems = findViewById(R.id.textTotalItems)
        textTotalValue = findViewById(R.id.textTotalValue)
        textLowStockCount = findViewById(R.id.textLowStockCount)
        textTotalQuantity = findViewById(R.id.textTotalQuantity)
        recyclerLowStock = findViewById(R.id.recyclerLowStock)
        recyclerCategories = findViewById(R.id.recyclerCategories)

        // Setup RecyclerViews
        recyclerLowStock.layoutManager = LinearLayoutManager(this)
        recyclerCategories.layoutManager = LinearLayoutManager(this)

        // Load dashboard data
        loadDashboard()

        // Navigation buttons
        findViewById<Button>(R.id.buttonBackToMain).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.buttonViewAllData).setOnClickListener {
            startActivity(Intent(this, ViewDataActivityInventory::class.java))
        }

        findViewById<Button>(R.id.buttonInOutReport).setOnClickListener {
            startActivity(Intent(this, InOutReportActivity::class.java))
        }

        findViewById<Button>(R.id.buttonRefresh).setOnClickListener {
            loadDashboard()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        try {
            val products = ProductManager.loadProducts(productsFile)
            val stats = ProductManager.calculateStatistics(productsFile)

            // Update summary cards
            textTotalItems.text = stats.totalItems.toString()
            textTotalQuantity.text = stats.totalQuantity.toString()
            textLowStockCount.text = stats.lowStockCount.toString()

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
            textTotalValue.text = currencyFormat.format(stats.totalValue)

            // Show low stock items
            if (stats.lowStockProducts.isNotEmpty()) {
                val lowStockAdapter = LowStockAdapter(stats.lowStockProducts)
                recyclerLowStock.adapter = lowStockAdapter
            } else {
                recyclerLowStock.adapter = LowStockAdapter(emptyList())
            }

            // Show category summary
            val categoryData = products.groupBy { it.category }
                .map { (category, items) ->
                    CategorySummary(
                        category = category,
                        itemCount = items.size,
                        totalQuantity = items.sumOf { it.currentStock },
                        totalValue = items.sumOf { it.getTotalValue() }
                    )
                }
                .sortedByDescending { it.totalValue }

            val categoryAdapter = CategoryAdapter(categoryData)
            recyclerCategories.adapter = categoryAdapter

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Adapter for low stock items
class LowStockAdapter(private val items: List<Product>) : RecyclerView.Adapter<LowStockAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textStockLevel: TextView = view.findViewById(R.id.textStockLevel)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_low_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textProductName.text = item.productName

        // Display category
        val categoryText = if (item.category.isNotEmpty()) {
            item.category
        } else {
            holder.itemView.context.getString(R.string.all)
        }
        holder.textCategory.text = "${holder.itemView.context.getString(R.string.category)}: $categoryText"

        holder.textStockLevel.text = "${item.currentStock} / ${item.minStock}"
    }

    override fun getItemCount() = items.size
}

// Adapter for category summary
class CategoryAdapter(private val categories: List<CategorySummary>) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textItems: TextView = view.findViewById(R.id.textItems)
        val textQuantity: TextView = view.findViewById(R.id.textQuantity)
        val textValue: TextView = view.findViewById(R.id.textValue)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.textCategory.text = category.category.ifEmpty { "Uncategorized" }
        holder.textItems.text = "${category.itemCount} items"
        holder.textQuantity.text = "Qty: ${category.totalQuantity}"

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
        holder.textValue.text = currencyFormat.format(category.totalValue)
    }

    override fun getItemCount() = categories.size
}

data class CategorySummary(
    val category: String,
    val itemCount: Int,
    val totalQuantity: Int,
    val totalValue: Double
)
