import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;
  init(): void;
  // getDeviceList(): string;
  getDeviceList(): { [key: string]: string }[];
  connect(vendorId: number, productId: number): string;
  RawData(data: string): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('UsbPrinter');
