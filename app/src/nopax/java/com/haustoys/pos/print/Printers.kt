package com.haustoys.pos.print

import android.content.Context

/** Built without the PAX SDK (app/libs has no NeptuneLite jar): receipts are shown on screen only. */
fun createPrinter(context: Context): ReceiptPrinter = NoPrinter

private object NoPrinter : ReceiptPrinter {
    override val name = "No printer"

    override fun status() = PrinterStatus(false, "PAX SDK not bundled - add the NeptuneLite jar to app/libs and rebuild")

    override fun print(text: String) {
        throw PrinterException("Printing unavailable: this build has no PAX SDK")
    }
}
