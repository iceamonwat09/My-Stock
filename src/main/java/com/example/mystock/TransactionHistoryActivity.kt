package com.example.mystock

import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.NumberFormat
import java.util.*

/**
 * TransactionHistoryActivity - แสดงประวัติการเคลื่อนไหวของสินค้าแต่ละรายการ
 */
class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File
    private lateinit var textProductName: TextView
    private lateinit var textCurrentStock: TextView
    private lateinit var textTotalValue: TextView
    private lateinit var textTransactionCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        // Get product name from intent
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: ""

        // Initialize files
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        productsFile = File(myFolder, "products.csv")
        transactionsFile = File(myFolder, "transactions.csv")

        // Initialize views
        textProductName = findViewById(R.id.textProductName)
        textCurrentStock = findViewById(R.id.textCurrentStock)
        textTotalValue = findViewById(R.id.textTotalValue)
        textTransactionCount = findViewById(R.id.textTransactionCount)
        recyclerView = findViewById(R.id.recyclerViewHistory)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load data
        loadHistory(productName)

        // Back button
        findViewById<android.widget.Button>(R.id.buttonBack).setOnClickListener {
            finish()
        }
    }

    private fun loadHistory(productName: String) {
        // Load product info
        val product = ProductManager.findProduct(productsFile, productName)

        if (product != null) {
            textProductName.text = product.productName

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
            textCurrentStock.text = "สต๊อกปัจจุบัน: ${product.currentStock} ชิ้น"
            textTotalValue.text = "มูลค่า: ${currencyFormat.format(product.getTotalValue())}"

            // Color code stock
            if (product.isLowStock()) {
                textCurrentStock.setTextColor(getColor(android.R.color.holo_red_dark))
                textCurrentStock.text = "⚠️ สต๊อกปัจจุบัน: ${product.currentStock} ชิ้น (ต่ำ!)"
            } else {
                textCurrentStock.setTextColor(getColor(android.R.color.holo_green_dark))
            }
        } else {
            textProductName.text = "สินค้า: $productName (ไม่พบในระบบ)"
        }

        // Load transactions
        val transactions = TransactionManager.loadTransactionsByProduct(transactionsFile, productName)
        textTransactionCount.text = "รายการทั้งหมด: ${transactions.size} รายการ"

        // Setup adapter
        adapter = TransactionAdapter(transactions)
        recyclerView.adapter = adapter

        // Calculate summary
        val totalIn = transactions.filter { it.transactionType == TransactionType.IN }
            .sumOf { it.quantity }
        val totalOut = transactions.filter { it.transactionType == TransactionType.OUT }
            .sumOf { it.quantity }

        findViewById<TextView>(R.id.textSummary).text =
            "สรุป: รับเข้า +$totalIn | เบิกออก -$totalOut"
    }
}
