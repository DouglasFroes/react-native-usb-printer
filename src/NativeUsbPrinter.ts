import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  init(): void;
  getDeviceList(): { [key: string]: string }[];
  connect(vendorId: number, productId: number): string;
  close(): void;
  RawData(data: string): Promise<string>;
  printImageURL(
    imageUrl: string,
    imageWidth: number,
    imageHeight: number
  ): Promise<string>;
  printImageBase64(
    base64: string,
    imageWidth: number,
    imageHeight: number
  ): Promise<string>;
  printCut(tailingLine: boolean, beep: boolean): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('UsbPrinter');
