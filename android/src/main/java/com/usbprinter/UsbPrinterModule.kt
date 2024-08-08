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

  override fun close() {
    adapter?.closeConnectionIfExists()
  }

  override fun RawData(base64Data: String, promise: Promise) {
    adapter?.printRawData(base64Data, promise)
  }

  override fun printImageURL(imageUrl: String, imageWidth: Double, imageHeight: Double, promise: Promise) {
    adapter?.printImageData(imageUrl, imageWidth.toInt(), imageHeight.toInt(), promise)
  }



  override fun printImageBase64(base64: String, imageWidth: Double, imageHeight: Double, promise: Promise) {
    val decodedString = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
    val decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    adapter?.printImageBase64(decodedByte, imageWidth.toInt(), imageHeight.toInt(), promise)
  }


  override fun printCut(tailingLine: Boolean, beep: Boolean, promise: Promise) {
    adapter?.printCut(tailingLine, beep, promise)
  }
}
