package com.example.mystock

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * TransactionsFragment - แสดงประวัติการเคลื่อนไหวสต๊อกทั้งหมด
 */
class TransactionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var buttonDateFrom: Button
    private lateinit var buttonDateTo: Button
    private lateinit var buttonClearFilter: Button
    private lateinit var textViewTotal: TextView

    private var allTransactions = mutableListOf<Transaction>()
    private var filteredTransactions = mutableListOf<Transaction>()
    private var dateFrom: String? = null
    private var dateTo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewTransactions)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        spinnerType = view.findViewById(R.id.spinnerType)
        buttonDateFrom = view.findViewById(R.id.buttonDateFrom)
        buttonDateTo = view.findViewById(R.id.buttonDateTo)
        buttonClearFilter = view.findViewById(R.id.buttonClearFilter)
        textViewTotal = view.findViewById(R.id.textViewTotal)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(filteredTransactions)
        recyclerView.adapter = adapter

        loadData()
        setupFilters()
        setupDatePickers()

        buttonClearFilter.setOnClickListener {
            clearFilters()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val activity = activity as? ViewDataActivityInventory ?: return
        allTransactions.clear()
        allTransactions.addAll(TransactionManager.loadTransactions(activity.transactionsFile))

        applyFilters()
    }

    private fun setupFilters() {
        // Type filter
        val types = listOf("ทั้งหมด", "Stock IN (รับเข้า)", "Stock OUT (เบิกออก)")
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            types
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Search filter
        editTextSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        buttonDateFrom.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    dateFrom = String.format("%04d-%02d-%02d", year, month + 1, day)
                    buttonDateFrom.text = "จาก: $dateFrom"
                    applyFilters()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonDateTo.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    dateTo = String.format("%04d-%02d-%02d", year, month + 1, day)
                    buttonDateTo.text = "ถึง: $dateTo"
                    applyFilters()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun applyFilters() {
        filteredTransactions.clear()

        val searchText = editTextSearch.text.toString().lowercase()
        val selectedType = spinnerType.selectedItemPosition

        filteredTransactions.addAll(allTransactions.filter { transaction ->
            // Search filter
            val matchSearch = searchText.isEmpty() ||
                    transaction.productName.lowercase().contains(searchText) ||
                    transaction.notes.lowercase().contains(searchText)

            // Type filter
            val matchType = when (selectedType) {
                0 -> true // ทั้งหมด
                1 -> transaction.transactionType == TransactionType.IN
                2 -> transaction.transactionType == TransactionType.OUT
                else -> true
            }

            // Date filter
            val matchDate = if (dateFrom != null || dateTo != null) {
                val transDate = transaction.dateTime.substring(0, 10) // YYYY-MM-DD
                val afterFrom = dateFrom == null || transDate >= dateFrom!!
                val beforeTo = dateTo == null || transDate <= dateTo!!
                afterFrom && beforeTo
            } else {
                true
            }

            matchSearch && matchType && matchDate
        })

        adapter.updateData(filteredTransactions)
        updateTotal()
    }

    private fun clearFilters() {
        editTextSearch.text.clear()
        spinnerType.setSelection(0)
        dateFrom = null
        dateTo = null
        buttonDateFrom.text = "วันที่เริ่มต้น"
        buttonDateTo.text = "วันที่สิ้นสุด"
        applyFilters()
    }

    private fun updateTotal() {
        val totalIn = filteredTransactions.filter { it.transactionType == TransactionType.IN }
            .sumOf { it.quantity }
        val totalOut = filteredTransactions.filter { it.transactionType == TransactionType.OUT }
            .sumOf { it.quantity }

        textViewTotal.text = "แสดง ${filteredTransactions.size} จาก ${allTransactions.size} รายการ\n" +
                "รวม IN: +$totalIn | OUT: -$totalOut"
    }
}
