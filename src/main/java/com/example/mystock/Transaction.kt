package com.example.mystock

import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction - ประวัติการเคลื่อนไหวสต๊อก
 * บันทึกทุกครั้งที่มีการ Stock IN/OUT
 */
data class Transaction(
    val dateTime: String = getCurrentDateTime(),
    val productName: String,
    val transactionType: TransactionType,    // IN หรือ OUT
    val quantity: Int,                       // จำนวนที่เพิ่ม/ลด
    val pricePerUnit: Double,
    val notes: String = "",
    val stockBefore: Int,                    // สต๊อกก่อนทำรายการ
    val stockAfter: Int                      // สต๊อกหลังทำรายการ (สำคัญ!)
) {
    /**
     * คำนวณมูลค่ารายการนี้
     */
    fun getTotalValue(): Double = quantity * pricePerUnit

    /**
     * แสดงการเปลี่ยนแปลงสต๊อก
     */
    fun getStockChange(): String {
        return when (transactionType) {
            TransactionType.IN -> "+$quantity"
            TransactionType.OUT -> "-$quantity"
        }
    }

    companion object {
        fun getCurrentDateTime(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return dateFormat.format(Date())
        }

        /**
         * สร้าง Transaction จาก CSV line
         */
        fun fromCsvLine(line: String): Transaction? {
            try {
                val parts = CsvHelper.parseCsvLine(line)
                if (parts.size < 8) return null

                return Transaction(
                    dateTime = parts[0].trim(),
                    productName = parts[1].trim(),
                    transactionType = TransactionType.valueOf(parts[2].trim()),
                    quantity = parts[3].trim().toIntOrNull() ?: 0,
                    pricePerUnit = parts[4].trim().toDoubleOrNull() ?: 0.0,
                    notes = parts[5].trim(),
                    stockBefore = parts[6].trim().toIntOrNull() ?: 0,
                    stockAfter = parts[7].trim().toIntOrNull() ?: 0
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * แปลง Transaction เป็น CSV line
         */
        fun toCsvLine(transaction: Transaction): String {
            return listOf(
                transaction.dateTime,
                CsvHelper.escapeField(transaction.productName),
                transaction.transactionType.name,
                transaction.quantity.toString(),
                transaction.pricePerUnit.toString(),
                CsvHelper.escapeField(transaction.notes),
                transaction.stockBefore.toString(),
                transaction.stockAfter.toString()
            ).joinToString(",")
        }
    }
}

/**
 * ประเภทรายการ
 */
enum class TransactionType {
    IN,   // รับเข้า
    OUT   // เบิกออก
}
