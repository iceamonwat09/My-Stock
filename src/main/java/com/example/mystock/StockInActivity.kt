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
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * StockInActivity - Modern Minimal Design for Stock IN
 * เพิ่มสต๊อกสินค้า - ดีไซน์ทันสมัยเรียบง่าย
 */
class StockInActivity : BaseActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File
    private lateinit var imageFolder: File
    private lateinit var billingManager: BillingManager

    private lateinit var autoCompleteProduct: AutoCompleteTextView
    private lateinit var textCurrentStock: TextView
    private lateinit var layoutCurrentStock: View
    private lateinit var editTextCategory: EditText
    private lateinit var editTextLocation: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextMinStock: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var textProductCount: TextView

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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_in)

        // Setup action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.stock_in)

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

        // Check Pro version
        lifecycleScope.launch {
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
        imagePreview = findViewById(R.id.imagePreview)
        textProductCount = findViewById(R.id.textViewProductCount)

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
        val stockText = "${getString(R.string.current_stock)}: ${product.currentStock}"
        textCurrentStock.text = stockText

        if (product.isLowStock()) {
            textCurrentStock.setTextColor(getColor(android.R.color.holo_red_dark))
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

        findViewById<Button>(R.id.buttonAttachImage).setOnClickListener {
            showImagePickerDialog()
        }

        findViewById<Button>(R.id.buttonConfirmStockIn).setOnClickListener {
            handleStockIn()
        }
    }

    private fun handleStockIn() {
        val productName = autoCompleteProduct.text.toString().trim()
        val quantityStr = editTextQuantity.text.toString()
        val priceStr = editTextPrice.text.toString()
        val notes = editTextNotes.text.toString()

        // Validation
        if (productName.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_product_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (quantityStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_quantity), Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 0
        if (quantity <= 0) {
            Toast.makeText(this, getString(R.string.quantity_must_positive), Toast.LENGTH_SHORT).show()
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
                // Update existing product
                val stockBefore = existingProduct.currentStock
                val updatedProduct = existingProduct.addStock(quantity)

                if (ProductManager.updateProduct(productsFile, updatedProduct)) {
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

                    Toast.makeText(this@StockInActivity,
                        "${getString(R.string.stock_in_success)} ${updatedProduct.currentStock}",
                        Toast.LENGTH_SHORT).show()

                    clearForm()
                    updateProductCount()
                }
            } else {
                // Create new product
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
                    val transaction = Transaction(
                        productName = productName,
                        transactionType = TransactionType.IN,
                        quantity = quantity,
                        pricePerUnit = price,
                        notes = "${getString(R.string.new_product)}",
                        stockBefore = 0,
                        stockAfter = quantity
                    )
                    TransactionManager.addTransaction(transactionsFile, transaction)

                    Toast.makeText(this@StockInActivity,
                        "${getString(R.string.product_created)} $quantity",
                        Toast.LENGTH_SHORT).show()

                    clearForm()
                    updateProductCount()
                }
            }
        }
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

    private fun updateProductCount() {
        lifecycleScope.launch {
            val productCount = ProductManager.loadProducts(productsFile).size
            val isPro = billingManager.isProVersionFlow.first()
            if (isPro) {
                textProductCount.text = "${getString(R.string.total_products)}: $productCount"
            } else {
                textProductCount.text = "${getString(R.string.products)}: $productCount / 50"
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }
}
