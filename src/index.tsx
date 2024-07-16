const UsbPrinter = require('./NativeUsbPrinter').default;

export function multiply(a: number, b: number): number {
  return UsbPrinter.multiply(a, b);
}
