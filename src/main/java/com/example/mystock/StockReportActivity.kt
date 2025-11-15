package com.example.mystock

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class StockReportActivity : BaseActivity() {

    private lateinit var transactionsFile: File
    private lateinit var buttonStartDate: Button
    private lateinit var buttonEndDate: Button
    private lateinit var buttonFilter: Button
    private lateinit var textTotalIn: TextView
    private lateinit var textTotalOut: TextView
    private lateinit var textTotalInCount: TextView
    private lateinit var textTotalOutCount: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var allTransactions: List<Transaction> = emptyList()
    private var filteredTransactions: List<Transaction> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_report)

        // Get transactions file
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        transactionsFile = File(myFolder, "transactions.csv")

        // Initialize views
        buttonStartDate = findViewById(R.id.buttonStartDate)
        buttonEndDate = findViewById(R.id.buttonEndDate)
        buttonFilter = findViewById(R.id.buttonFilter)
        textTotalIn = findViewById(R.id.textTotalIn)
        textTotalOut = findViewById(R.id.textTotalOut)
        textTotalInCount = findViewById(R.id.textTotalInCount)
        textTotalOutCount = findViewById(R.id.textTotalOutCount)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // Set default dates to today
        updateDateButtons()

        // Setup date pickers
        buttonStartDate.setOnClickListener { showDatePicker(true) }
        buttonEndDate.setOnClickListener { showDatePicker(false) }

        // Setup filter button
        buttonFilter.setOnClickListener {
            loadAndFilterTransactions()
        }

        // Setup back button
        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        // Load initial data
        loadAndFilterTransactions()
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateDateButtons()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtons() {
        buttonStartDate.text = dateFormat.format(startDate.time)
        buttonEndDate.text = dateFormat.format(endDate.time)
    }

    private fun loadAndFilterTransactions() {
        try {
            // Load all transactions
            allTransactions = TransactionManager.loadTransactions(transactionsFile)

            // Filter by date range
            val startDateStr = dateFormat.format(startDate.time)
            val endDateStr = dateFormat.format(endDate.time)

            filteredTransactions = allTransactions.filter { transaction ->
                val transactionDate = transaction.dateTime.substring(0, 10) // Extract date part
                transactionDate >= startDateStr && transactionDate <= endDateStr
            }

            // Update summary cards
            updateSummaryCards()

            // Setup tabs and viewpager
            setupTabs()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSummaryCards() {
        val inTransactions = filteredTransactions.filter { it.transactionType == TransactionType.IN }
        val outTransactions = filteredTransactions.filter { it.transactionType == TransactionType.OUT }

        val totalInAmount = inTransactions.sumOf { it.getTotalValue() }
        val totalOutAmount = outTransactions.sumOf { it.getTotalValue() }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("th", "TH"))

        // Update IN card
        textTotalIn.text = currencyFormat.format(totalInAmount)
        textTotalInCount.text = "${inTransactions.size} ${getString(R.string.transactions)}"

        // Update OUT card
        textTotalOut.text = currencyFormat.format(totalOutAmount)
        textTotalOutCount.text = "${outTransactions.size} ${getString(R.string.transactions)}"
    }

    private fun setupTabs() {
        // Setup ViewPager2 with adapter
        val adapter = ReportPagerAdapter(this, filteredTransactions)
        viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_stock_in)
                1 -> getString(R.string.tab_stock_out)
                else -> ""
            }
        }.attach()
    }
}

/**
 * ViewPager2 Adapter สำหรับจัดการ Tabs
 */
class ReportPagerAdapter(
    activity: StockReportActivity,
    private val transactions: List<Transaction>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2 // 2 tabs: IN and OUT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReportFragment.newInstance(TransactionType.IN, transactions)
            1 -> ReportFragment.newInstance(TransactionType.OUT, transactions)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

/**
 * Fragment สำหรับแสดงรายการใน Tab แต่ละอัน
 */
class ReportFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var transactionType: TransactionType
    private var transactions: List<Transaction> = emptyList()

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_TRANSACTIONS = "transactions"

        fun newInstance(type: TransactionType, transactions: List<Transaction>): ReportFragment {
            val fragment = ReportFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type.name)
            // Convert transactions to array for Bundle
            fragment.transactionType = type
            fragment.transactions = transactions
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        recyclerView = view.findViewById(R.id.recyclerReport)
        textEmpty = view.findViewById(R.id.textEmpty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadData()

        return view
    }

    private fun loadData() {
        // Filter transactions by type
        val filteredByType = transactions.filter { it.transactionType == transactionType }

        if (filteredByType.isEmpty()) {
            recyclerView.visibility = View.GONE
            textEmpty.visibility = View.VISIBLE
            textEmpty.text = getString(R.string.no_transactions)
            return
        }

        // Group by product name and calculate summary
        val groupedData = filteredByType
            .groupBy { it.productName }
            .map { (productName, transactions) ->
                ProductReportSummary(
                    productName = productName,
                    transactionCount = transactions.size,
                    totalQuantity = transactions.sumOf { it.quantity },
                    totalAmount = transactions.sumOf { it.getTotalValue() },
                    transactionType = transactionType
                )
            }
            .sortedByDescending { it.totalAmount }

        // Show data
        recyclerView.visibility = View.VISIBLE
        textEmpty.visibility = View.GONE

        val adapter = ProductReportAdapter(groupedData)
        recyclerView.adapter = adapter
    }
}
