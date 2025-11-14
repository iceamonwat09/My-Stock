package com.example.mystock

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var csvFile: File
    private lateinit var editTextData: EditText
    private lateinit var editTextUser: EditText
    private lateinit var editTextLocation: EditText
    private lateinit var buttonScan: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonOpenFile: Button
    private lateinit var buttonClearData: Button
    private lateinit var buttonShowPath: Button
    private lateinit var buttonUpgradePro: Button
    private lateinit var textViewRowCount: TextView
    private lateinit var billingManager: BillingManager

    // ตัวรับผลลัพธ์จาก QR Scanner
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrData = result.data?.getStringExtra("QR_DATA")
            if (!qrData.isNullOrEmpty()) {
                editTextData.setText(qrData)
                Toast.makeText(this, "Scan successful", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Billing
        billingManager = BillingManager(this)
        billingManager.initialize()

        // Initialize Views
        editTextData = findViewById(R.id.editTextData)
        editTextUser = findViewById(R.id.editTextUser)
        editTextLocation = findViewById(R.id.editTextLocation)
        buttonScan = findViewById(R.id.buttonScan)
        buttonSave = findViewById(R.id.buttonSave)
        buttonOpenFile = findViewById(R.id.buttonOpenFile)
        buttonClearData = findViewById(R.id.buttonClearData)
        buttonShowPath = findViewById(R.id.buttonShowPath)

        buttonUpgradePro = findViewById(R.id.buttonUpgradePro)
        val buttonViewData = findViewById<Button>(R.id.buttonViewData)
        textViewRowCount = findViewById(R.id.textViewRowCount)
        buttonViewData.setOnClickListener {
            val intent = Intent(this, ViewDataActivity::class.java)
            startActivity(intent)
        }
        // เตรียม CSV path
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (myFolder != null && !myFolder.exists()) {
            myFolder.mkdirs()
        }
        csvFile = File(myFolder, "my_data.csv")

        editTextData.requestFocus()
        updateRowCount()

        // ตรวจสอบสถานะ Pro และซ่อน/แสดงปุ่ม
        lifecycleScope.launch {
            billingManager.isProVersionFlow.collect { isPro ->
                buttonUpgradePro.visibility = if (isPro) View.GONE else View.VISIBLE
                updateRowCount()
            }
        }

        // ปุ่ม Scan QR Code
        buttonScan.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }

        // ปุ่ม Save
        buttonSave.setOnClickListener {
            saveData()
        }

        // ปุ่มอัปเกรด Pro
        buttonUpgradePro.setOnClickListener {
            upgradeToPro()
        }

        // ปุ่มเปิดไฟล์
        buttonOpenFile.setOnClickListener {
            openCSVFile()
        }

        // ปุ่มล้างข้อมูล
        buttonClearData.setOnClickListener {
            showClearDataDialog()
        }
        buttonShowPath.visibility = View.GONE
        // ปุ่มแสดง Path
        buttonShowPath.setOnClickListener {
            Toast.makeText(this, "Path:\n${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveData() {
        val data = editTextData.text.toString()
        val user = editTextUser.text.toString()
        val location = editTextLocation.text.toString()

        if (data.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกข้อมูล", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val currentRows = countCSVRows()

            if (billingManager.canAddMoreRows(currentRows)) {
                // บันทึกได้
                val dateTime = getCurrentDateTime()
                saveToCSV(dateTime, user, location, data)
                editTextData.text.clear()
                editTextData.requestFocus()
                updateRowCount()
                Toast.makeText(this@MainActivity, "Save successful", Toast.LENGTH_SHORT).show()
            } else {
                // เกิน Limit แล้ว
                showUpgradeDialog()
            }
        }
    }

    private fun saveToCSV(dateTime: String, user: String, location: String, data: String) {
        try {
            val isNewFile = !csvFile.exists()
            val fileWriter = OutputStreamWriter(FileOutputStream(csvFile, true), "UTF-8")

            // ✅ ใหม่ (ถูกต้อง)
            if (isNewFile) {
                fileWriter.write('\uFEFF'.code)  // BOM for UTF-8
                fileWriter.append("Datetime,Detail 1,Detail 2,Data\n")
            }

            val safeUser = user.replace(",", " ")
            val safeLocation = location.replace(",", " ")
            val safeData = data.replace(",", " ")

            fileWriter.append("$dateTime,$safeUser,$safeLocation,\"$safeData\"\n")
            fileWriter.flush()
            fileWriter.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCSVFile() {
        if (!csvFile.exists()) {
            Toast.makeText(this, "No CSV", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "No app found to open the file CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun countCSVRows(): Int {
        return if (csvFile.exists()) {
            csvFile.readLines().size - 1 // ลบหัวตาราง
        } else {
            0
        }
    }

    private fun updateRowCount() {
        lifecycleScope.launch {
            val currentRows = countCSVRows()
            billingManager.isProVersionFlow.collect { isPro ->
                if (isPro) {
                    textViewRowCount.text = "Saved: $currentRows rows (Unrestricted)"
                } else {
                    textViewRowCount.text = "Saved: $currentRows / 50 แถว"
                }
            }
        }
    }

    private fun showUpgradeDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Limit has been reached")
            .setMessage("Free version allows saving up to 50 rows\n\n🌟 Upgrade to Pro Version to:\n• Save unlimited rows\n• Support long-term usage\n• Support developers")
            .setPositiveButton("อัปเกรด Pro (฿99)") { _, _ ->
                upgradeToPro()
            }
            .setNegativeButton("ยกเลิก", null)
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
        input.hint = "Input code to confirm deletion (2025)"

        AlertDialog.Builder(this)
            .setTitle("⚠️ Confirm data clearing")
            .setMessage("Will delete all data files\n Please enter the code: 2025")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val code = input.text.toString()
                if (code == "2025") {
                    deleteCSVFile()
                } else {
                    Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCSVFile() {
        if (csvFile.exists()) {
            val deleted = csvFile.delete()
            if (deleted) {
                updateRowCount()
                Toast.makeText(this, "Data cleared successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Unable to clear data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "There are no files to clean.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }
}
