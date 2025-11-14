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
class MainActivityInventory : BaseActivity() {

    private lateinit var productsFile: File
    private lateinit var transactionsFile: File

    private lateinit var buttonStockIn: Button
    private lateinit var buttonStockOut: Button
    private lateinit var buttonViewData: Button
    private lateinit var buttonDashboard: Button
    private lateinit var buttonOpenFile: Button
    private lateinit var buttonClearData: Button
    private lateinit var buttonUpgradePro: Button
    private lateinit var textViewRowCount: TextView
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_inventory_menu)

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

        initializeViews()
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
        buttonStockIn = findViewById(R.id.buttonStockIn)
        buttonStockOut = findViewById(R.id.buttonStockOut)
        buttonViewData = findViewById(R.id.buttonViewData)
        buttonDashboard = findViewById(R.id.buttonDashboard)
        buttonOpenFile = findViewById(R.id.buttonOpenFile)
        buttonClearData = findViewById(R.id.buttonClearData)
        buttonUpgradePro = findViewById(R.id.buttonUpgradePro)
        textViewRowCount = findViewById(R.id.textViewProductCount)
    }


    private fun setupButtonListeners() {
        buttonStockIn.setOnClickListener {
            val intent = Intent(this, StockInActivity::class.java)
            startActivity(intent)
        }

        buttonStockOut.setOnClickListener {
            val intent = Intent(this, StockOutActivity::class.java)
            startActivity(intent)
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
