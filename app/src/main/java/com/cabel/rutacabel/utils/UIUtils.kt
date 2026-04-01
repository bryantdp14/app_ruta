package com.cabel.rutacabel.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.cabel.rutacabel.databinding.DialogSuccessBinding

object UIUtils {

    fun showSuccessDialog(activity: Activity, message: String, onConfirm: () -> Unit = {}) {
        val binding = DialogSuccessBinding.inflate(LayoutInflater.from(activity))
        
        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        builder.setCancelable(false)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        binding.tvMessage.text = message
        
        binding.btnOk.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        
        if (!activity.isFinishing) {
            dialog.show()
        }
    }

    fun toast(context: android.content.Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
