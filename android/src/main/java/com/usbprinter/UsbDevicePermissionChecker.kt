package com.usbprinter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log

object UsbDevicePermissionChecker {
    private const val ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION"
    private const val TAG = "UsbDevicePermissionChecker"

    fun checkDeviceAndPermission(context: Context, productId: Int): UsbDevice? {
        val devices = UsbDeviceHelper.getConnectedUsbDevices(context)
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = devices.firstOrNull { it.productId == productId }
        if (device == null) {
            Log.w(TAG, "Device with productId $productId not found")
            return null
        }

        Log.d(TAG, "Found device: ${device.deviceName}, vendorId: ${device.vendorId}, productId: ${device.productId}")

        if (!usbManager.hasPermission(device)) {
            Log.d(TAG, "Requesting permission for device")
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_USB_PERMISSION) {
                        try {
                            context?.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error unregistering receiver", e)
                        }
                        Log.d(TAG, "Permission response received")
                    }
                }
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(receiver, filter)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering receiver", e)
            }
            usbManager.requestPermission(device, permissionIntent)
            return null
        }

        return device
    }
}
