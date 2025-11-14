package com.example.mystock

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * ProductManager - จัดการไฟล์ products.csv
 * รักษาข้อมูลสินค้าหลักและสต๊อกปัจจุบัน
 */
object ProductManager {

    private const val CSV_HEADER = "ProductName,Category,Location,CurrentStock,PricePerUnit,MinStock,ImagePath,LastUpdated"

    /**
     * โหลดสินค้าทั้งหมด
     */
    fun loadProducts(csvFile: File): List<Product> {
        val products = mutableListOf<Product>()

        if (!csvFile.exists()) {
            return products
        }

        try {
            csvFile.readLines()
                .drop(1) // ข้ามหัวตาราง
                .forEach { line ->
                    if (line.isNotBlank()) {
                        Product.fromCsvLine(line)?.let { products.add(it) }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return products
    }

    /**
     * หาสินค้าตามชื่อ
     */
    fun findProduct(csvFile: File, productName: String): Product? {
        return loadProducts(csvFile).find {
            it.productName.equals(productName, ignoreCase = true)
        }
    }

    /**
     * เพิ่มสินค้าใหม่
     */
    fun addProduct(csvFile: File, product: Product): Boolean {
        return try {
            // เช็คว่ามีอยู่แล้วหรือไม่
            if (findProduct(csvFile, product.productName) != null) {
                return false // มีอยู่แล้ว
            }

            val isNewFile = !csvFile.exists()
            val fileWriter = OutputStreamWriter(FileOutputStream(csvFile, true), "UTF-8")

            if (isNewFile) {
                fileWriter.write('\uFEFF'.code)  // BOM for UTF-8
                fileWriter.append("$CSV_HEADER\n")
            }

            fileWriter.append(Product.toCsvLine(product) + "\n")
            fileWriter.flush()
            fileWriter.close()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * อัพเดทสินค้า (เปลี่ยนสต๊อก หรือข้อมูลอื่น)
     */
    fun updateProduct(csvFile: File, updatedProduct: Product): Boolean {
        return try {
            val allProducts = loadProducts(csvFile).toMutableList()
            val index = allProducts.indexOfFirst {
                it.productName.equals(updatedProduct.productName, ignoreCase = true)
            }

            if (index == -1) {
                return false // ไม่พบสินค้า
            }

            allProducts[index] = updatedProduct
            rewriteProductsCsv(csvFile, allProducts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ลบสินค้า
     */
    fun deleteProduct(csvFile: File, productName: String): Boolean {
        return try {
            val allProducts = loadProducts(csvFile).toMutableList()
            val removed = allProducts.removeAll {
                it.productName.equals(productName, ignoreCase = true)
            }

            if (removed) {
                rewriteProductsCsv(csvFile, allProducts)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * เพิ่มสต๊อก (Stock IN)
     * @return Product ที่อัพเดทแล้ว หรือ null ถ้าไม่พบสินค้า
     */
    fun stockIn(csvFile: File, productName: String, quantity: Int): Product? {
        val product = findProduct(csvFile, productName) ?: return null
        val updatedProduct = product.addStock(quantity)

        return if (updateProduct(csvFile, updatedProduct)) {
            updatedProduct
        } else {
            null
        }
    }

    /**
     * เบิกสต๊อก (Stock OUT)
     * @return Product ที่อัพเดทแล้ว, null ถ้าไม่พบสินค้า หรือสต๊อกไม่พอ
     */
    fun stockOut(csvFile: File, productName: String, quantity: Int): Product? {
        val product = findProduct(csvFile, productName) ?: return null
        val updatedProduct = product.removeStock(quantity) ?: return null // สต๊อกไม่พอ

        return if (updateProduct(csvFile, updatedProduct)) {
            updatedProduct
        } else {
            null
        }
    }

    /**
     * เขียนทับไฟล์ products.csv ทั้งหมด
     */
    private fun rewriteProductsCsv(csvFile: File, products: List<Product>) {
        if (csvFile.exists()) {
            csvFile.delete()
        }

        val fileWriter = OutputStreamWriter(FileOutputStream(csvFile, false), "UTF-8")
        fileWriter.write('\uFEFF'.code)  // BOM for UTF-8
        fileWriter.append("$CSV_HEADER\n")

        for (product in products) {
            fileWriter.append(Product.toCsvLine(product) + "\n")
        }

        fileWriter.flush()
        fileWriter.close()
    }

    /**
     * คำนวณสถิติ
     */
    fun calculateStatistics(csvFile: File): ProductStatistics {
        val products = loadProducts(csvFile)

        val totalItems = products.size
        val totalQuantity = products.sumOf { it.currentStock }
        val totalValue = products.sumOf { it.getTotalValue() }
        val lowStockItems = products.filter { it.isLowStock() }
        val categoriesCount = products.groupBy { it.category }.size

        return ProductStatistics(
            totalItems = totalItems,
            totalQuantity = totalQuantity,
            totalValue = totalValue,
            lowStockCount = lowStockItems.size,
            categoriesCount = categoriesCount,
            lowStockProducts = lowStockItems
        )
    }
}

/**
 * สถิติสินค้า
 */
data class ProductStatistics(
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: Double,
    val lowStockCount: Int,
    val categoriesCount: Int,
    val lowStockProducts: List<Product>
)
