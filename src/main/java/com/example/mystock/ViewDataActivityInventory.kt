package com.example.mystock

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File

/**
 * ViewDataActivityInventory - แสดงข้อมูลแบบ Tabs
 * Tab 1: Products (สินค้าทั้งหมด + สต๊อกปัจจุบัน)
 * Tab 2: Transactions (ประวัติการเคลื่อนไหว)
 */
class ViewDataActivityInventory : BaseActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    lateinit var productsFile: File
    lateinit var transactionsFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_data_inventory)

        // Initialize files
        val myFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        productsFile = File(myFolder, "products.csv")
        transactionsFile = File(myFolder, "transactions.csv")

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "📦 สินค้า"
                1 -> "📜 ประวัติ"
                else -> ""
            }
        }.attach()
    }

    private inner class ViewPagerAdapter(activity: ViewDataActivityInventory) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProductsFragment()
                1 -> TransactionsFragment()
                else -> ProductsFragment()
            }
        }
    }
}
