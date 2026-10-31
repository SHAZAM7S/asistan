package com.asistan.app;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final String UYANDIRMA = "bilgisayar";
    private static final Locale TR = new Locale("tr", "TR");
    private static final long SOHBET_SURESI = 60000;

    private Model model;
    private SpeechService speechService;
    private TextToSpeech tts;
    private TextView durum;
    private Button dugme;
    private Button sesDugme;
    private boolean dinliyor = false;
    private boolean sohbetModu = false;
    private long sonEtkilesim = 0;
    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Voice> sesler = new ArrayList<>();
    private int sesNo = 0;

    private static final String[] EFENDIM = {
            "Efendim?", "Buyur.", "Dinliyorum.", "Evet?"
    };
    private static final String[] ANLAMADIM = {
            "Anlamadım.",
            "Bu da neydi?",
            "Bir daha dene.",
            "Hmm, hayır.",
            "Buna cevabım yok."
    };
    private static final String[] SAKALAR = {
            "Bilgisayarlar neden acıkmaz? Çünkü belleği hep dolu.",
            "Programcıya demişler ki süt al, yumurta varsa altı tane al. Altı süt almış.",
            "İki bayt buluşmuş. Biri demiş ki, bit gibi görünüyorsun.",
            "Yazılımcının en sevdiği oda hangisi? Çalışma odası değil, bug odası."
    };
    private static final String[] TESEKKUR = {
            "Rica ederim.", "Ne demek.", "Önemli değil."
    };
    private static final String[] LAF = {
            "Telefonda da yazıyor ama neyse.",
            "Zor soruydu, ter döktüm.",
            "Bu kadar mı?",
            "Beni bunun için mi uyandırdın?",
            "Yine mi sen?"
    };

    private String sec(String[] a) { return a[rnd.nextInt(a.length)]; }

    private String laf(String cevap) {
        if (rnd.nextInt(10) < 3) return cevap + " " + sec(LAF);
        return cevap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout kutu = new LinearLayout(this);
        kutu.setOrientation(LinearLayout.VERTICAL);
        kutu.setGravity(Gravity.CENTER);
        kutu.setPadding(40, 40, 40, 40);

        durum = new TextView(this);
        durum.setTextSize(20);
        durum.setText("Model yükleniyor...");
        kutu.addView(durum);

        dugme = new Button(this);
        dugme.setText("Dinlemeyi Başlat");
        dugme.setEnabled(false);
        dugme.setOnClickListener(v -> degistir());
        kutu.addView(dugme);

        sesDugme = new Button(this);
        sesDugme.setText("Sesi Değiştir");
        sesDugme.setEnabled(false);
        sesDugme.setOnClickListener(v -> sesDegistir());
        kutu.addView(sesDugme);

        setContentView(kutu);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(TR);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) { devamEt(); }
                    @Override public void onError(String id) { devamEt(); }
                });
                sesleriHazirla();
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            modeliYukle();
        }
    }

    private void sesleriHazirla() {
        try {
            Set<Voice> hepsi = tts.getVoices();
            if (hepsi == null) return;
            for (Voice v : hepsi) {
                if ("tr".equals(v.getLocale().getLanguage())) sesler.add(v);
            }
            Collections.sort(sesler, (a, b) -> a.getName().compareTo(b.getName()));
            if (sesler.isEmpty()) return;
            String kayit = getSharedPreferences("asistan", MODE_PRIVATE).getString("ses", null);
            for (int i = 0; i < sesler.size(); i++) {
                if (sesler.get(i).getName().equals(kayit)) sesNo = i;
            }
            tts.setVoice(sesler.get(sesNo));
            sesDugme.setEnabled(true);
            sesDugme.setText("Sesi Değiştir (" + (sesNo + 1) + "/" + sesler.size() + ")");
        } catch (Exception ignored) {}
    }

    private void sesDegistir() {
        if (sesler.isEmpty()) return;
        sesNo = (sesNo + 1) % sesler.size();
        tts.setVoice(sesler.get(sesNo));
        getSharedPreferences("asistan", MODE_PRIVATE).edit()
                .putString("ses", sesler.get(sesNo).getName()).apply();
        sesDugme.setText("Sesi Değiştir (" + (sesNo + 1) + "/" + sesler.size() + ")");
        tts.speak("Merhaba, ben bu sesim.", TextToSpeech.QUEUE_FLUSH, null, "deneme");
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        super.onRequestPermissionsResult(kod, izinler, sonuc);
        if (sonuc.length > 0 && sonuc[0] == PackageManager.PERMISSION_GRANTED) {
            modeliYukle();
        } else {
            durum.setText("Mikrofon izni gerekli.");
        }
    }

    private void modeliYukle() {
        StorageService.unpack(this, "model-tr", "model",
                (m) -> {
                    model = m;
                    durum.setText("Hazır. Düğmeye bas.");
                    dugme.setEnabled(true);
                },
                (e) -> durum.setText("Model hatası: " + e.getMessage()));
    }

    private void degistir() {
        if (dinliyor) {
            if (speechService != null) {
                speechService.stop();
                speechService = null;
            }
            dinliyor = false;
            sohbetModu = false;
            dugme.setText("Dinlemeyi Başlat");
            durum.setText("Kapalı.");
        } else {
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
                dinliyor = true;
                dugme.setText("Dinlemeyi Durdur");
                durum.setText("Dinliyorum... ('" + UYANDIRMA + "' de)");
            } catch (Exception e) {
                durum.setText("Hata: " + e.getMessage());
            }
        }
    }

    private void konus(String duyulan, String metin) {
        durum.setText("Duydum: " + duyulan + "\n\n" + metin);
        if (speechService != null) speechService.setPause(true);
        tts.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "cevap");
    }

    private void devamEt() {
        sonEtkilesim = System.currentTimeMillis();
        handler.postDelayed(() -> {
            if (speechService != null && dinliyor) {
                speechService.reset();
                speechService.setPause(false);
            }
        }, 500);
        handler.postDelayed(() -> {
            if (sohbetModu && System.currentTimeMillis() - sonEtkilesim >= SOHBET_SURESI) {
                sohbetModu = false;
                durum.setText("Sohbet bitti. ('" + UYANDIRMA + "' de)");
            }
        }, SOHBET_SURESI + 500);
    }

    private void isle(String metin) {
        if (metin == null || metin.trim().isEmpty()) return;
        String m = metin.toLowerCase(TR).replace("bilgi sayar", "bilgisayar");

        if (sohbetModu && System.currentTimeMillis() - sonEtkilesim >= SOHBET_SURESI) {
            sohbetModu = false;
        }

        if (m.contains(UYANDIRMA)) {
            String komut = m.substring(m.indexOf(UYANDIRMA) + UYANDIRMA.length()).trim();
            sohbetModu = true;
            if (komut.isEmpty()) {
                konus(metin, sec(EFENDIM));
            } else {
                komutCalistir(metin, komut);
            }
        } else if (sohbetModu) {
            komutCalistir(metin, m);
        } else {
            durum.setText("Yok saydım: " + metin);
        }
    }

    private boolean icerir(String m, String... kelimeler) {
        for (String k : kelimeler) if (m.contains(k)) return true;
        return false;
    }

    private String sarjBilgisi() {
        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return "Pil bilgisini alamadım.";
        int seviye = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int olcek = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int fis = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        if (seviye < 0 || olcek <= 0) return "Pil bilgisini alamadım.";
        int yuzde = seviye * 100 / olcek;
        return "Şarjın yüzde " + yuzde + (fis != 0 ? ", şarj oluyor." : ".");
    }

    private void komutCalistir(String duyulan, String m) {
        if (icerir(m, "kapan", "kendini kapat", "dinlemeyi bırak")) {
            konus(duyulan, "Kapanıyorum.");
            handler.postDelayed(() -> { if (dinliyor) degistir(); }, 2500);
        } else if (icerir(m, "görüşürüz", "sohbeti bitir", "sohbeti kapat", "sus", "yeter")) {
            sohbetModu = false;
            konus(duyulan, "Tamam.");
        } else if (icerir(m, "şarj", "şarz", "sarj", "sarım", "şarım", "pil", "batarya")) {
            konus(duyulan, laf(sarjBilgisi()));
        } else if (icerir(m, "saat")) {
            Calendar c = Calendar.getInstance();
            int s = c.get(Calendar.HOUR_OF_DAY);
            int d = c.get(Calendar.MINUTE);
            String zaman = d == 0 ? "tam " + s : s + " ve " + d + " dakika";
            konus(duyulan, laf("Saat " + zaman + "."));
        } else if (icerir(m, "tarih", "günlerden", "hangi gün", "ayın kaçı")) {
            String t = new SimpleDateFormat("d MMMM EEEE", TR).format(new Date());
            konus(duyulan, laf("Bugün " + t + "."));
        } else if (icerir(m, "merhaba", "selam", "günaydın", "iyi akşamlar")) {
            konus(duyulan, laf("Merhaba."));
        } else if (icerir(m, "nasılsın", "naber", "ne haber")) {
            konus(duyulan, laf("İyiyim. Bir telefonun içinde olabildiğim kadar."));
        } else if (icerir(m, "adın ne", "ismin ne", "sen kimsin")) {
            konus(duyulan, "Bilgisayar. İsmi sen koydun.");
        } else if (icerir(m, "şaka", "güldür", "espri")) {
            konus(duyulan, sec(SAKALAR));
        } else if (icerir(m, "teşekkür", "sağ ol", "sağol")) {
            konus(duyulan, sec(TESEKKUR));
        } else {
            if (sohbetModu && m.trim().split("\\s+").length < 2) {
                durum.setText("Yok saydım: " + duyulan);
                return;
            }
            konus(duyulan, sec(ANLAMADIM));
        }
    }

    @Override
    public void onResult(String hypothesis) {
        try {
            JSONObject j = new JSONObject(hypothesis);
            isle(j.optString("text"));
        } catch (Exception ignored) {}
    }

    @Override public void onFinalResult(String hypothesis) {}
    @Override public void onPartialResult(String hypothesis) {}
    @Override public void onError(Exception e) { durum.setText("Hata: " + e.getMessage()); }
    @Override public void onTimeout() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechService != null) speechService.stop();
        if (tts != null) tts.shutdown();
    }
}
