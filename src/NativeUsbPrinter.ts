import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface PrintTextOptions {
  text: string;
  productId: number;
  /**
   * Multiplicador de tamanho do texto (largura e altura), de 1 (normal) a 8 (máximo).
   * 8x é o teto real do comando ESC/POS `GS !`; valores maiores são limitados a 8.
   */
  size?: number;
  align?: 'left' | 'center' | 'right';
  encoding?: string;
  /**
   * Página de código ESC/POS (comando ESC t) a ser selecionada na impressora antes
   * de enviar o texto, e usada para codificar os bytes corretamente. Use os valores
   * de `codepages` (ex: `codepages.CP850`). Quando informado, tem prioridade sobre `encoding`.
   * Consulte o manual da sua impressora para saber qual página ela suporta/espera.
   */
  codepage?: number;
  bold?: boolean;
  font?: 'A' | 'B' | 'C';
  cut?: boolean;
  beep?: boolean;
  tailingLine?: boolean;
  underline?: boolean;
}

export interface UsbDeviceInfo {
  deviceName: string;
  deviceId: number;
  vendorId: number;
  productId: number;
  manufacturerName?: string;
  productName?: string;
}

export interface PrinterResult {
  success: boolean;
  message?: string;
}

export interface BarCodeOptions {
  text: string;
  width?: number;
  height?: number;
  productId: number;
}

export interface QrCodeOptions {
  text: string;
  size?: number;
  align?: 'left' | 'center' | 'right';
  productId: number;
}

export interface PrintImageBase64Options {
  base64Image: string;
  align?: 'left' | 'center' | 'right';
  pageWidth?: number;
  productId: number;
}

export interface PrintImageUriOptions {
  imageUri: string;
  align?: 'left' | 'center' | 'right';
  pageWidth?: number;
  productId: number;
}

export interface PrintHtmlOptions {
  html: string;
  align?: 'left' | 'center' | 'right';
  pageWidth?: number;
  htmlHeight?: number;
  productId: number;
}

export interface Spec extends TurboModule {
  getList(): UsbDeviceInfo[];
  printText(options: PrintTextOptions): Promise<PrinterResult>;
  printCut(
    tailingLine: boolean,
    beep: boolean,
    productId: number
  ): Promise<PrinterResult>;
  barCode(options: BarCodeOptions): Promise<PrinterResult>;
  qrCode(options: QrCodeOptions): Promise<PrinterResult>;
  sendRawData(data: string, productId: number): Promise<PrinterResult>;
  printImageBase64(options: PrintImageBase64Options): Promise<PrinterResult>;
  printImageUri(options: PrintImageUriOptions): Promise<PrinterResult>;
  printHtml(options: PrintHtmlOptions): Promise<PrinterResult>;
  reset(productId: number): Promise<PrinterResult>;
  getErrorLogs(): Promise<string>;
  clearErrorLogs(): Promise<boolean>;
}

const UsbPrinter = TurboModuleRegistry.getEnforcing<Spec>('UsbPrinter');
export default UsbPrinter;
