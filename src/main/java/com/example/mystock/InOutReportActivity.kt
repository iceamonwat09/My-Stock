package com.example.mystock

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class InOutReportActivity : BaseActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File
    private lateinit var buttonSelectDate: Button
    private lateinit var textTotalIn: TextView
    private lateinit var textTotalOut: TextView
    private lateinit var recyclerReport: RecyclerView
    private lateinit var textNoData: TextView
    private lateinit var selectedDate: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_out_report)

        // Get CSV files
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        productsFile = File(myFolder, "products.csv")
        transactionsFile = File(myFolder, "transactions.csv")

        // Initialize views
        buttonSelectDate = findViewById(R.id.buttonSelectDate)
        textTotalIn = findViewById(R.id.textTotalIn)
        textTotalOut = findViewById(R.id.textTotalOut)
        recyclerReport = findViewById(R.id.recyclerReport)
        textNoData = findViewById(R.id.textNoData)

        // Setup RecyclerView
        recyclerReport.layoutManager = LinearLayoutManager(this)

        // Initialize with today's date
        selectedDate = Calendar.getInstance()
        updateDateButton()
        loadReport()

        // Date picker
        buttonSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Back button
        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateButton()
                loadReport()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateButton() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        buttonSelectDate.text = dateFormat.format(selectedDate.time)
    }

    private fun loadReport() {
        try {
            // Load all products and transactions
            val products = ProductManager.loadProducts(productsFile)
            val transactions = TransactionManager.loadTransactions(transactionsFile)

            // Filter transactions for selected date
            val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
            val filteredTransactions = transactions.filter { transaction ->
                transaction.dateTime.startsWith(selectedDateStr)
            }

            if (filteredTransactions.isEmpty()) {
                // Show no data message
                recyclerReport.visibility = View.GONE
                textNoData.visibility = View.VISIBLE
                textTotalIn.text = "฿0.00"
                textTotalOut.text = "฿0.00"
                return
            }

            // Show data
            recyclerReport.visibility = View.VISIBLE
            textNoData.visibility = View.GONE

            // Group transactions by product
            val reportData = mutableMapOf<String, InOutReportItem>()

            filteredTransactions.forEach { transaction ->
                val existing = reportData[transaction.productName]

                if (existing != null) {
                    // Update existing item
                    when (transaction.transactionType) {
                        TransactionType.IN -> {
                            reportData[transaction.productName] = existing.copy(
                                qtyIn = existing.qtyIn + transaction.quantity,
                                valueIn = existing.valueIn + transaction.getTotalValue()
                            )
                        }
                        TransactionType.OUT -> {
                            reportData[transaction.productName] = existing.copy(
                                qtyOut = existing.qtyOut + transaction.quantity,
                                valueOut = existing.valueOut + transaction.getTotalValue()
                            )
                        }
                    }
                } else {
                    // Create new item
                    val product = products.find { it.productName == transaction.productName }
                    val category = product?.category ?: ""

                    when (transaction.transactionType) {
                        TransactionType.IN -> {
                            reportData[transaction.productName] = InOutReportItem(
                                productName = transaction.productName,
                                category = category,
                                pricePerUnit = transaction.pricePerUnit,
                                qtyIn = transaction.quantity,
                                valueIn = transaction.getTotalValue(),
                                qtyOut = 0,
                                valueOut = 0.0
                            )
                        }
                        TransactionType.OUT -> {
                            reportData[transaction.productName] = InOutReportItem(
                                productName = transaction.productName,
                                category = category,
                                pricePerUnit = transaction.pricePerUnit,
                                qtyIn = 0,
                                valueIn = 0.0,
                                qtyOut = transaction.quantity,
                                valueOut = transaction.getTotalValue()
                            )
                        }
                    }
                }
            }

            // Convert to list and sort by product name
            val reportList = reportData.values.toList().sortedBy { it.productName }

            // Calculate totals
            val totalValueIn = reportList.sumOf { it.valueIn }
            val totalValueOut = reportList.sumOf { it.valueOut }

            // Update UI
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
            textTotalIn.text = currencyFormat.format(totalValueIn)
            textTotalOut.text = currencyFormat.format(totalValueOut)

            // Update RecyclerView
            val adapter = InOutReportAdapter(reportList)
            recyclerReport.adapter = adapter

        } catch (e: Exception) {
            e.printStackTrace()
            recyclerReport.visibility = View.GONE
            textNoData.visibility = View.VISIBLE
            textTotalIn.text = "฿0.00"
            textTotalOut.text = "฿0.00"
        }
    }
}

// Data class for report items
data class InOutReportItem(
    val productName: String,
    val category: String,
    val pricePerUnit: Double,
    val qtyIn: Int,
    val valueIn: Double,
    val qtyOut: Int,
    val valueOut: Double
)

// Adapter for report items
class InOutReportAdapter(private val items: List<InOutReportItem>) :
    RecyclerView.Adapter<InOutReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val textPrice: TextView = view.findViewById(R.id.textPrice)
        val textQtyIn: TextView = view.findViewById(R.id.textQtyIn)
        val textValueIn: TextView = view.findViewById(R.id.textValueIn)
        val textQtyOut: TextView = view.findViewById(R.id.textQtyOut)
        val textValueOut: TextView = view.findViewById(R.id.textValueOut)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_in_out_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))

        holder.textProductName.text = item.productName

        val categoryText = if (item.category.isNotEmpty()) {
            item.category
        } else {
            context.getString(R.string.all)
        }
        holder.textCategory.text = "${context.getString(R.string.category)}: $categoryText"

        holder.textPrice.text = currencyFormat.format(item.pricePerUnit)

        // IN section
        holder.textQtyIn.text = "${context.getString(R.string.qty_in)}: ${item.qtyIn}"
        holder.textValueIn.text = currencyFormat.format(item.valueIn)

        // OUT section
        holder.textQtyOut.text = "${context.getString(R.string.qty_out)}: ${item.qtyOut}"
        holder.textValueOut.text = currencyFormat.format(item.valueOut)
    }

    override fun getItemCount() = items.size
}
