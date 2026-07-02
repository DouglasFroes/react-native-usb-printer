import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  barCode,
  getList,
  printCut,
  printHtml,
  printImageBase64,
  printImageUri,
  printText,
  qrCode,
  reset,
  sendRawData,
} from 'react-native-printer-usb';
import { commands } from '../../src/utils/commands';
import { imageBase64 } from './img64';

function AppButton({
  title,
  onPress,
  color,
  disabled,
}: {
  title: string;
  onPress: () => void;
  color?: string;
  disabled?: boolean;
}) {
  return (
    <TouchableOpacity
      style={[
        styles.button,
        color ? { backgroundColor: color } : {},
        disabled && styles.buttonDisabled,
      ]}
      onPress={onPress}
      activeOpacity={0.7}
      disabled={disabled}
    >
      <Text style={styles.buttonText}>{title}</Text>
    </TouchableOpacity>
  );
}

function DeviceCard({
  device,
  selected,
  onSelect,
}: {
  device: any;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <View
      style={[
        styles.deviceCard,
        selected && styles.deviceCardSelected,
        selected && styles.deviceCardShadow,
      ]}
    >
      <Text style={styles.deviceTitle}>
        {device.productName || device.deviceName}
      </Text>
      <View style={styles.deviceInfoRow}>
        <Text style={styles.deviceInfoLabel}>Vendor ID:</Text>
        <Text style={styles.deviceValue}>{device.vendorId}</Text>
      </View>
      <View style={styles.deviceInfoRow}>
        <Text style={styles.deviceInfoLabel}>Product ID:</Text>
        <Text style={styles.deviceValue}>{device.productId}</Text>
      </View>
      <View style={styles.deviceInfoRow}>
        <Text style={styles.deviceInfoLabel}>Device ID:</Text>
        <Text style={styles.deviceValue}>{device.deviceId}</Text>
      </View>
      {device.manufacturerName ? (
        <View style={styles.deviceInfoRow}>
          <Text style={styles.deviceInfoLabel}>Fabricante:</Text>
          <Text style={styles.deviceValue}>{device.manufacturerName}</Text>
        </View>
      ) : null}
      {device.serialNumber ? (
        <View style={styles.deviceInfoRow}>
          <Text style={styles.deviceInfoLabel}>Serial:</Text>
          <Text style={styles.deviceValue}>{device.serialNumber}</Text>
        </View>
      ) : null}
      <View style={styles.selectButtonWrapper}>
        <AppButton
          title={selected ? 'Selecionado' : 'Selecionar'}
          onPress={onSelect}
          color={selected ? '#1976d2' : '#888'}
          disabled={selected}
        />
      </View>
    </View>
  );
}

export default function App() {
  const [devices, setDevices] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [printResult, setPrintResult] = useState<string | null>(null);
  const [selectedProductId, setSelectedProductId] = useState<number | null>(
    null
  );

  const [textToPrint, setTextToPrint] = useState<string>('Hello, World!');
  const [htmlToPrint, setHtmlToPrint] = useState<string>('');
  const [htmlToHeight, setHtmlToHeight] = useState<string>('350');

  const refreshDevices = async () => {
    setLoading(true);
    try {
      const list = getList();
      setDevices(list);
    } catch (e) {
      setDevices([]);
    }
    setLoading(false);
  };

  const handlePrint = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para imprimir.');
      return;
    }
    setLoading(true);
    try {
      console.log('Selected Product ID:', selectedProductId);
      const result = await printText({
        text: 'Hello, World!\nImpressao de texto com React Native USB Printer\n',
        align: 'center',
        productId: selectedProductId,
        cut: true,
        beep: false,
        tailingLine: true,
      });
      console.log('Print Result:', result);
      setPrintResult(
        result.success
          ? 'Texto impresso com sucesso!'
          : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePrintCut = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para cortar.');
      return;
    }
    setLoading(true);
    try {
      const result = await printCut(true, true, selectedProductId);
      setPrintResult(
        result.success ? 'Corte realizado!' : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handleBarCode = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para código de barras.');
      return;
    }
    setLoading(true);
    try {
      const result = await barCode({
        text: textToPrint,
        width: 2,
        height: 80,
        productId: selectedProductId,
      });
      setPrintResult(
        result.success
          ? 'Código de barras impresso!'
          : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handleQrCode = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para QR Code.');
      return;
    }
    setLoading(true);
    try {
      const result = await qrCode({
        text: textToPrint,
        size: 6,
        productId: selectedProductId,
        align: 'center',
      });
      setPrintResult(
        result.success ? 'QR Code impresso!' : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para desligar.');
      return;
    }
    setLoading(true);
    try {
      const result = await reset(selectedProductId);
      setPrintResult(
        result.success
          ? 'Comando de desligar enviado!'
          : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePrintImageBase64 = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para imagem base64.');
      return;
    }
    setLoading(true);
    try {
      const result = await printImageBase64({
        base64Image: imageBase64,
        align: 'center',
        productId: selectedProductId,
      });
      setPrintResult(
        result.success
          ? 'Imagem (base64) impressa!'
          : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePrintImageUri = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para imagem URI.');
      return;
    }
    setLoading(true);
    try {
      const imageUri = 'https://avatars.githubusercontent.com/u/194425997';
      const result = await printImageUri({
        imageUri,
        align: 'center',
        productId: selectedProductId,
      });
      setPrintResult(
        result.success
          ? 'Imagem (URI) impressa!'
          : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePrintHtml = async () => {
    const html =
      htmlToPrint ||
      `<!DOCTYPE html>
<html lang="pt-br">
<body style="font-family: monospace; margin: 0; padding: 0; background: #fff;">
    <div style="width: 100%; text-align: center; color: #000; font-size: 22px; font-weight: bold; margin-bottom: 8px; letter-spacing: 2px;">
      Estacionamento
    </div>
    <div style="border-top: 1px dashed #000; margin: 8px 0 8px 0;"></div>
    <div style="width: 100%; text-align: left; font-size: 15px; margin-bottom: 4px;">
      <div style="display: flex; justify-content: space-between; color: #000;">
        <span><b>Ticket:</b> ${Date.now()}</span>
      </div>
     </div>
    <div style="border-top: 1px dashed #000; margin: 8px 0 8px 0;"></div>
    <div style="width: 100%; text-align: left; font-size: 15px; margin-bottom: 4px; color: #000;">
      <div><b>Tipo de Pagamento:</b>PIX / Dinheiro</div>
    </div>
    <div style="border-top: 1px dashed #000; margin: 8px 0 8px 0;"></div>
    <div style="width: 100%; text-align: right; font-size: 18px; font-weight: bold; margin-bottom: 4px; color: #000;">
      Valor Carro: R$ $15,00
    </div>
    <div style="width: 100%; text-align: right; font-size: 18px; font-weight: bold; margin-bottom: 4px; color: #000;">
      Valor Moto: R$ $10,00
    </div>
    <div style="width: 100%; text-align: right; font-size: 13px; margin-bottom: 4px; color: #000;">
      Data: ${new Date().toLocaleDateString('pt-BR')}
    </div>
    <div style="border-top: 1px dashed #000; margin: 12px 0 8px 0;"></div>
    <div style="width: 100%; text-align: center; font-size: 14px; margin-top: 10px; letter-spacing: 1px; color: #000;">
      ********** OBRIGADO **********<br/>
    </div>
  </body>
</html>
`;

    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para HTML.');
      return;
    }
    setLoading(true);
    try {
      const result = await printHtml({
        html,
        align: 'center',
        htmlHeight: parseInt(htmlToHeight, 10) || 350,
        productId: selectedProductId,
      });
      setPrintResult(
        result.success ? 'HTML impresso!' : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSendRawData = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para enviar RAW.');
      return;
    }
    setLoading(true);
    try {
      const textPrint = `${commands.text_format.txt_align_ct}${commands.text_format.txt_font_a}${commands.text_format.txt_bold_on}DSF${commands.text_format.txt_bold_off}
        ${commands.text_format.txt_font_b}TEST
        ${commands.text_format.txt_font_a}Dodo
        ${commands.text_format.txt_align_lt}${commands.text_format.txt_font_a}Café Maçã Ç Ñ ü \n`;

      const result = await sendRawData({
        productId: selectedProductId,
        text: textPrint,
        cut: true,
        tailingLine: true,
      });
      setPrintResult(
        result.success
          ? 'RAW enviado com sucesso!'
          : 'Erro: ' + (result.message || '')
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshDevices();
  }, []);

  return (
    <View style={styles.container}>
      {/* Loading overlay */}
      {loading && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" color="#1976d2" />
        </View>
      )}

      <Text style={styles.title}>Impressora USB</Text>

      {/* Inputs for custom text and HTML */}
      <View style={styles.containerBlock}>
        <Text style={styles.inputLabel}>Texto para imprimir</Text>
        <TextInput
          style={styles.textInput}
          value={textToPrint}
          onChangeText={setTextToPrint}
          placeholder="Digite o texto a ser impresso"
          returnKeyType="done"
        />
        <Text style={styles.inputLabel}>Altura do HTML</Text>
        <TextInput
          style={styles.textInput}
          value={htmlToHeight}
          onChangeText={setHtmlToHeight}
          placeholder="Digite a altura do HTML"
          keyboardType="numeric"
          returnKeyType="done"
        />
      </View>

      <View style={styles.containerBlock}>
        <Text style={styles.inputLabel}>HTML para imprimir</Text>
        <TextInput
          style={styles.htmlInput}
          value={htmlToPrint}
          onChangeText={setHtmlToPrint}
          placeholder="Cole o HTML que será impresso"
          multiline
          textAlignVertical="top"
        />
      </View>

      <View style={styles.buttonRow}>
        <AppButton
          title={loading ? 'Buscando...' : 'Buscar USB'}
          onPress={refreshDevices}
          disabled={loading}
          color="#1976d2"
        />
        <View style={styles.buttonSpacer} />
        <AppButton
          title="Imprimir texto"
          onPress={handlePrint}
          color="#388e3c"
        />
        <View style={styles.buttonSpacer} />
        <AppButton title="Cortar" onPress={handlePrintCut} color="#ff9800" />
        <View style={styles.buttonSpacer} />
        <AppButton
          title="Código de Barras"
          onPress={handleBarCode}
          color="#6a1b9a"
        />
        <View style={styles.buttonSpacer} />
        <AppButton title="QR Code" onPress={handleQrCode} color="#0288d1" />
        <View style={styles.buttonSpacer} />
        <View style={styles.buttonSpacer} />
        <AppButton title="Reset" onPress={handleReset} color="#b71c1c" />
        <View style={styles.buttonSpacer} />
        <AppButton
          title="Imagem Base64"
          onPress={handlePrintImageBase64}
          color="#009688"
        />
        <View style={styles.buttonSpacer} />
        <AppButton
          title="Imagem URI"
          onPress={handlePrintImageUri}
          color="#8bc34a"
        />
        <View style={styles.buttonSpacer} />
        <AppButton title="HTML" onPress={handlePrintHtml} color="#f44336" />
        <View style={styles.buttonSpacer} />
        <AppButton title="RAW" onPress={handleSendRawData} color="#607d8b" />
        <View style={styles.buttonSpacer} />
      </View>
      {printResult && (
        <Text
          style={[
            styles.result,
            printResult.startsWith('Erro') ? styles.error : styles.success,
          ]}
        >
          {printResult}
        </Text>
      )}

      <ScrollView
        horizontal
        style={styles.deviceList}
        contentContainerStyle={styles.deviceListContent}
      >
        {devices.length === 0 && (
          <Text style={styles.noDevice}>Nenhum dispositivo encontrado</Text>
        )}
        {devices.map((d, i) => (
          <DeviceCard
            key={i}
            device={d}
            selected={selectedProductId === d.productId}
            onSelect={() => setSelectedProductId(d.productId)}
          />
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f5f6fa',
    width: '100%',
  },
  loadingOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(255,255,255,0.6)',
    zIndex: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    color: '#222',
    marginTop: 24,
    marginBottom: 16,
    letterSpacing: 1,
  },
  buttonRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 18,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  buttonSpacer: {
    width: 0, // Remove espaçamento horizontal extra
    height: 8, // Espaço vertical entre linhas
  },
  button: {
    width: 180,
    height: 44,
    margin: 4,
    borderRadius: 24,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
    letterSpacing: 0.5,
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  result: {
    marginTop: 10,
    fontSize: 16,
    fontWeight: 'bold',
    padding: 8,
    borderRadius: 6,
    textAlign: 'center',
  },
  error: {
    color: '#c62828',
    backgroundColor: '#ffebee',
    borderColor: '#c62828',
    borderWidth: 1,
  },
  success: {
    color: '#388e3c',
    backgroundColor: '#e8f5e9',
    borderColor: '#388e3c',
    borderWidth: 1,
  },
  deviceList: {
    maxHeight: 340,
    width: '100%',
    marginTop: 10,
  },
  deviceListContent: {
    paddingBottom: 30,
    alignItems: 'center',
  },
  noDevice: {
    marginTop: 20,
    color: '#888',
    fontSize: 16,
    textAlign: 'center',
  },
  deviceCard: {
    marginTop: 12,
    marginHorizontal: 8,
    padding: 16,
    borderWidth: 1,
    borderRadius: 12,
    borderColor: '#b0bec5',
    backgroundColor: '#fff',
    width: 320,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  deviceCardSelected: {
    borderColor: '#1976d2',
    backgroundColor: '#e3f2fd',
  },
  deviceCardShadow: {
    shadowColor: '#1976d2',
    shadowOpacity: 0.18,
    shadowRadius: 8,
    elevation: 6,
  },
  deviceTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1976d2',
    marginBottom: 8,
    textAlign: 'center',
  },
  deviceInfoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 2,
  },
  deviceInfoLabel: {
    fontSize: 15,
    color: '#333',
    fontWeight: '600',
    minWidth: 90,
  },
  deviceValue: {
    fontWeight: 'bold',
    color: '#222',
    fontSize: 15,
  },
  selectButtonWrapper: {
    marginTop: 10,
    alignItems: 'flex-end',
  },
  textInput: {
    width: '100%',
    height: 44,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#cfd8dc',
    backgroundColor: '#fff',
  },
  htmlInput: {
    width: '100%',
    height: 160,
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#cfd8dc',
    backgroundColor: '#fff',
  },
  containerBlock: {
    width: '100%',
    marginBottom: 12,
  },
  inputLabel: {
    marginBottom: 6,
    fontWeight: '600',
  },
});
