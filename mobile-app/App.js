import { useEffect, useState } from 'react';
import {
  ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StatusBar as RNStatusBar,
  StyleSheet, Text, TextInput, View, useColorScheme,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Speech from 'expo-speech';
import * as Clipboard from 'expo-clipboard';
import { DEFAULT_SETTINGS, health, translate, whoami } from './src/api';

const SOURCES = ['auto', 'English', 'French', 'Spanish'];

export default function App() {
  const dark = useColorScheme() === 'dark';
  const c = dark ? colors.dark : colors.light;

  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [showSettings, setShowSettings] = useState(false);
  const [text, setText] = useState('');
  const [source, setSource] = useState('auto');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [history, setHistory] = useState([]);

  useEffect(() => {
    (async () => {
      const s = await AsyncStorage.getItem('settings');
      if (s) setSettings({ ...DEFAULT_SETTINGS, ...JSON.parse(s) });
      else setShowSettings(true);
      const h = await AsyncStorage.getItem('history');
      if (h) setHistory(JSON.parse(h));
    })();
  }, []);

  async function onTranslate() {
    if (!text.trim()) { setMessage('Type something to translate.'); return; }
    setLoading(true); setMessage('');
    try {
      const r = await translate(settings, text.trim(), source);
      setResult(r);
      const next = [{ o: r.original, t: r.translation, l: r.transliteration }, ...history.filter((x) => x.o !== r.original)].slice(0, 15);
      setHistory(next);
      AsyncStorage.setItem('history', JSON.stringify(next));
    } catch (e) {
      setMessage(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function onSpeak() {
    if (!result) return;
    Speech.stop();
    const voices = await Speech.getAvailableVoicesAsync();
    const arabic = voices.find((v) => v.language && v.language.toLowerCase().startsWith('ar'));
    if (arabic) Speech.speak(result.translation, { language: arabic.language, rate: 0.95 });
    else Speech.speak(result.transliteration || result.translation, { language: 'fr-FR', rate: 0.95 });
  }

  async function onSaveSettings() {
    await AsyncStorage.setItem('settings', JSON.stringify(settings));
    try {
      const h = await health(settings);
      const me = await whoami(settings);
      setMessage(`Connected ✓ ${h.provider}/${h.model} as ${me.user}`);
      setShowSettings(false);
    } catch (e) {
      setMessage(e.message);
    }
  }

  return (
    <KeyboardAvoidingView style={[styles.flex, { backgroundColor: c.bg }]} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <StatusBar style="light" />
      <View style={styles.header}>
        <Text style={styles.title}>Darija Translator</Text>
        <Pressable onPress={() => setShowSettings(!showSettings)} hitSlop={12} accessibilityLabel="Settings">
          <Text style={styles.gear}>⚙</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
        {showSettings && (
          <View style={[styles.card, { backgroundColor: c.card, borderColor: c.border }]}>
            <Text style={[styles.label, { color: c.muted }]}>Server URL (use your PC's IP, not localhost)</Text>
            <TextInput style={[styles.input, { color: c.text, borderColor: c.border }]} autoCapitalize="none" autoCorrect={false}
              value={settings.serverUrl} onChangeText={(v) => setSettings({ ...settings, serverUrl: v })} />
            <Text style={[styles.label, { color: c.muted }]}>Username</Text>
            <TextInput style={[styles.input, { color: c.text, borderColor: c.border }]} autoCapitalize="none"
              value={settings.username} onChangeText={(v) => setSettings({ ...settings, username: v })} />
            <Text style={[styles.label, { color: c.muted }]}>Password</Text>
            <TextInput style={[styles.input, { color: c.text, borderColor: c.border }]} secureTextEntry
              value={settings.password} onChangeText={(v) => setSettings({ ...settings, password: v })} />
            <Pressable style={styles.primary} onPress={onSaveSettings}><Text style={styles.primaryText}>Save & test connection</Text></Pressable>
          </View>
        )}

        <View style={styles.chips}>
          {SOURCES.map((s) => (
            <Pressable key={s} onPress={() => setSource(s)}
              style={[styles.chip, { borderColor: c.border }, source === s && styles.chipActive]}>
              <Text style={[{ color: c.text }, source === s && { color: '#fff' }]}>{s === 'auto' ? 'Detect' : s}</Text>
            </Pressable>
          ))}
        </View>

        <TextInput style={[styles.input, styles.textArea, { color: c.text, borderColor: c.border, backgroundColor: c.card }]}
          multiline placeholder="Type English, French… text" placeholderTextColor={c.muted} value={text} onChangeText={setText} />

        <Pressable style={[styles.primary, loading && { opacity: 0.6 }]} onPress={onTranslate} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryText}>Translate to Darija</Text>}
        </Pressable>

        {!!message && <Text style={styles.message}>{message}</Text>}

        {result && (
          <View style={[styles.card, { backgroundColor: c.card, borderColor: c.border }]}>
            <Text style={[styles.arabic, { color: c.text }]} selectable>{result.translation}</Text>
            {!!result.transliteration && <Text style={[styles.latin, { color: c.muted }]} selectable>{result.transliteration}</Text>}
            <View style={styles.row}>
              <Pressable style={[styles.secondary, { borderColor: c.border }]} onPress={onSpeak}><Text style={{ color: c.text }}>🔊 Read aloud</Text></Pressable>
              <Pressable style={[styles.secondary, { borderColor: c.border }]} onPress={() => Clipboard.setStringAsync(result.translation)}><Text style={{ color: c.text }}>📋 Copy</Text></Pressable>
            </View>
            <Text style={[styles.meta, { color: c.muted }]}>{result.sourceLanguage} → Darija · {result.provider}/{result.model} · {result.durationMs} ms</Text>
          </View>
        )}

        {history.length > 0 && <Text style={[styles.label, { color: c.muted, marginTop: 8 }]}>History</Text>}
        {history.map((h) => (
          <Pressable key={h.o} onPress={() => { setText(h.o); setResult({ original: h.o, translation: h.t, transliteration: h.l, sourceLanguage: 'history', provider: '-', model: '-', durationMs: 0 }); }}
            style={[styles.historyItem, { borderColor: c.border }]}>
            <Text style={{ color: c.text }} numberOfLines={1}>{h.o}</Text>
            <Text style={[styles.historyAr, { color: c.muted }]} numberOfLines={1}>{h.t}</Text>
          </Pressable>
        ))}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const colors = {
  light: { bg: '#fafaf9', card: '#ffffff', text: '#1c1917', muted: '#78716c', border: '#e7e5e4' },
  dark: { bg: '#1c1917', card: '#292524', text: '#f5f5f4', muted: '#a8a29e', border: '#44403c' },
};

const styles = StyleSheet.create({
  flex: { flex: 1 },
  header: {
    backgroundColor: '#C1272D', paddingTop: (RNStatusBar.currentHeight || 44) + 8, paddingBottom: 14, paddingHorizontal: 16,
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
  },
  title: { color: '#fff', fontSize: 20, fontWeight: '700' },
  gear: { color: '#fff', fontSize: 22 },
  body: { padding: 16, gap: 12 },
  card: { borderWidth: 1, borderRadius: 12, padding: 14, gap: 6 },
  label: { fontSize: 12 },
  input: { borderWidth: 1, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, fontSize: 16 },
  textArea: { minHeight: 110, textAlignVertical: 'top' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { borderWidth: 1, borderRadius: 16, paddingHorizontal: 12, paddingVertical: 6 },
  chipActive: { backgroundColor: '#006233', borderColor: '#006233' },
  primary: { backgroundColor: '#006233', borderRadius: 10, paddingVertical: 14, alignItems: 'center', marginTop: 4 },
  primaryText: { color: '#fff', fontWeight: '700', fontSize: 16 },
  secondary: { borderWidth: 1, borderRadius: 10, paddingVertical: 8, paddingHorizontal: 12 },
  message: { color: '#C1272D' },
  arabic: { fontSize: 26, lineHeight: 42, textAlign: 'right', writingDirection: 'rtl' },
  latin: { fontStyle: 'italic', fontSize: 15 },
  row: { flexDirection: 'row', gap: 8, marginTop: 8 },
  meta: { fontSize: 11, marginTop: 6 },
  historyItem: { borderBottomWidth: 1, paddingVertical: 8 },
  historyAr: { textAlign: 'right', writingDirection: 'rtl' },
});
