package com.example.mystock

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object CsvHelper {

    private const val CSV_HEADER = "DateTime,ProductName,Category,Location,Quantity,PricePerUnit,MinStock,ImagePath,TransactionType,Notes"

    /**
     * Save a row to CSV file with new structure
     */
    fun saveToCsv(csvFile: File, row: CsvRow) {
        try {
            val isNewFile = !csvFile.exists()
            val fileWriter = OutputStreamWriter(FileOutputStream(csvFile, true), "UTF-8")

            if (isNewFile) {
                fileWriter.write('\uFEFF'.code)  // BOM for UTF-8
                fileWriter.append("$CSV_HEADER\n")
            }

            // Escape commas and quotes
            val line = listOf(
                row.dateTime,
                escapeField(row.productName),
                escapeField(row.category),
                escapeField(row.location),
                row.quantity.toString(),
                row.pricePerUnit.toString(),
                row.minStock.toString(),
                escapeField(row.imagePath),
                row.transactionType,
                escapeField(row.notes)
            ).joinToString(",")

            fileWriter.append("$line\n")
            fileWriter.flush()
            fileWriter.close()

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Load all rows from CSV file with backward compatibility
     */
    fun loadFromCsv(csvFile: File): List<CsvRow> {
        val rows = mutableListOf<CsvRow>()

        if (!csvFile.exists()) {
            return rows
        }

        try {
            val lines = csvFile.readLines().drop(1) // Skip header

            for (line in lines) {
                if (line.isBlank()) continue

                val parts = parseCsvLine(line)

                // Check if it's old format (4 columns) or new format (10 columns)
                val row = if (parts.size == 4) {
                    // Old format: DateTime, Detail1(user), Detail2(location), Data
                    CsvRow(
                        dateTime = parts[0].trim(),
                        category = parts[1].trim(),
                        location = parts[2].trim(),
                        productName = parts[3].trim()
                    )
                } else if (parts.size >= 10) {
                    // New format
                    CsvRow(
                        dateTime = parts[0].trim(),
                        productName = parts[1].trim(),
                        category = parts[2].trim(),
                        location = parts[3].trim(),
                        quantity = parts[4].trim().toIntOrNull() ?: 0,
                        pricePerUnit = parts[5].trim().toDoubleOrNull() ?: 0.0,
                        minStock = parts[6].trim().toIntOrNull() ?: 0,
                        imagePath = parts[7].trim(),
                        transactionType = parts[8].trim(),
                        notes = parts.getOrNull(9)?.trim() ?: ""
                    )
                } else {
                    continue // Skip malformed lines
                }

                rows.add(row)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return rows
    }

    /**
     * Update an existing row in CSV file
     */
    fun updateRow(csvFile: File, oldRow: CsvRow, newRow: CsvRow) {
        val allRows = loadFromCsv(csvFile).toMutableList()
        val index = allRows.indexOfFirst {
            it.dateTime == oldRow.dateTime &&
            it.productName == oldRow.productName
        }

        if (index != -1) {
            allRows[index] = newRow
            rewriteCsv(csvFile, allRows)
        }
    }

    /**
     * Delete a row from CSV file
     */
    fun deleteRow(csvFile: File, row: CsvRow) {
        val allRows = loadFromCsv(csvFile).toMutableList()
        allRows.removeAll {
            it.dateTime == row.dateTime &&
            it.productName == row.productName
        }
        rewriteCsv(csvFile, allRows)
    }

    /**
     * Rewrite entire CSV file with new data
     */
    private fun rewriteCsv(csvFile: File, rows: List<CsvRow>) {
        if (csvFile.exists()) {
            csvFile.delete()
        }

        val fileWriter = OutputStreamWriter(FileOutputStream(csvFile, false), "UTF-8")
        fileWriter.write('\uFEFF'.code)  // BOM for UTF-8
        fileWriter.append("$CSV_HEADER\n")

        for (row in rows) {
            val line = listOf(
                row.dateTime,
                escapeField(row.productName),
                escapeField(row.category),
                escapeField(row.location),
                row.quantity.toString(),
                row.pricePerUnit.toString(),
                row.minStock.toString(),
                escapeField(row.imagePath),
                row.transactionType,
                escapeField(row.notes)
            ).joinToString(",")

            fileWriter.append("$line\n")
        }

        fileWriter.flush()
        fileWriter.close()
    }

    /**
     * Escape CSV field (handle commas and quotes)
     */
    fun escapeField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    /**
     * Parse a CSV line considering quoted fields
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val char = line[i]

            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        // Skip next quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().removeSurrounding("\""))
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }

        result.add(current.toString().removeSurrounding("\""))
        return result
    }

    /**
     * Calculate statistics from rows
     */
    fun calculateStatistics(rows: List<CsvRow>): StockStatistics {
        val totalItems = rows.size
        val totalQuantity = rows.sumOf { it.quantity }
        val totalValue = rows.sumOf { it.getTotalValue() }
        val lowStockItems = rows.filter { it.isLowStock() }
        val categoriesCount = rows.groupBy { it.category }.size

        return StockStatistics(
            totalItems = totalItems,
            totalQuantity = totalQuantity,
            totalValue = totalValue,
            lowStockCount = lowStockItems.size,
            categoriesCount = categoriesCount,
            lowStockItems = lowStockItems
        )
    }
}

data class StockStatistics(
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: Double,
    val lowStockCount: Int,
    val categoriesCount: Int,
    val lowStockItems: List<CsvRow>
)
