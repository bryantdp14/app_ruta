package com.cabel.rutacabel.services

import android.graphics.*
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.*

class TicketRenderer {

    companion object {
        private const val TICKET_WIDTH = 384 // 58mm at 203 DPI
        private const val PADDING = 16f
        private const val LINE_SPACING = 4f
        private const val TEXT_SIZE_NORMAL = 20f
        private const val TEXT_SIZE_LARGE = 28f
    }

    fun renderTicket(ticket: TicketData): Bitmap {
        // Calculate required height
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = TEXT_SIZE_NORMAL
        
        val headerHeight = 280f
        val itemHeight = 80f * ticket.items.size
        val footerHeight = 350f
        val totalHeight = (headerHeight + itemHeight + footerHeight).toInt()

        val bitmap = Bitmap.createBitmap(TICKET_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.BLACK
        
        var y = PADDING + 30f

        // ═══════ HEADER ═══════
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        textPaint.textSize = TEXT_SIZE_LARGE
        canvas.drawText(ticket.companyName, TICKET_WIDTH / 2f, y, textPaint)
        
        y += 40f
        textPaint.textSize = TEXT_SIZE_NORMAL
        canvas.drawText("NOTA DE PEDIDO", TICKET_WIDTH / 2f, y, textPaint)
        
        y += 30f
        textPaint.isFakeBoldText = false
        canvas.drawLine(PADDING, y, TICKET_WIDTH - PADDING, y, textPaint)
        
        y += 30f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Folio: ${ticket.getFolio()}", PADDING, y, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(ticket.getFormattedDate(), TICKET_WIDTH - PADDING, y, textPaint)
        
        y += 30f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Cliente: ${ticket.clientName}", PADDING, y, textPaint)
        
        y += 30f
        canvas.drawText("Vendedor: ${ticket.sellerName}", PADDING, y, textPaint)
        
        y += 30f
        canvas.drawText("Método: ${if (ticket.isCredit) "Crédito" else "Efectivo"}", PADDING, y, textPaint)

        // ═══════ ITEMS HEADER ═══════
        y += 40f
        canvas.drawLine(PADDING, y, TICKET_WIDTH - PADDING, y, textPaint)
        y += 30f
        textPaint.isFakeBoldText = true
        canvas.drawText("DESCRIPCIÓN DE PRODUCTOS", PADDING, y, textPaint)
        
        y += 10f
        canvas.drawLine(PADDING, y, TICKET_WIDTH - PADDING, y, textPaint)
        textPaint.isFakeBoldText = false
        textPaint.textAlign = Paint.Align.LEFT

        // ═══════ ITEMS ═══════
        for (item in ticket.items) {
            y += 40f
            // Line 1: Code - Description
            textPaint.isFakeBoldText = true
            val line1 = "${item.code} - ${item.description}"
            val truncatedLine1 = if (line1.length > 28) line1.substring(0, 25) + ".." else line1
            canvas.drawText(truncatedLine1, PADDING, y, textPaint)
            
            y += 30f
            // Line 2: Qty x Price ... Total
            textPaint.isFakeBoldText = false
            val qtyPrice = "${item.quantity} x $${String.format("%.2f", item.unitPrice)}"
            canvas.drawText(qtyPrice, PADDING + 20f, y, textPaint)
            
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$${String.format("%.2f", item.total)}", TICKET_WIDTH - PADDING, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
        }

        // ═══════ TOTALS ═══════
        y += 40f
        canvas.drawLine(PADDING, y, TICKET_WIDTH - PADDING, y, textPaint)
        y += 50f
        textPaint.isFakeBoldText = true
        textPaint.textSize = TEXT_SIZE_LARGE
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL: $${String.format("%.2f", ticket.total)}", TICKET_WIDTH - PADDING, y, textPaint)

        // ═══════ FOOTER ═══════
        y += 60f
        textPaint.textSize = TEXT_SIZE_NORMAL
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("¡Gracias por su compra!", TICKET_WIDTH / 2f, y, textPaint)
        
        y += 40f
        canvas.drawLine(PADDING, y, TICKET_WIDTH - PADDING, y, textPaint)

        return bitmap
    }
}
