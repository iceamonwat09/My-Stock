package com.example.mystock

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * StockOutActivity - Modern Minimal Design for Stock OUT
 * เบิกออกสินค้า - แสดงข้อมูลสินค้าเมื่อ scan
 */
class StockOutActivity : BaseActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File

    private lateinit var autoCompleteProduct: AutoCompleteTextView
    private lateinit var cardProductInfo: CardView
    private lateinit var imageProduct: ImageView
    private lateinit var textProductName: TextView
    private lateinit var textCategory: TextView
    private lateinit var textLocation: TextView
    private lateinit var textCurrentStock: TextView
    private lateinit var textPrice: TextView
    private lateinit var textMinStock: TextView
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextNotes: EditText

    private var selectedProduct: Product? = null

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrData = result.data?.getStringExtra("QR_DATA")
            if (!qrData.isNullOrEmpty()) {
                autoCompleteProduct.setText(qrData)
                // Will trigger text changed listener and load product data
                Toast.makeText(this, getString(R.string.scan_successful), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_out)

        // Setup action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.stock_out)

        // Initialize files
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        productsFile = File(myFolder, "products.csv")
        transactionsFile = File(myFolder, "transactions.csv")

        initializeViews()
        setupProductAutoComplete()
        setupButtonListeners()
    }

    private fun initializeViews() {
        autoCompleteProduct = findViewById(R.id.autoCompleteProduct)
        cardProductInfo = findViewById(R.id.cardProductInfo)
        imageProduct = findViewById(R.id.imageProduct)
        textProductName = findViewById(R.id.textProductName)
        textCategory = findViewById(R.id.textCategory)
        textLocation = findViewById(R.id.textLocation)
        textCurrentStock = findViewById(R.id.textCurrentStock)
        textPrice = findViewById(R.id.textPrice)
        textMinStock = findViewById(R.id.textMinStock)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        editTextNotes = findViewById(R.id.editTextNotes)

        autoCompleteProduct.requestFocus()
    }

    private fun setupProductAutoComplete() {
        val products = ProductManager.loadProducts(productsFile)
        val productNames = products.map { it.productName }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productNames)
        autoCompleteProduct.setAdapter(adapter)
        autoCompleteProduct.threshold = 1

        autoCompleteProduct.setOnItemClickListener { _, _, position, _ ->
            val productName = adapter.getItem(position)
            productName?.let { loadProductData(it) }
        }

        autoCompleteProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    val product = ProductManager.findProduct(productsFile, text)
                    if (product != null) {
                        selectedProduct = product
                        displayProductInfo(product)
                    } else {
                        hideProductInfo()
                    }
                } else {
                    hideProductInfo()
                }
            }
        })
    }

    private fun loadProductData(productName: String) {
        val product = ProductManager.findProduct(productsFile, productName)
        if (product != null) {
            selectedProduct = product
            displayProductInfo(product)
        }
    }

    private fun displayProductInfo(product: Product) {
        cardProductInfo.visibility = View.VISIBLE

        textProductName.text = product.productName
        textCategory.text = "${getString(R.string.category)}: ${product.category}"
        textLocation.text = "${getString(R.string.location)}: ${product.location}"
        textPrice.text = "${getString(R.string.price)}: ฿${product.pricePerUnit}"
        textMinStock.text = "${getString(R.string.min_stock)}: ${product.minStock}"

        // Display current stock with color
        textCurrentStock.text = "${getString(R.string.current_stock)}: ${product.currentStock}"
        if (product.isLowStock()) {
            textCurrentStock.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            textCurrentStock.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        // Load product image if exists
        if (product.imagePath.isNotEmpty() && File(product.imagePath).exists()) {
            imageProduct.setImageURI(Uri.fromFile(File(product.imagePath)))
            imageProduct.visibility = View.VISIBLE
        } else {
            imageProduct.visibility = View.GONE
        }
    }

    private fun hideProductInfo() {
        cardProductInfo.visibility = View.GONE
        selectedProduct = null
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.buttonScan).setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }

        findViewById<Button>(R.id.buttonConfirmStockOut).setOnClickListener {
            handleStockOut()
        }
    }

    private fun handleStockOut() {
        val product = selectedProduct
        if (product == null) {
            Toast.makeText(this, getString(R.string.select_product_first), Toast.LENGTH_SHORT).show()
            return
        }

        val quantityStr = editTextQuantity.text.toString()
        if (quantityStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_quantity), Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 0
        if (quantity <= 0) {
            Toast.makeText(this, getString(R.string.quantity_must_positive), Toast.LENGTH_SHORT).show()
            return
        }

        // Check if stock is sufficient
        if (product.currentStock < quantity) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.insufficient_stock))
                .setMessage("${getString(R.string.current_stock)}: ${product.currentStock}\n" +
                    "${getString(R.string.requested)}: $quantity\n\n" +
                    "${getString(R.string.shortage)}: ${quantity - product.currentStock}")
                .setPositiveButton(getString(R.string.ok), null)
                .show()
            return
        }

        // Confirm stock out
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_stock_out))
            .setMessage("${getString(R.string.product)}: ${product.productName}\n" +
                "${getString(R.string.quantity)}: $quantity\n" +
                "${getString(R.string.current_stock)}: ${product.currentStock}\n" +
                "${getString(R.string.after_stock_out)}: ${product.currentStock - quantity}")
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                performStockOut(product, quantity)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performStockOut(product: Product, quantity: Int) {
        val notes = editTextNotes.text.toString()
        val stockBefore = product.currentStock
        val updatedProduct = product.removeStock(quantity)

        if (updatedProduct != null) {
            if (ProductManager.updateProduct(productsFile, updatedProduct)) {
                // Save transaction
                val transaction = Transaction(
                    productName = product.productName,
                    transactionType = TransactionType.OUT,
                    quantity = quantity,
                    pricePerUnit = product.pricePerUnit,
                    notes = notes,
                    stockBefore = stockBefore,
                    stockAfter = updatedProduct.currentStock
                )
                TransactionManager.addTransaction(transactionsFile, transaction)

                Toast.makeText(this,
                    "${getString(R.string.stock_out_success)} ${updatedProduct.currentStock}",
                    Toast.LENGTH_SHORT).show()

                // Show low stock alert if needed
                if (updatedProduct.isLowStock()) {
                    showLowStockAlert(updatedProduct)
                }

                clearForm()
            }
        }
    }

    private fun showLowStockAlert(product: Product) {
        val message = getString(R.string.low_stock_message,
            product.productName, product.currentStock, product.minStock)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.low_stock_alert))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun clearForm() {
        autoCompleteProduct.text.clear()
        editTextQuantity.text.clear()
        editTextNotes.text.clear()
        hideProductInfo()
        autoCompleteProduct.requestFocus()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
