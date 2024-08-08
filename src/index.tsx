import { textTo64Buffer } from './utils/to64Buffer';
import type {
  IPrinter,
  PrinterImageOptions,
  PrinterOptions,
} from './utils/types';

const UsbPrinter = require('./NativeUsbPrinter').default;

export function init() {
  return UsbPrinter.init();
}

export function getDeviceList(): IPrinter[] {
  return UsbPrinter.getDeviceList();
}

export function connect(vendorId: number, productId: number): string {
  return UsbPrinter.connect(vendorId, productId);
}

export function close(): void {
  return UsbPrinter.close();
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

export async function printImageURL(
  imageUrl: string,
  opts: PrinterImageOptions = {}
): Promise<string> {
  const result = await UsbPrinter.printImageURL(
    imageUrl,
    opts.imageWidth ?? 0,
    opts.imageHeight ?? 0
  );

  if (opts.cut) {
    await UsbPrinter.printCut(!!opts.tailingLine, !!opts.beep);
  }

  return result;
}

export async function printImageBase64(
  base64: string,
  opts: PrinterImageOptions = {}
): Promise<string> {
  const result = await UsbPrinter.printImageBase64(
    base64,
    opts.imageWidth ?? 0,
    opts.imageHeight ?? 0
  );

  if (opts.cut) {
    await UsbPrinter.printCut(!!opts.tailingLine, !!opts.beep);
  }

  return result;
}

export async function printCut(line: boolean, beep: boolean): Promise<string> {
  return UsbPrinter.printCut(line, beep);
}
