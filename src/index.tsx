import { textTo64Buffer, type PrinterOptions } from './utils/to64Buffer';
import type { IPrinter } from './utils/types';

const UsbPrinter = require('./NativeUsbPrinter').default;

export function multiply(a: number, b: number): number {
  return UsbPrinter.multiply(a, b);
}

export function init() {
  return UsbPrinter.init();
}

export function getDeviceList(): IPrinter[] {
  return UsbPrinter.getDeviceList();
}

export function connect(vendorId: number, productId: number): string {
  return UsbPrinter.connect(vendorId, productId);
}

export function onText(
  text: string,
  opts: PrinterOptions = {}
): Promise<string> {
  return UsbPrinter.RawData(textTo64Buffer(text, opts));
}

export function onBill(
  text: string,
  opts: PrinterOptions = {}
): Promise<string> {
  return UsbPrinter.RawData(textTo64Buffer(text, opts));
}
