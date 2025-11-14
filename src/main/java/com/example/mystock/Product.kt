package com.example.mystock

import java.text.SimpleDateFormat
import java.util.*

/**
 * Product - สินค้าหลัก (Master Data)
 * เก็บข้อมูลสินค้าและสต๊อกปัจจุบัน
 */
data class Product(
    val productName: String,
    val category: String = "",
    val location: String = "",
    val currentStock: Int = 0,           // สต๊อกปัจจุบัน (สำคัญ!)
    val pricePerUnit: Double = 0.0,
    val minStock: Int = 0,                // ค่าเริ่มต้น 0
    val imagePath: String = "",
    val lastUpdated: String = getCurrentDateTime()
) {
    /**
     * คำนวณมูลค่าสต๊อกปัจจุบัน
     */
    fun getTotalValue(): Double = currentStock * pricePerUnit

    /**
     * เช็คว่าสต๊อกต่ำกว่าขั้นต่ำหรือไม่
     */
    fun isLowStock(): Boolean = minStock > 0 && currentStock <= minStock

    /**
     * เพิ่มสต๊อก (Stock IN)
     */
    fun addStock(quantity: Int): Product {
        return this.copy(
            currentStock = this.currentStock + quantity,
            lastUpdated = getCurrentDateTime()
        )
    }

    /**
     * ลดสต๊อก (Stock OUT)
     * @return Product ใหม่ถ้าสำเร็จ, null ถ้าสต๊อกไม่พอ
     */
    fun removeStock(quantity: Int): Product? {
        return if (this.currentStock >= quantity) {
            this.copy(
                currentStock = this.currentStock - quantity,
                lastUpdated = getCurrentDateTime()
            )
        } else {
            null // สต๊อกไม่พอ
        }
    }

    /**
     * อัพเดทข้อมูลสินค้า
     */
    fun updateInfo(
        category: String? = null,
        location: String? = null,
        pricePerUnit: Double? = null,
        minStock: Int? = null,
        imagePath: String? = null
    ): Product {
        return this.copy(
            category = category ?: this.category,
            location = location ?: this.location,
            pricePerUnit = pricePerUnit ?: this.pricePerUnit,
            minStock = minStock ?: this.minStock,
            imagePath = imagePath ?: this.imagePath,
            lastUpdated = getCurrentDateTime()
        )
    }

    companion object {
        private fun getCurrentDateTime(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return dateFormat.format(Date())
        }

        /**
         * สร้าง Product จาก CSV line
         */
        fun fromCsvLine(line: String): Product? {
            try {
                val parts = CsvHelper.parseCsvLine(line)
                if (parts.size < 8) return null

                return Product(
                    productName = parts[0].trim(),
                    category = parts[1].trim(),
                    location = parts[2].trim(),
                    currentStock = parts[3].trim().toIntOrNull() ?: 0,
                    pricePerUnit = parts[4].trim().toDoubleOrNull() ?: 0.0,
                    minStock = parts[5].trim().toIntOrNull() ?: 0,
                    imagePath = parts[6].trim(),
                    lastUpdated = parts.getOrNull(7)?.trim() ?: getCurrentDateTime()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * แปลง Product เป็น CSV line
         */
        fun toCsvLine(product: Product): String {
            return listOf(
                CsvHelper.escapeField(product.productName),
                CsvHelper.escapeField(product.category),
                CsvHelper.escapeField(product.location),
                product.currentStock.toString(),
                product.pricePerUnit.toString(),
                product.minStock.toString(),
                CsvHelper.escapeField(product.imagePath),
                product.lastUpdated
            ).joinToString(",")
        }
    }
}
