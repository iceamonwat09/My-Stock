package com.example.mystock

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ViewDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CsvRowAdapter
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerUser: Spinner
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
        setContentView(R.layout.activity_view_data)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewData)
        editTextSearch = findViewById(R.id.editTextSearch)
        spinnerUser = findViewById(R.id.spinnerUser)
        spinnerLocation = findViewById(R.id.spinnerLocation)
        buttonDateFrom = findViewById(R.id.buttonDateFrom)
        buttonDateTo = findViewById(R.id.buttonDateTo)
        buttonClearFilter = findViewById(R.id.buttonClearFilter)
        textViewTotal = findViewById(R.id.textViewTotal)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CsvRowAdapter(filteredRows)
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

    private fun loadCSVData() {
        allRows.clear()

        if (!csvFile.exists()) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            csvFile.readLines().drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    val dateTime = parts[0].trim()
                    val user = parts[1].trim()
                    val location = parts[2].trim()
                    val data = parts.getOrNull(3)?.trim()?.removeSurrounding("\"") ?: ""
                    
                    allRows.add(CsvRow(dateTime, user, location, data))
                }
            }

            filteredRows.clear()
            filteredRows.addAll(allRows)
            adapter.updateData(filteredRows)
            updateTotal()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        // Get unique users
        val users = mutableListOf("All")
        users.addAll(allRows.map { it.user }.distinct().sorted())
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUser.adapter = userAdapter

        // Get unique locations
        val locations = mutableListOf("All")
        locations.addAll(allRows.map { it.location }.distinct().sorted())
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = locationAdapter

        // Filter on selection
        spinnerUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                    buttonDateFrom.text = "From: $dateFrom"
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
                    buttonDateTo.text = "To: $dateTo"
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
        val selectedUser = spinnerUser.selectedItem?.toString()
        val selectedLocation = spinnerLocation.selectedItem?.toString()

        filteredRows.addAll(allRows.filter { row ->
            // Search filter
            val matchSearch = searchText.isEmpty() || 
                row.data.lowercase().contains(searchText) ||
                row.user.lowercase().contains(searchText) ||
                row.location.lowercase().contains(searchText)

            // User filter
            val matchUser = selectedUser == "All" || row.user == selectedUser

            // Location filter
            val matchLocation = selectedLocation == "All" || row.location == selectedLocation

            // Date filter
            val matchDate = if (dateFrom != null || dateTo != null) {
                val rowDate = row.dateTime.substring(0, 10) // Get YYYY-MM-DD part
                val afterFrom = dateFrom == null || rowDate >= dateFrom!!
                val beforeTo = dateTo == null || rowDate <= dateTo!!
                afterFrom && beforeTo
            } else {
                true
            }

            matchSearch && matchUser && matchLocation && matchDate
        })

        adapter.updateData(filteredRows)
        updateTotal()
    }

    private fun clearFilters() {
        editTextSearch.text.clear()
        spinnerUser.setSelection(0)
        spinnerLocation.setSelection(0)
        dateFrom = null
        dateTo = null
        buttonDateFrom.text = "START DATE"
        buttonDateTo.text = "END DATE"
        
        filteredRows.clear()
        filteredRows.addAll(allRows)
        adapter.updateData(filteredRows)
        updateTotal()
    }

    private fun updateTotal() {
        textViewTotal.text = "Showing  ${filteredRows.size} of  ${allRows.size} rows"
    }
}
