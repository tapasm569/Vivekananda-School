package com.vidyamandir.schoolmanager.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Direct ESC/POS printing to an already-paired Bluetooth thermal printer (58mm/80mm). */
object ThermalReceiptPrinter {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun choosePrinterAndPrint(activity: Activity, receiptText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Allow Nearby devices permission, then tap Print again.", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 7301)
            return
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(activity, "This device does not support Bluetooth printing.", Toast.LENGTH_LONG).show()
            return
        }
        if (!adapter.isEnabled) {
            Toast.makeText(activity, "Please turn on Bluetooth and try again.", Toast.LENGTH_LONG).show()
            return
        }
        val devices = try { adapter.bondedDevices?.toList().orEmpty() } catch (_: SecurityException) { emptyList() }
        if (devices.isEmpty()) {
            Toast.makeText(activity, "Pair your thermal printer in Android Bluetooth settings first.", Toast.LENGTH_LONG).show()
            return
        }
        val labels = devices.map { "${it.name ?: "Bluetooth device"}\n${it.address}" }.toTypedArray()
        AlertDialog.Builder(activity)
            .setTitle("Select thermal printer")
            .setItems(labels) { _, which ->
                print(activity, devices[which], receiptText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun print(activity: Activity, device: BluetoothDevice, receiptText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(sppUuid)
                BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                socket.connect()
                socket.outputStream.use { output ->
                    output.write(buildEscPos(receiptText))
                    output.flush()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Receipt sent to ${device.name ?: "printer"}.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Printing failed: ${e.localizedMessage ?: "Check printer connection"}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { socket?.close() } catch (_: Exception) { }
            }
        }
    }

    private fun buildEscPos(receipt: String): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) { bytes.forEach { out.write(it) } }
        fun line(text: String = "") { out.write(text.toByteArray(Charsets.US_ASCII)); out.write(byteArrayOf(10)) }
        cmd(0x1B, 0x40) // initialize
        cmd(0x1B, 0x61, 1) // center
        cmd(0x1B, 0x45, 1)
        cmd(0x1D, 0x21, 0x11)
        line("VIVEKANANDA")
        line("VIDYAMANDIR")
        cmd(0x1D, 0x21, 0x00)
        cmd(0x1B, 0x45, 0)
        line("Dhupguri, Jalpaiguri")
        line("FEE PAYMENT RECEIPT")
        line("================================")
        cmd(0x1B, 0x61, 0)
        receipt.lineSequence().forEach { raw ->
            val text = raw.trimEnd()
            val emphasized = text.startsWith("TOTAL PAID:") || text.startsWith("Receipt No:")
            if (emphasized) cmd(0x1B, 0x45, 1)
            if (text.isEmpty() || text.all { it == '=' || it == '-' }) {
                line(text.take(32))
            } else {
                var remaining = text
                while (remaining.length > 32) {
                    var split = remaining.lastIndexOf(' ', 32)
                    if (split <= 0) split = 32
                    line(remaining.substring(0, split))
                    remaining = remaining.substring(split).trimStart()
                }
                line(remaining)
            }
            if (emphasized) cmd(0x1B, 0x45, 0)
        }
        cmd(0x1B, 0x61, 1)
        line("--------------------------------")
        line("Thank you!")
        line("Please keep this receipt safely.")
        line("\n\n")
        cmd(0x1D, 0x56, 0x41, 0x10) // partial cut (if supported)
        return out.toByteArray()
    }
}
