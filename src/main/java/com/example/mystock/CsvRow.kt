package com.example.mystock

data class CsvRow(
    val dateTime: String,
    val productName: String,        // Previously 'data', now more descriptive
    val category: String,            // Previously 'user', now 'category'
    val location: String,
    val quantity: Int = 0,
    val pricePerUnit: Double = 0.0,
    val minStock: Int = 0,
    val imagePath: String = "",
    val transactionType: String = "IN",  // "IN" or "OUT"
    val notes: String = ""
) {
    // Calculate total value for this item
    fun getTotalValue(): Double = quantity * pricePerUnit

    // Check if stock is low
    fun isLowStock(): Boolean = quantity <= minStock && minStock > 0

    // Legacy constructor for backward compatibility with old CSV files
    constructor(dateTime: String, user: String, location: String, data: String) : this(
        dateTime = dateTime,
        productName = data,
        category = user,
        location = location,
        quantity = 0,
        pricePerUnit = 0.0,
        minStock = 0,
        imagePath = "",
        transactionType = "IN",
        notes = ""
    )
}
