package com.usbprinter.adapter;

import static com.usbprinter.adapter.UtilsImage.getPixelsSlow;
import static com.usbprinter.adapter.UtilsImage.recollectSlice;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Promise;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class USBPrinterAdapter implements PrinterAdapter {
    @SuppressLint("StaticFieldLeak")
    private static USBPrinterAdapter mInstance;

    private final String LOG_TAG = "RNUSBPrinter";
    private Context mContext;
    private UsbManager mUSBManager;
    private PendingIntent mPermissionIndent;
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mEndPoint;
    private static final String ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION";
    private static final String EVENT_USB_DEVICE_ATTACHED = "usbAttached";

    private final static char ESC_CHAR = 0x1B;
    private static final byte[] SELECT_BIT_IMAGE_MODE = {0x1B, 0x2A, 33};
    private final static byte[] SET_LINE_SPACE_24 = new byte[]{ESC_CHAR, 0x33, 24};
    private final static byte[] SET_LINE_SPACE_32 = new byte[]{ESC_CHAR, 0x33, 32};
    private final static byte[] LINE_FEED = new byte[]{0x0A};
    private static final byte[] CENTER_ALIGN = {0x1B, 0X61, 0X31};

    private USBPrinterAdapter() {
    }

    public static USBPrinterAdapter getInstance() {
        if (mInstance == null) {
            mInstance = new USBPrinterAdapter();
        }
        return mInstance;
    }

    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                handleUsbPermission(intent, context);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                handleUsbDetached(context);
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action) ||
                       UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                notifyUsbDeviceAttached();
            }
        }
    };

    @SuppressLint("UnspecifiedImmutableFlag")
    public void init(ReactApplicationContext reactContext) {
        this.mContext = reactContext;
        this.mUSBManager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);
        this.mPermissionIndent = PendingIntent.getBroadcast(
                this.mContext,
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_MUTABLE | 0
        );

        registerUsbReceiver();
        Log.i(LOG_TAG, "USB Printer Adapter initialized");
    }

    private void registerUsbReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);

        this.mContext.registerReceiver(mUsbDeviceReceiver, filter);
    }

    private void handleUsbPermission(Intent intent, Context context) {
        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (usbDevice != null) {
                Log.i(LOG_TAG, "Permission granted for device " + usbDevice.getDeviceId());
                mUsbDevice = usbDevice;
            }
        } else {
            if (usbDevice != null) {
                Toast.makeText(context, "Permission denied for device " + usbDevice.getDeviceName(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleUsbDetached(Context context) {
        if (mUsbDevice != null) {
            Toast.makeText(context, "USB device disconnected", Toast.LENGTH_LONG).show();
            closeConnectionIfExists();
        }
    }

    private void notifyUsbDeviceAttached() {
        if (mContext != null) {
            ((ReactApplicationContext) mContext).getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(EVENT_USB_DEVICE_ATTACHED, null);
        }
    }

    public void closeConnectionIfExists() {
        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbInterface = null;
            mEndPoint = null;
            mUsbDeviceConnection = null;
        }
    }

    public List<PrinterDevice> getDeviceList() {
        List<PrinterDevice> lists = new ArrayList<>();
        if (mUSBManager == null) {
            sendEvent("USBManager is not initialized while getting device list");
            return lists;
        }

        for (UsbDevice usbDevice : mUSBManager.getDeviceList().values()) {
            lists.add(new USBPrinterDevice(usbDevice));
        }
        return lists;
    }

    @Override
    public String selectDevice(PrinterDeviceId printerDeviceId) {
        if (printerDeviceId == null || mUSBManager == null) {
            return "Failed to select device, device ID or USBManager is null";
        }

        USBPrinterDeviceId usbPrinterDeviceId = (USBPrinterDeviceId) printerDeviceId;
        closeConnectionIfExists();

        if (mUSBManager.getDeviceList().isEmpty()) {
            return "No device found";
        }

        return findAndSelectDevice(usbPrinterDeviceId);
    }

    private String findAndSelectDevice(USBPrinterDeviceId usbPrinterDeviceId) {
        for (UsbDevice usbDevice : mUSBManager.getDeviceList().values()) {
            if (usbDevice.getVendorId() == usbPrinterDeviceId.getVendorId() &&
                usbDevice.getProductId() == usbPrinterDeviceId.getProductId()) {
                mUSBManager.requestPermission(usbDevice, mPermissionIndent);
                return "Success to select device with vendor_id: " + usbPrinterDeviceId.getVendorId() +
                       " product_id: " + usbPrinterDeviceId.getProductId();
            }
        }

        return "Failed to find device with vendor_id: " + usbPrinterDeviceId.getVendorId() +
               " product_id: " + usbPrinterDeviceId.getProductId();
    }

    private boolean openConnection() {
        if (mUsbDevice == null) {
            return false;
        }

        if (mUsbDeviceConnection != null) {
            return true;
        }

        UsbInterface usbInterface = getPrinterInterface();
        if (usbInterface == null) {
            return false;
        }

        UsbEndpoint endPoint = getBulkTransferEndpoint(usbInterface);
        if (endPoint == null) {
            return false;
        }

        return initializeConnection(usbInterface, endPoint);
    }

    private UsbInterface getPrinterInterface() {
        for (int i = 0; i < mUsbDevice.getInterfaceCount(); i++) {
            UsbInterface tempInterface = mUsbDevice.getInterface(i);
            if (tempInterface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                return tempInterface;
            }
        }
        return null;
    }

    private UsbEndpoint getBulkTransferEndpoint(UsbInterface usbInterface) {
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint tempEndPoint = usbInterface.getEndpoint(i);
            if (tempEndPoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                return tempEndPoint;
            }
        }
        return null;
    }

    private boolean initializeConnection(UsbInterface usbInterface, UsbEndpoint endPoint) {
        UsbDeviceConnection connection = mUSBManager.openDevice(mUsbDevice);
        if (connection == null || !connection.claimInterface(usbInterface, true)) {
            if (connection != null) {
                connection.close();
            }
            return false;
        }

        mUsbDeviceConnection = connection;
        mUsbInterface = usbInterface;
        mEndPoint = endPoint;
        return true;
    }

    public void printRawData(String rawData, Promise promise) {
        Log.v(LOG_TAG, "Start to print raw data: " + rawData);

        if (openConnection()) {
            sendRawData(Base64.decode(rawData, Base64.DEFAULT), promise);
        } else {
            rejectPrintPromise(promise, "Failed to connect to device");
        }
    }

    private void sendRawData(byte[] data, Promise promise) {
        int result = mUsbDeviceConnection.bulkTransfer(mEndPoint, data, data.length, 0);
        if (result < 0) {
            rejectPrintPromise(promise, "Failed to print raw data");
        } else {
            promise.resolve("Success to print raw data");
        }
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

            return bitmap;
        } catch (IOException e) {
            Log.e("getBitmapFromURL", "Error fetching image from URL", e);
            return null;
        }
    }

    @Override
    public void printImageData(final String imageUrl, int imageWidth, int imageHeight, Promise promise) {
        Bitmap bitmapImage = getBitmapFromURL(imageUrl);
        handleImagePrint(bitmapImage, imageWidth, imageHeight, promise);
    }

    @Override
    public void printImageBase64(final Bitmap bitmapImage, int imageWidth, int imageHeight, Promise promise) {
        handleImagePrint(bitmapImage, imageWidth, imageHeight, promise);
    }

    private void handleImagePrint(Bitmap bitmapImage, int imageWidth, int imageHeight, Promise promise) {
        if (bitmapImage == null) {
            promise.reject("Image not found");
            return;
        }

        Log.v(LOG_TAG, "Start to print image data");
        if (openConnection()) {
            printImage(bitmapImage, imageWidth, imageHeight, promise);
        } else {
            rejectPrintPromise(promise, "Failed to connect to device");
        }
    }

    private void printImage(Bitmap bitmapImage, int imageWidth, int imageHeight, Promise promise) {
        int[][] pixels = getPixelsSlow(bitmapImage, imageWidth, imageHeight);

        mUsbDeviceConnection.bulkTransfer(mEndPoint, SET_LINE_SPACE_24, SET_LINE_SPACE_24.length, 0);
        mUsbDeviceConnection.bulkTransfer(mEndPoint, CENTER_ALIGN, CENTER_ALIGN.length, 0);

        for (int y = 0; y < pixels.length; y += 24) {
            sendImageSlice(pixels, y);
            mUsbDeviceConnection.bulkTransfer(mEndPoint, LINE_FEED, LINE_FEED.length, 0);
        }

        mUsbDeviceConnection.bulkTransfer(mEndPoint, SET_LINE_SPACE_32, SET_LINE_SPACE_32.length, 0);
        int result = mUsbDeviceConnection.bulkTransfer(mEndPoint, LINE_FEED, LINE_FEED.length, 0);

        if (result < 0) {
            rejectPrintPromise(promise, "Failed to print image data");
        } else {
            promise.resolve("Success to print image data");
        }
    }

    private void sendImageSlice(int[][] pixels, int y) {
        mUsbDeviceConnection.bulkTransfer(mEndPoint, SELECT_BIT_IMAGE_MODE, SELECT_BIT_IMAGE_MODE.length, 0);

        byte[] row = new byte[]{
            (byte) (0x00ff & pixels[y].length),
            (byte) ((0xff00 & pixels[y].length) >> 8)
        };

        mUsbDeviceConnection.bulkTransfer(mEndPoint, row, row.length, 0);

        for (int x = 0; x < pixels[y].length; x++) {
            byte[] slice = recollectSlice(y, x, pixels);
            mUsbDeviceConnection.bulkTransfer(mEndPoint, slice, slice.length, 0);
        }
    }

    public void printCut(boolean tailingLine, boolean beep, Promise promise) {
        if (openConnection()) {
            byte[] command = createCutCommand(tailingLine, beep);
            int result = mUsbDeviceConnection.bulkTransfer(mEndPoint, command, command.length, 0);

            if (result < 0) {
                rejectPrintPromise(promise, "Failed to print cut command");
            } else {
                promise.resolve("Success to print cut command");
            }
        } else {
            rejectPrintPromise(promise, "Failed to connect to device");
        }
    }

    private byte[] createCutCommand(boolean tailingLine, boolean beep) {
        byte[] command = new byte[]{0x1D, 0x56, 0x42, 0x00};
        if (tailingLine) command[3] = 0x01;
        if (beep) command[3] = 0x03;
        return command;
    }

    private void rejectPrintPromise(Promise promise, String errorMessage) {
        Log.v(LOG_TAG, errorMessage);
        sendEvent(errorMessage);
        promise.reject(errorMessage);
    }

    private void sendEvent(String msg) {
        if (mContext != null) {
            ((ReactApplicationContext) mContext)
              .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
              .emit("com.usbprinter", msg);
        }
    }
}
