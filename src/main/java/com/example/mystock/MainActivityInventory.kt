package com.example.mystock

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity - Professional Inventory Management
 * ระบบจัดการสต๊อกแบบมืออาชีพ
 */
class MainActivityInventory : AppCompatActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File
    private lateinit var imageFolder: File

    private lateinit var autoCompleteProduct: AutoCompleteTextView
    private lateinit var textCurrentStock: TextView
    private lateinit var layoutCurrentStock: View
    private lateinit var editTextCategory: EditText
    private lateinit var editTextLocation: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextMinStock: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var buttonAttachImage: Button
    private lateinit var buttonStockIn: Button
    private lateinit var buttonStockOut: Button
    private lateinit var buttonViewData: Button
    private lateinit var buttonDashboard: Button
    private lateinit var buttonOpenFile: Button
    private lateinit var buttonClearData: Button
    private lateinit var buttonUpgradePro: Button
    private lateinit var textViewRowCount: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var billingManager: BillingManager

    private var selectedProduct: Product? = null
    private var currentImagePath: String = ""

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrData = result.data?.getStringExtra("QR_DATA")
            if (!qrData.isNullOrEmpty()) {
                autoCompleteProduct.setText(qrData)
                Toast.makeText(this, getString(R.string.scan_successful), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImagePath.isNotEmpty()) {
            imagePreview.setImageURI(Uri.fromFile(File(currentImagePath)))
            imagePreview.visibility = View.VISIBLE
            Toast.makeText(this, getString(R.string.image_attached), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_inventory)

        // Initialize Billing
        billingManager = BillingManager(this)
        billingManager.initialize()

        // Initialize files
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (myFolder != null && !myFolder.exists()) {
            myFolder.mkdirs()
        }
        productsFile = File(myFolder, "products.csv")
        transactionsFile = File(myFolder, "transactions.csv")

        // Initialize image folder
        imageFolder = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ProductImages")
        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        initializeViews()
        setupProductAutoComplete()
        setupButtonListeners()
        updateProductCount()

        // Check Pro version status
        lifecycleScope.launch {
            val isPro = billingManager.isProVersionFlow.first()
            buttonUpgradePro.visibility = if (isPro) View.GONE else View.VISIBLE
            updateProductCount()
        }
    }

    private fun initializeViews() {
        autoCompleteProduct = findViewById(R.id.autoCompleteProduct)
        textCurrentStock = findViewById(R.id.textCurrentStock)
        layoutCurrentStock = findViewById(R.id.layoutCurrentStock)
        editTextCategory = findViewById(R.id.editTextCategory)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextMinStock = findViewById(R.id.editTextMinStock)
        editTextNotes = findViewById(R.id.editTextNotes)
        buttonAttachImage = findViewById(R.id.buttonAttachImage)
        buttonStockIn = findViewById(R.id.buttonStockIn)
        buttonStockOut = findViewById(R.id.buttonStockOut)
        buttonViewData = findViewById(R.id.buttonViewData)
        buttonDashboard = findViewById(R.id.buttonDashboard)
        buttonOpenFile = findViewById(R.id.buttonOpenFile)
        buttonClearData = findViewById(R.id.buttonClearData)
        buttonUpgradePro = findViewById(R.id.buttonUpgradePro)
        textViewRowCount = findViewById(R.id.textViewProductCount)
        imagePreview = findViewById(R.id.imagePreview)

        autoCompleteProduct.requestFocus()
    }

    private fun setupProductAutoComplete() {
        // Load existing products
        val products = ProductManager.loadProducts(productsFile)
        val productNames = products.map { it.productName }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productNames)
        autoCompleteProduct.setAdapter(adapter)
        autoCompleteProduct.threshold = 1

        // When product is selected
        autoCompleteProduct.setOnItemClickListener { _, _, position, _ ->
            val productName = adapter.getItem(position)
            productName?.let { loadProductData(it) }
        }

        // When text changes
        autoCompleteProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                val product = ProductManager.findProduct(productsFile, text)
                if (product != null) {
                    selectedProduct = product
                    showCurrentStock(product)
                } else {
                    selectedProduct = null
                    hideCurrentStock()
                }
            }
        })
    }

    private fun loadProductData(productName: String) {
        val product = ProductManager.findProduct(productsFile, productName)
        if (product != null) {
            selectedProduct = product
            editTextCategory.setText(product.category)
            editTextLocation.setText(product.location)
            editTextPrice.setText(product.pricePerUnit.toString())
            editTextMinStock.setText(product.minStock.toString())

            // Load image if exists
            if (product.imagePath.isNotEmpty() && File(product.imagePath).exists()) {
                currentImagePath = product.imagePath
                imagePreview.setImageURI(Uri.fromFile(File(product.imagePath)))
                imagePreview.visibility = View.VISIBLE
            }

            showCurrentStock(product)
        }
    }

    private fun showCurrentStock(product: Product) {
        layoutCurrentStock.visibility = View.VISIBLE
        val stockText = "สต๊อกปัจจุบัน: ${product.currentStock} ชิ้น"
        textCurrentStock.text = stockText

        // Warning if low stock
        if (product.isLowStock()) {
            textCurrentStock.setTextColor(getColor(android.R.color.holo_red_dark))
            textCurrentStock.text = "⚠️ $stockText (ขั้นต่ำ: ${product.minStock})"
        } else {
            textCurrentStock.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    private fun hideCurrentStock() {
        layoutCurrentStock.visibility = View.GONE
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.buttonScan).setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }

        buttonAttachImage.setOnClickListener {
            showImagePickerDialog()
        }

        buttonStockIn.setOnClickListener {
            handleStockIn()
        }

        buttonStockOut.setOnClickListener {
            handleStockOut()
        }

        buttonViewData.setOnClickListener {
            val intent = Intent(this, ViewDataActivityInventory::class.java)
            startActivity(intent)
        }

        buttonDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        buttonUpgradePro.setOnClickListener {
            upgradeToPro()
        }

        buttonOpenFile.setOnClickListener {
            openProductsFile()
        }

        buttonClearData.setOnClickListener {
            showClearDataDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateProductCount()
        setupProductAutoComplete() // Refresh product list
    }

    private fun handleStockIn() {
        val productName = autoCompleteProduct.text.toString().trim()
        val quantityStr = editTextQuantity.text.toString()
        val priceStr = editTextPrice.text.toString()
        val notes = editTextNotes.text.toString()

        // Validation
        if (productName.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกชื่อสินค้า", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantityStr.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกจำนวน", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 0
        if (quantity <= 0) {
            Toast.makeText(this, "จำนวนต้องมากกว่า 0", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0

        lifecycleScope.launch {
            val currentProducts = ProductManager.loadProducts(productsFile).size

            if (!billingManager.canAddMoreRows(currentProducts)) {
                showUpgradeDialog()
                return@launch
            }

            val existingProduct = ProductManager.findProduct(productsFile, productName)

            if (existingProduct != null) {
                // สินค้ามีอยู่แล้ว - เพิ่มสต๊อก
                val stockBefore = existingProduct.currentStock
                val updatedProduct = existingProduct.addStock(quantity)

                if (ProductManager.updateProduct(productsFile, updatedProduct)) {
                    // บันทึกประวัติ
                    val transaction = Transaction(
                        productName = productName,
                        transactionType = TransactionType.IN,
                        quantity = quantity,
                        pricePerUnit = price,
                        notes = notes,
                        stockBefore = stockBefore,
                        stockAfter = updatedProduct.currentStock
                    )
                    TransactionManager.addTransaction(transactionsFile, transaction)

                    Toast.makeText(this@MainActivityInventory,
                        "✅ Stock IN สำเร็จ! สต๊อกใหม่: ${updatedProduct.currentStock}",
                        Toast.LENGTH_SHORT).show()

                    clearForm()
                    updateProductCount()
                }
            } else {
                // สินค้าใหม่ - สร้าง
                val category = editTextCategory.text.toString()
                val location = editTextLocation.text.toString()
                val minStock = editTextMinStock.text.toString().toIntOrNull() ?: 0

                val newProduct = Product(
                    productName = productName,
                    category = category,
                    location = location,
                    currentStock = quantity,
                    pricePerUnit = price,
                    minStock = minStock,
                    imagePath = currentImagePath
                )

                if (ProductManager.addProduct(productsFile, newProduct)) {
                    // บันทึกประวัติ
                    val transaction = Transaction(
                        productName = productName,
                        transactionType = TransactionType.IN,
                        quantity = quantity,
                        pricePerUnit = price,
                        notes = "สร้างสินค้าใหม่",
                        stockBefore = 0,
                        stockAfter = quantity
                    )
                    TransactionManager.addTransaction(transactionsFile, transaction)

                    Toast.makeText(this@MainActivityInventory,
                        "✅ สร้างสินค้าใหม่สำเร็จ! สต๊อก: $quantity",
                        Toast.LENGTH_SHORT).show()

                    clearForm()
                    updateProductCount()
                }
            }
        }
    }

    private fun handleStockOut() {
        val productName = autoCompleteProduct.text.toString().trim()
        val quantityStr = editTextQuantity.text.toString()
        val notes = editTextNotes.text.toString()

        // Validation
        if (productName.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกชื่อสินค้า", Toast.LENGTH_SHORT).show()
            return
        }

        val existingProduct = ProductManager.findProduct(productsFile, productName)
        if (existingProduct == null) {
            Toast.makeText(this, "❌ ไม่พบสินค้านี้ในระบบ", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantityStr.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกจำนวนที่ต้องการเบิก", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 0
        if (quantity <= 0) {
            Toast.makeText(this, "จำนวนต้องมากกว่า 0", Toast.LENGTH_SHORT).show()
            return
        }

        // เช็คสต๊อกพอหรือไม่
        if (existingProduct.currentStock < quantity) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ สต๊อกไม่พอ")
                .setMessage("สต๊อกปัจจุบัน: ${existingProduct.currentStock} ชิ้น\n" +
                    "ต้องการเบิก: $quantity ชิ้น\n\n" +
                    "ขาดอีก ${quantity - existingProduct.currentStock} ชิ้น")
                .setPositiveButton("ตกลง", null)
                .show()
            return
        }

        // ยืนยันการเบิก
        AlertDialog.Builder(this)
            .setTitle("ยืนยัน Stock OUT")
            .setMessage("สินค้า: $productName\n" +
                "เบิก: $quantity ชิ้น\n" +
                "สต๊อกปัจจุบัน: ${existingProduct.currentStock}\n" +
                "สต๊อกหลังเบิก: ${existingProduct.currentStock - quantity}")
            .setPositiveButton("ยืนยัน") { _, _ ->
                performStockOut(existingProduct, quantity, notes)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun performStockOut(product: Product, quantity: Int, notes: String) {
        val stockBefore = product.currentStock
        val updatedProduct = product.removeStock(quantity)

        if (updatedProduct != null) {
            if (ProductManager.updateProduct(productsFile, updatedProduct)) {
                // บันทึกประวัติ
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
                    "✅ Stock OUT สำเร็จ! สต๊อกใหม่: ${updatedProduct.currentStock}",
                    Toast.LENGTH_SHORT).show()

                // แจ้งเตือนถ้าสต๊อกต่ำ
                if (updatedProduct.isLowStock()) {
                    showLowStockAlert(updatedProduct)
                }

                clearForm()
                updateProductCount()
            }
        }
    }

    private fun showLowStockAlert(product: Product) {
        val message = getString(R.string.low_stock_message,
            product.productName, product.currentStock, product.minStock)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.low_stock_alert))
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearForm() {
        autoCompleteProduct.text.clear()
        editTextCategory.text.clear()
        editTextLocation.text.clear()
        editTextQuantity.text.clear()
        editTextPrice.text.clear()
        editTextMinStock.text.clear()
        editTextNotes.text.clear()
        currentImagePath = ""
        imagePreview.visibility = View.GONE
        selectedProduct = null
        hideCurrentStock()
        autoCompleteProduct.requestFocus()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.attach_image))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "PRODUCT_${timeStamp}_"
        val image = File.createTempFile(imageFileName, ".jpg", imageFolder)
        currentImagePath = image.absolutePath
        return image
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destinationFile = File(imageFolder, "PRODUCT_${timeStamp}.jpg")

            contentResolver.openInputStream(uri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            currentImagePath = destinationFile.absolutePath
            imagePreview.setImageURI(Uri.fromFile(destinationFile))
            imagePreview.visibility = View.VISIBLE
            Toast.makeText(this, getString(R.string.image_attached), Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openProductsFile() {
        if (!productsFile.exists()) {
            Toast.makeText(this, getString(R.string.no_csv), Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            productsFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open CSV file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProductCount() {
        lifecycleScope.launch {
            val productCount = ProductManager.loadProducts(productsFile).size
            val isPro = billingManager.isProVersionFlow.first()
            if (isPro) {
                textViewRowCount.text = "สินค้าทั้งหมด: $productCount รายการ (ไม่จำกัด)"
            } else {
                textViewRowCount.text = "สินค้า: $productCount / 50 รายการ"
            }
        }
    }

    private fun showUpgradeDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.limit_reached))
            .setMessage(getString(R.string.upgrade_message))
            .setPositiveButton(getString(R.string.upgrade_button)) { _, _ ->
                upgradeToPro()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setCancelable(false)
            .show()
    }

    private fun upgradeToPro() {
        billingManager.launchPurchaseFlow(this) { success, message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (success) {
                updateProductCount()
            }
        }
    }

    private fun showClearDataDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.enter_code)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_clear))
            .setMessage("จะลบไฟล์ products.csv และ transactions.csv\n" +
                getString(R.string.clear_data_message))
            .setView(input)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val code = input.text.toString()
                if (code == "2025") {
                    deleteAllData()
                } else {
                    Toast.makeText(this, getString(R.string.invalid_code), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteAllData() {
        var success = true

        if (productsFile.exists()) {
            success = success && productsFile.delete()
        }

        if (transactionsFile.exists()) {
            success = success && transactionsFile.delete()
        }

        if (success) {
            updateProductCount()
            clearForm()
            Toast.makeText(this, getString(R.string.data_cleared), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.cannot_clear), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }
}
