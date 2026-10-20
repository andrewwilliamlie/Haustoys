package com.haustoys.pos.print

import android.content.Context
import com.pax.dal.IPrinter
import com.pax.dal.entity.EFontTypeAscii
import com.pax.dal.entity.EFontTypeExtCode
import com.pax.neptunelite.api.NeptuneLiteUser

fun createPrinter(context: Context): ReceiptPrinter = PaxPrinter(context.applicationContext)

/** Built-in thermal printer of PAX terminals (A920, A910, A930, ...) via NeptuneLite. */
private class PaxPrinter(private val context: Context) : ReceiptPrinter {
    override val name = "PAX built-in printer"

    @Volatile
    private var printer: IPrinter? = null

    private fun printer(): IPrinter = printer ?: synchronized(this) {
        printer ?: run {
            val dal = try {
                NeptuneLiteUser.getInstance().getDal(context)
            } catch (e: Exception) {
                throw PrinterException("PAX service unavailable: ${e.message}")
            } ?: throw PrinterException("PAX service unavailable (is this a PAX terminal?)")
            dal.printer.also { printer = it }
        }
    }

    override fun status(): PrinterStatus = try {
        val code = printer().status
        PrinterStatus(code == 0, describe(code))
    } catch (e: Exception) {
        PrinterStatus(false, e.message ?: e.toString())
    }

    override fun print(text: String) {
        val code = try {
            val p = printer()
            p.init()
            // 12x24 ASCII font = 32 characters per line on 58mm paper.
            p.fontSet(EFontTypeAscii.FONT_12_24, EFontTypeExtCode.FONT_24_24)
            p.spaceSet(0.toByte(), 0.toByte())
            p.printStr(text + "\n", null)
            p.step(150) // feed paper past the tear bar
            p.start()
        } catch (e: PrinterException) {
            throw e
        } catch (e: Exception) {
            throw PrinterException(e.message ?: e.toString())
        }
        if (code != 0) throw PrinterException(describe(code))
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "Ready"
        1 -> "Printer is busy"
        2 -> "Out of paper"
        3 -> "Print data format error"
        4 -> "Printer malfunction"
        8 -> "Printer overheated"
        9 -> "Battery voltage too low to print"
        -16 -> "Printing unfinished"
        -4 -> "Printer font library missing"
        -2 -> "Print data too long"
        else -> "Printer error ($code)"
    }
}
