package com.example.mystock

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.util.*

class ViewDataActivityNew : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CsvRowAdapterNew
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerLocation: Spinner
    private lateinit var buttonDateFrom: Button
    private lateinit var buttonDateTo: Button
    private lateinit var buttonClearFilter: Button
    private lateinit var textViewTotal: TextView
    private lateinit var csvFile: File

    private var allRows = mutableListOf<CsvRow>()
    private var filteredRows = mutableListOf<CsvRow>()
    private var dateFrom: String? = null
    private var dateTo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_data_new)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewData)
        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerLocation = findViewById(R.id.spinnerLocation)
        buttonDateFrom = findViewById(R.id.buttonDateFrom)
        buttonDateTo = findViewById(R.id.buttonDateTo)
        buttonClearFilter = findViewById(R.id.buttonClearFilter)
        textViewTotal = findViewById(R.id.textViewTotal)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CsvRowAdapterNew(
            filteredRows,
            onEdit = { row -> showEditDialog(row) },
            onDelete = { row -> showDeleteConfirmDialog(row) }
        )
        recyclerView.adapter = adapter

        // Get CSV file
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        csvFile = File(myFolder, "my_data.csv")

        // Load data
        loadCSVData()

        // Setup filters
        setupSpinners()
        setupDatePickers()
        setupSearch()

        // Clear filter button
        buttonClearFilter.setOnClickListener {
            clearFilters()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCSVData()
    }

    private fun loadCSVData() {
        allRows.clear()

        if (!csvFile.exists()) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            allRows.addAll(CsvHelper.loadFromCsv(csvFile))

            filteredRows.clear()
            filteredRows.addAll(allRows)
            adapter.updateData(filteredRows)
            updateTotal()

            // Refresh spinners
            setupSpinners()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        // Get unique categories
        val categories = mutableListOf(getString(R.string.all))
        categories.addAll(allRows.map { it.category }.distinct().sorted())
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Get unique locations
        val locations = mutableListOf(getString(R.string.all))
        locations.addAll(allRows.map { it.location }.distinct().sorted())
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = locationAdapter

        // Filter on selection
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        buttonDateFrom.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateFrom = String.format("%04d-%02d-%02d", year, month + 1, day)
                    buttonDateFrom.text = "${getString(R.string.start_date)}: $dateFrom"
                    applyFilters()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonDateTo.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateTo = String.format("%04d-%02d-%02d", year, month + 1, day)
                    buttonDateTo.text = "${getString(R.string.end_date)}: $dateTo"
                    applyFilters()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupSearch() {
        editTextSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun applyFilters() {
        filteredRows.clear()

        val searchText = editTextSearch.text.toString().lowercase()
        val selectedCategory = spinnerCategory.selectedItem?.toString()
        val selectedLocation = spinnerLocation.selectedItem?.toString()

        filteredRows.addAll(allRows.filter { row ->
            // Search filter
            val matchSearch = searchText.isEmpty() ||
                row.productName.lowercase().contains(searchText) ||
                row.category.lowercase().contains(searchText) ||
                row.location.lowercase().contains(searchText)

            // Category filter
            val matchCategory = selectedCategory == getString(R.string.all) || row.category == selectedCategory

            // Location filter
            val matchLocation = selectedLocation == getString(R.string.all) || row.location == selectedLocation

            // Date filter
            val matchDate = if (dateFrom != null || dateTo != null) {
                val rowDate = row.dateTime.substring(0, 10) // Get YYYY-MM-DD part
                val afterFrom = dateFrom == null || rowDate >= dateFrom!!
                val beforeTo = dateTo == null || rowDate <= dateTo!!
                afterFrom && beforeTo
            } else {
                true
            }

            matchSearch && matchCategory && matchLocation && matchDate
        })

        adapter.updateData(filteredRows)
        updateTotal()
    }

    private fun clearFilters() {
        editTextSearch.text.clear()
        spinnerCategory.setSelection(0)
        spinnerLocation.setSelection(0)
        dateFrom = null
        dateTo = null
        buttonDateFrom.text = getString(R.string.start_date)
        buttonDateTo.text = getString(R.string.end_date)

        filteredRows.clear()
        filteredRows.addAll(allRows)
        adapter.updateData(filteredRows)
        updateTotal()
    }

    private fun updateTotal() {
        textViewTotal.text = getString(R.string.showing_rows, filteredRows.size, allRows.size)
    }

    private fun showEditDialog(row: CsvRow) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_row, null)

        val editProductName = dialogView.findViewById<TextInputEditText>(R.id.editProductName)
        val editCategory = dialogView.findViewById<TextInputEditText>(R.id.editCategory)
        val editLocation = dialogView.findViewById<TextInputEditText>(R.id.editLocation)
        val editQuantity = dialogView.findViewById<TextInputEditText>(R.id.editQuantity)
        val editPrice = dialogView.findViewById<TextInputEditText>(R.id.editPrice)
        val editMinStock = dialogView.findViewById<TextInputEditText>(R.id.editMinStock)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreviewEdit)

        // Fill current values
        editProductName.setText(row.productName)
        editCategory.setText(row.category)
        editLocation.setText(row.location)
        editQuantity.setText(row.quantity.toString())
        editPrice.setText(row.pricePerUnit.toString())
        editMinStock.setText(row.minStock.toString())

        // Show image if exists
        if (row.imagePath.isNotEmpty() && File(row.imagePath).exists()) {
            imagePreview.setImageURI(Uri.fromFile(File(row.imagePath)))
            imagePreview.visibility = View.VISIBLE
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                // Update row with new values
                val updatedRow = row.copy(
                    productName = editProductName.text.toString(),
                    category = editCategory.text.toString(),
                    location = editLocation.text.toString(),
                    quantity = editQuantity.text.toString().toIntOrNull() ?: row.quantity,
                    pricePerUnit = editPrice.text.toString().toDoubleOrNull() ?: row.pricePerUnit,
                    minStock = editMinStock.text.toString().toIntOrNull() ?: row.minStock
                )

                try {
                    CsvHelper.updateRow(csvFile, row, updatedRow)
                    loadCSVData()
                    Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmDialog(row: CsvRow) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                try {
                    CsvHelper.deleteRow(csvFile, row)
                    loadCSVData()
                    Toast.makeText(this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
