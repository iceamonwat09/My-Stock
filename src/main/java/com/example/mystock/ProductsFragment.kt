package com.example.mystock

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

/**
 * ProductsFragment - แสดงสินค้าทั้งหมดพร้อมสต๊อกปัจจุบัน
 */
class ProductsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var editTextSearch: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var buttonClearFilter: Button
    private lateinit var textViewTotal: TextView

    private var allProducts = mutableListOf<Product>()
    private var filteredProducts = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewProducts)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        buttonClearFilter = view.findViewById(R.id.buttonClearFilter)
        textViewTotal = view.findViewById(R.id.textViewTotal)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProductAdapter(
            filteredProducts,
            onEdit = { product -> showEditDialog(product) },
            onDelete = { product -> showDeleteDialog(product) },
            onViewHistory = { product -> viewProductHistory(product) }
        )
        recyclerView.adapter = adapter

        loadData()
        setupFilters()

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
        allProducts.clear()
        allProducts.addAll(ProductManager.loadProducts(activity.productsFile))

        setupCategorySpinner()
        applyFilters()
    }

    private fun setupCategorySpinner() {
        val categories = mutableListOf(getString(R.string.all))
        categories.addAll(allProducts.map { it.category }.distinct().sorted())

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFilters() {
        editTextSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun applyFilters() {
        filteredProducts.clear()

        val searchText = editTextSearch.text.toString().lowercase()
        val selectedCategory = spinnerCategory.selectedItem?.toString()

        filteredProducts.addAll(allProducts.filter { product ->
            val matchSearch = searchText.isEmpty() ||
                    product.productName.lowercase().contains(searchText) ||
                    product.category.lowercase().contains(searchText) ||
                    product.location.lowercase().contains(searchText)

            val matchCategory = selectedCategory == getString(R.string.all) ||
                    product.category == selectedCategory

            matchSearch && matchCategory
        })

        adapter.updateData(filteredProducts)
        updateTotal()
    }

    private fun clearFilters() {
        editTextSearch.text.clear()
        spinnerCategory.setSelection(0)
        applyFilters()
    }

    private fun updateTotal() {
        textViewTotal.text = getString(R.string.showing_rows, filteredProducts.size, allProducts.size)
    }

    private fun showEditDialog(product: Product) {
        val activity = activity ?: return
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_product, null)

        val editCategory = dialogView.findViewById<TextInputEditText>(R.id.editCategory)
        val editLocation = dialogView.findViewById<TextInputEditText>(R.id.editLocation)
        val editPrice = dialogView.findViewById<TextInputEditText>(R.id.editPrice)
        val editMinStock = dialogView.findViewById<TextInputEditText>(R.id.editMinStock)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreviewEdit)
        val textProductName = dialogView.findViewById<TextView>(R.id.textProductName)
        val textCurrentStock = dialogView.findViewById<TextView>(R.id.textCurrentStock)

        // Fill current values
        textProductName.text = product.productName
        textCurrentStock.text = "สต๊อกปัจจุบัน: ${product.currentStock} ชิ้น"
        editCategory.setText(product.category)
        editLocation.setText(product.location)
        editPrice.setText(product.pricePerUnit.toString())
        editMinStock.setText(product.minStock.toString())

        // Show image if exists
        if (product.imagePath.isNotEmpty() && java.io.File(product.imagePath).exists()) {
            imagePreview.setImageURI(Uri.fromFile(java.io.File(product.imagePath)))
            imagePreview.visibility = View.VISIBLE
        }

        AlertDialog.Builder(activity)
            .setTitle("แก้ไขสินค้า")
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val updatedProduct = product.updateInfo(
                    category = editCategory.text.toString(),
                    location = editLocation.text.toString(),
                    pricePerUnit = editPrice.text.toString().toDoubleOrNull(),
                    minStock = editMinStock.text.toString().toIntOrNull()
                )

                val activityInv = activity as? ViewDataActivityInventory
                if (activityInv != null) {
                    if (ProductManager.updateProduct(activityInv.productsFile, updatedProduct)) {
                        Toast.makeText(activity, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteDialog(product: Product) {
        val activity = activity ?: return

        AlertDialog.Builder(activity)
            .setTitle(getString(R.string.delete))
            .setMessage("ลบสินค้า: ${product.productName}?\n\nสต๊อกปัจจุบัน: ${product.currentStock} ชิ้น\n\n⚠️ การลบจะไม่สามารถกู้คืนได้")
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val activityInv = activity as? ViewDataActivityInventory
                if (activityInv != null) {
                    if (ProductManager.deleteProduct(activityInv.productsFile, product.productName)) {
                        Toast.makeText(activity, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun viewProductHistory(product: Product) {
        val intent = Intent(requireContext(), TransactionHistoryActivity::class.java)
        intent.putExtra("PRODUCT_NAME", product.productName)
        startActivity(intent)
    }
}
