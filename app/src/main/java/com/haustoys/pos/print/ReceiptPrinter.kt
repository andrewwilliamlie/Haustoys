package com.haustoys.pos.print

data class PrinterStatus(val ready: Boolean, val message: String)

class PrinterException(message: String) : Exception(message)

/**
 * A receipt printer. Both calls block (they talk to device hardware), so call
 * them off the main thread. The implementation comes from `createPrinter`,
 * which lives in src/pax (PAX SDK present) or src/nopax (SDK missing).
 */
interface ReceiptPrinter {
    val name: String
    fun status(): PrinterStatus

    /** Prints plain text; throws [PrinterException] on failure. */
    fun print(text: String)
}
