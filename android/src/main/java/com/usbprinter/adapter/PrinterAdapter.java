package com.usbprinter.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.telecom.Call;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Promise;


import java.util.List;


public interface PrinterAdapter {


    public void init(ReactApplicationContext reactContext);

    public List<PrinterDevice> getDeviceList();

    public String selectDevice(PrinterDeviceId printerDeviceId);

    public void closeConnectionIfExists();

    public void printRawData(String rawBase64Data, Promise promise);

    public void printImageData(String imageUrl, int imageWidth, int imageHeight, Callback errorCallback);

    public void printImageBase64(Bitmap imageUrl, int imageWidth, int imageHeight, Callback errorCallback);
}
