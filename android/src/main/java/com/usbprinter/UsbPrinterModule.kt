package com.usbprinter

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.WritableArray

import com.usbprinter.adapter.PrinterAdapter
import com.usbprinter.adapter.USBPrinterAdapter
import com.usbprinter.adapter.USBPrinterDeviceId
import com.facebook.react.bridge.Promise

@ReactModule(name = UsbPrinterModule.NAME)
class UsbPrinterModule(reactContext: ReactApplicationContext) :
  NativeUsbPrinterSpec(reactContext) {

  private val context = reactContext
  private var adapter: PrinterAdapter? = null

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = "UsbPrinter"
  }

  override fun init() {
      adapter = USBPrinterAdapter.getInstance();
      adapter?.init(context)
  }

  override fun getDeviceList() : WritableArray {
    adapter?.init(context)

    val printerDevices = adapter?.getDeviceList()
    val pairedDeviceList:WritableArray = Arguments.createArray()

    if (printerDevices?.size!! > 0) {
      for (printerDevice in printerDevices) {
        pairedDeviceList.pushMap(printerDevice.toRNWritableMap())
      }
    }

   return pairedDeviceList
  }

  override fun connect(vendorId: Double, productId: Double):  String {
    val result= adapter?.selectDevice(
        USBPrinterDeviceId.valueOf(vendorId.toInt(), productId.toInt())
      )

    return result ?: "No device found"
  }

  override fun RawData(base64Data: String, promise: Promise) {
    adapter?.printRawData(base64Data, promise)
  }

}

