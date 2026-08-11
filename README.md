# react-native-printer-usb

React Native module to print on USB thermal printers (ESC/POS) for Android. Supports text, images (base64/URL), barcodes, QR codes, HTML, and raw commands. Handles accented characters, alignment, font, cut, beep, underline, and more.

## Installation

```sh
npm install react-native-printer-usb
```

### Android Setup
- No manual steps required for most React Native projects (autolinking).
- For custom setups, ensure USB permissions in `AndroidManifest.xml`:

```xml
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

## Usage

### List Devices
```js
import { getList } from 'react-native-printer-usb';
const devices = getList();
// [{ vendorId, productId, deviceId, manufacturerName, productName, serialNumber, deviceName }]
```

### Print Text
```js
import { printText } from 'react-native-printer-usb';
await printText({
  text: 'Olá, mundo! Çãõé',
  productId, // required
  align: 'center', // 'left' | 'center' | 'right'
  encoding: 'CP850', // or 'utf8', 'ISO-8859-1', etc.
  bold: true,
  underline: true,
  font: 'A', // 'A' | 'B' | 'C'
  size: 2, // 1 (normal) to 8 (8x) — hardware ceiling of the ESC/POS `GS !` command
  cut: true, // cut paper after print
  beep: true, // beep after print
  tailingLine: true, // add blank lines at end
});
```

#### Configuring the codepage for your specific printer

Different thermal printers default to different ESC/POS character tables, so accented
characters (á, ç, ã...) can print as garbage if the wrong table is assumed. Instead of
guessing, pass `codepage` with the exact table your printer uses — it takes priority
over `encoding`, sends the `ESC t` select-codepage command to the printer, and encodes
the text with the matching charset:

```js
import { printText, codepages } from 'react-native-printer-usb';

await printText({
  text: 'Olá, mundo! Çãõé',
  productId,
  codepage: codepages.CP850, // or codepages.PC860, codepages.WPC1252, ...
});
```

| Constant          | Value | Table                      |
| ------------------ | ----- | --------------------------- |
| `codepages.PC437`  | 0     | USA: Standard Europe        |
| `codepages.CP850`  | 2     | Multilingual                |
| `codepages.PC860`  | 3     | Portuguese                  |
| `codepages.PC863`  | 4     | Canadian-French              |
| `codepages.PC865`  | 5     | Nordic                       |
| `codepages.WPC1252`| 16    | Windows-1252                 |
| `codepages.PC866`  | 17    | Cyrillic #2                  |
| `codepages.PC852`  | 18    | Latin 2                      |
| `codepages.PC858`  | 19    | Multilingual + Euro          |

Check your printer's manual for the codepage it supports — for Brazilian ESC/POS
printers (Gertec, Bematech, Elgin, Daruma, etc.) this is usually `CP850` or `PC860`.

### Print Image (Base64 or URL)
```js
import { printImageBase64, printImageUri } from 'react-native-printer-usb';
await printImageBase64({ base64Image, productId, align: 'center' });
await printImageUri({ imageUri: 'https://...', productId, align: 'center' });
```

### Print Barcode / QR Code
```js
import { barCode, qrCode } from 'react-native-printer-usb';
await barCode({ text: '123456789012', productId, width: 2, height: 80 });
await qrCode({ text: 'https://reactnative.dev', productId, size: 6, align: 'center' });
```

### Print HTML
```js
import { printHtml } from 'react-native-printer-usb';
await printHtml({
  html: '<h1>Impressão HTML</h1>',
  productId,
  align: 'center',
  htmlHeight: 760, // px (optional)
});
```

### Send Raw Data
```js
import { sendRawData } from 'react-native-printer-usb';
await sendRawData({
  productId,
  text: '\x1B\x40Hello\n', // ESC/POS commands
  cut: true,
  tailingLine: true,
  encoding: 'utf8',
});
```

### Cut, Reset, Beep
```js
import { printCut, reset } from 'react-native-printer-usb';
await printCut(true, true, productId); // cut, beep, productId
await reset(productId);
```

### Debugging & Error Logs

The library includes a built-in persistent error logger on Android to help diagnose connection, formatting, or hardware issues in production. Logs are saved securely inside the app's private internal storage (`usb_printer_errors.log`) with precise timestamps and full exception stack traces.

```js
import { getErrorLogs, clearErrorLogs } from 'react-native-printer-usb';

// Retrieve all accumulated error logs as a string
const logs = await getErrorLogs();
console.log(logs);

// Clear the accumulated logs file
const success = await clearErrorLogs();
```

## Options Reference

| Option         | Type      | Description                                  |
| -------------- | --------- | -------------------------------------------- |
| text           | string    | Text to print                                |
| productId      | number    | USB Product ID (required)                    |
| align          | string    | 'left', 'center', 'right'                    |
| encoding       | string    | 'utf8', 'CP850', 'ISO-8859-1', etc.          |
| codepage       | number    | ESC t codepage number (see `codepages`); overrides `encoding` |
| bold           | boolean   | Bold text                                    |
| underline      | boolean   | Underline text                               |
| font           | string    | 'A', 'B', 'C'                                |
| size           | number    | 1 (normal) to 8 (8x); values above 8 are clamped to 8 |
| cut            | boolean   | Cut paper after print                        |
| beep           | boolean   | Beep after print                             |
| tailingLine    | boolean   | Add blank lines at end                       |
| base64Image    | string    | PNG/JPG base64 image (for printImageBase64)  |
| imageUri       | string    | Image URL (for printImageUri)                |
| html           | string    | HTML string (for printHtml)                  |
| htmlHeight     | number    | Height in px for HTML print (optional)       |

## Example App

See `example/App.tsx` for a full-featured demo with device selection, all print types, and UI/UX best practices.

## Troubleshooting
- Certifique-se de que o dispositivo USB está conectado e com permissão.
- Use encoding compatível com sua impressora (CP850, ISO-8859-1, etc).
- Para imagens, use PNG/JPG pequenos e alinhamento adequado.
- Para impressoras que não cortam/beep, ignore as opções `cut`/`beep`.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
