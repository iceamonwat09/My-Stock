package com.example.mystock

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * TransactionManager - จัดการไฟล์ transactions.csv
 * บันทึกประวัติการเคลื่อนไหวสต๊อกทั้งหมด
 */
object TransactionManager {

    private const val CSV_HEADER = "DateTime,ProductName,TransactionType,Quantity,PricePerUnit,Notes,StockBefore,StockAfter"

    /**
     * บันทึกรายการใหม่
     */
    fun addTransaction(csvFile: File, transaction: Transaction): Boolean {
        return try {
            val isNewFile = !csvFile.exists()
            val fileWriter = OutputStreamWriter(FileOutputStream(csvFile, true), "UTF-8")

            if (isNewFile) {
                fileWriter.write('\uFEFF'.code)  // BOM for UTF-8
                fileWriter.append("$CSV_HEADER\n")
            }

            fileWriter.append(Transaction.toCsvLine(transaction) + "\n")
            fileWriter.flush()
            fileWriter.close()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * โหลดประวัติทั้งหมด
     */
    fun loadTransactions(csvFile: File): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        if (!csvFile.exists()) {
            return transactions
        }

        try {
            csvFile.readLines()
                .drop(1) // ข้ามหัวตาราง
                .forEach { line ->
                    if (line.isNotBlank()) {
                        Transaction.fromCsvLine(line)?.let { transactions.add(it) }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // เรียงจากใหม่ไปเก่า
        return transactions.sortedByDescending { it.dateTime }
    }

    /**
     * โหลดประวัติของสินค้าใดสินค้าหนึ่ง
     */
    fun loadTransactionsByProduct(csvFile: File, productName: String): List<Transaction> {
        return loadTransactions(csvFile).filter {
            it.productName.equals(productName, ignoreCase = true)
        }
    }

    /**
     * โหลดประวัติตามประเภท (IN หรือ OUT)
     */
    fun loadTransactionsByType(csvFile: File, type: TransactionType): List<Transaction> {
        return loadTransactions(csvFile).filter {
            it.transactionType == type
        }
    }

    /**
     * โหลดประวัติในช่วงวันที่
     */
    fun loadTransactionsByDateRange(
        csvFile: File,
        startDate: String,
        endDate: String
    ): List<Transaction> {
        return loadTransactions(csvFile).filter { transaction ->
            val transDate = transaction.dateTime.substring(0, 10) // YYYY-MM-DD
            transDate >= startDate && transDate <= endDate
        }
    }

    /**
     * ลบประวัติทั้งหมด
     */
    fun clearAllTransactions(csvFile: File): Boolean {
        return try {
            if (csvFile.exists()) {
                csvFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * คำนวณสถิติการเคลื่อนไหว
     */
    fun calculateTransactionStats(csvFile: File): TransactionStats {
        val transactions = loadTransactions(csvFile)

        val totalTransactions = transactions.size
        val stockInCount = transactions.count { it.transactionType == TransactionType.IN }
        val stockOutCount = transactions.count { it.transactionType == TransactionType.OUT }
        val totalIn = transactions.filter { it.transactionType == TransactionType.IN }
            .sumOf { it.quantity }
        val totalOut = transactions.filter { it.transactionType == TransactionType.OUT }
            .sumOf { it.quantity }

        return TransactionStats(
            totalTransactions = totalTransactions,
            stockInCount = stockInCount,
            stockOutCount = stockOutCount,
            totalQuantityIn = totalIn,
            totalQuantityOut = totalOut
        )
    }
}

/**
 * สถิติการเคลื่อนไหว
 */
data class TransactionStats(
    val totalTransactions: Int,
    val stockInCount: Int,
    val stockOutCount: Int,
    val totalQuantityIn: Int,
    val totalQuantityOut: Int
)
