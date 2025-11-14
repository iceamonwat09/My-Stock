package com.example.mystock

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivityNew : AppCompatActivity() {

    private lateinit var csvFile: File
    private lateinit var imageFolder: File
    private lateinit var editTextProductName: EditText
    private lateinit var editTextCategory: EditText
    private lateinit var editTextLocation: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextMinStock: EditText
    private lateinit var buttonScan: Button
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

    private var currentImagePath: String = ""

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrData = result.data?.getStringExtra("QR_DATA")
            if (!qrData.isNullOrEmpty()) {
                editTextProductName.setText(qrData)
                Toast.makeText(this, getString(R.string.scan_successful), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleImageSelection(it)
        }
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
        setContentView(R.layout.activity_main_new)

        // Initialize Billing
        billingManager = BillingManager(this)
        billingManager.initialize()

        // Initialize CSV file
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (myFolder != null && !myFolder.exists()) {
            myFolder.mkdirs()
        }
        csvFile = File(myFolder, "my_data.csv")

        // Initialize image folder
        imageFolder = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ProductImages")
        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        // Initialize Views
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextCategory = findViewById(R.id.editTextCategory)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextMinStock = findViewById(R.id.editTextMinStock)
        buttonScan = findViewById(R.id.buttonScan)
        buttonAttachImage = findViewById(R.id.buttonAttachImage)
        buttonStockIn = findViewById(R.id.buttonStockIn)
        buttonStockOut = findViewById(R.id.buttonStockOut)
        buttonViewData = findViewById(R.id.buttonViewData)
        buttonDashboard = findViewById(R.id.buttonDashboard)
        buttonOpenFile = findViewById(R.id.buttonOpenFile)
        buttonClearData = findViewById(R.id.buttonClearData)
        buttonUpgradePro = findViewById(R.id.buttonUpgradePro)
        textViewRowCount = findViewById(R.id.textViewRowCount)
        imagePreview = findViewById(R.id.imagePreview)

        editTextProductName.requestFocus()
        updateRowCount()

        // Check Pro version status
        lifecycleScope.launch {
            billingManager.isProVersionFlow.collect { isPro ->
                buttonUpgradePro.visibility = if (isPro) View.GONE else View.VISIBLE
                updateRowCount()
            }
        }

        // Button Click Listeners
        buttonScan.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }

        buttonAttachImage.setOnClickListener {
            showImagePickerDialog()
        }

        buttonStockIn.setOnClickListener {
            saveData("IN")
        }

        buttonStockOut.setOnClickListener {
            saveData("OUT")
        }

        buttonViewData.setOnClickListener {
            val intent = Intent(this, ViewDataActivityNew::class.java)
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
            openCSVFile()
        }

        buttonClearData.setOnClickListener {
            showClearDataDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateRowCount()
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
            // Copy image to app's internal storage
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

    private fun saveData(transactionType: String) {
        val productName = editTextProductName.text.toString()
        val category = editTextCategory.text.toString()
        val location = editTextLocation.text.toString()
        val quantityStr = editTextQuantity.text.toString()
        val priceStr = editTextPrice.text.toString()
        val minStockStr = editTextMinStock.text.toString()

        if (productName.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_required), Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 0
        val price = priceStr.toDoubleOrNull() ?: 0.0
        val minStock = minStockStr.toIntOrNull() ?: 0

        lifecycleScope.launch {
            val currentRows = CsvHelper.loadFromCsv(csvFile).size

            if (billingManager.canAddMoreRows(currentRows)) {
                val dateTime = getCurrentDateTime()
                val row = CsvRow(
                    dateTime = dateTime,
                    productName = productName,
                    category = category,
                    location = location,
                    quantity = quantity,
                    pricePerUnit = price,
                    minStock = minStock,
                    imagePath = currentImagePath,
                    transactionType = transactionType,
                    notes = ""
                )

                try {
                    CsvHelper.saveToCsv(csvFile, row)
                    clearForm()
                    updateRowCount()
                    Toast.makeText(this@MainActivityNew, getString(R.string.save_success), Toast.LENGTH_SHORT).show()

                    // Check for low stock
                    if (row.isLowStock()) {
                        showLowStockAlert(row)
                    }

                } catch (e: Exception) {
                    Toast.makeText(this@MainActivityNew, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                showUpgradeDialog()
            }
        }
    }

    private fun clearForm() {
        editTextProductName.text.clear()
        editTextCategory.text.clear()
        editTextLocation.text.clear()
        editTextQuantity.text.clear()
        editTextPrice.text.clear()
        editTextMinStock.text.clear()
        currentImagePath = ""
        imagePreview.visibility = View.GONE
        editTextProductName.requestFocus()
    }

    private fun showLowStockAlert(row: CsvRow) {
        val message = getString(R.string.low_stock_message, row.productName, row.quantity, row.minStock)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.low_stock_alert))
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openCSVFile() {
        if (!csvFile.exists()) {
            Toast.makeText(this, getString(R.string.no_csv), Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            csvFile
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

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun updateRowCount() {
        lifecycleScope.launch {
            val currentRows = CsvHelper.loadFromCsv(csvFile).size
            billingManager.isProVersionFlow.collect { isPro ->
                if (isPro) {
                    textViewRowCount.text = getString(R.string.saved_rows_unlimited, currentRows)
                } else {
                    textViewRowCount.text = getString(R.string.saved_rows, currentRows, 50)
                }
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
                updateRowCount()
            }
        }
    }

    private fun showClearDataDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.enter_code)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_clear))
            .setMessage(getString(R.string.clear_data_message))
            .setView(input)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val code = input.text.toString()
                if (code == "2025") {
                    deleteCSVFile()
                } else {
                    Toast.makeText(this, getString(R.string.invalid_code), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteCSVFile() {
        if (csvFile.exists()) {
            val deleted = csvFile.delete()
            if (deleted) {
                updateRowCount()
                Toast.makeText(this, getString(R.string.data_cleared), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.cannot_clear), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.no_files), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }
}
