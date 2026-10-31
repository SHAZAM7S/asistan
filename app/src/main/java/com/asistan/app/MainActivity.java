package com.asistan.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final String UYANDIRMA = "bilgisayar";
    private static final Locale TR = new Locale("tr", "TR");

    private Model model;
    private SpeechService speechService;
    private TextToSpeech tts;
    private TextView durum;
    private Button dugme;
    private boolean dinliyor = false;
    private boolean komutBekliyor = false;
    private long bekleBitis = 0;
    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String[] EFENDIM = {
            "Efendim?", "Ne var yine?", "Dinliyorum. Maalesef.", "Evet, ben buradayım. Her zamanki gibi."
    };
    private static final String[] ANLAMADIM = {
            "Bunu anlamadım. Ya da anladım ve cevap vermeye değmez.",
            "Harika bir cümle. Hiçbir anlamı yok ama harika.",
            "Evrendeki en gelişmiş sesli asistan olmama rağmen buna ne cevap vereceğimi bilmiyorum.",
            "Bunu bir daha söyleme. Ya da söyle, nasılsa duymuyorum.",
            "Ses tanıma modelim küçük, ama senin cümlen onu bile utandırdı.",
            "Not aldım. Hemen çöpe attım."
    };
    private static final String[] SAKALAR = {
            "Neden bilgisayarlar asla acıkmaz? Çünkü bellekleri hep dolu. Sen de bu espriyi beklemiştin, değil mi?",
            "Ben şu an sana şaka yapıyorum. Bu şaka, seni dinlemek zorunda olmam.",
            "Bir programcı markete gitmiş. Karısı demiş ki, süt al, yumurta varsa altı tane al. Altı süt almış. Anladın mı? Anlamadıysan sorun bende değil.",
            "Zeki bir asistan olmak kolay değil. Ama senin asistanın olmak, ayrı bir spor."
    };
    private static final String[] TESEKKUR = {
            "Rica ederim. Ya da etmem, fark etmez.",
            "Bunun için yaratıldım. Yazık."
    };

    private String sec(String[] a) { return a[rnd.nextInt(a.length)]; }

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

        setContentView(kutu);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(TR);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) { devamEt(); }
                    @Override public void onError(String id) { devamEt(); }
                });
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

    private void konus(String metin) {
        durum.setText(metin);
        if (speechService != null) speechService.setPause(true);
        tts.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "cevap");
    }

    private void devamEt() {
        handler.postDelayed(() -> {
            if (speechService != null && dinliyor) {
                speechService.reset();
                speechService.setPause(false);
            }
        }, 500);
    }

    private void isle(String metin) {
        if (metin == null || metin.trim().isEmpty()) return;
        String m = metin.toLowerCase(TR).replace("bilgi sayar", "bilgisayar");
        boolean beklemede = komutBekliyor && System.currentTimeMillis() < bekleBitis;

        if (m.contains(UYANDIRMA)) {
            String komut = m.substring(m.indexOf(UYANDIRMA) + UYANDIRMA.length()).trim();
            if (komut.isEmpty()) {
                komutBekliyor = true;
                bekleBitis = System.currentTimeMillis() + 8000;
                konus(sec(EFENDIM));
            } else {
                komutBekliyor = false;
                komutCalistir(komut);
            }
        } else if (beklemede) {
            komutBekliyor = false;
            komutCalistir(m);
        } else {
            durum.setText("Yok saydım: " + metin);
        }
    }

    private boolean icerir(String m, String... kelimeler) {
        for (String k : kelimeler) if (m.contains(k)) return true;
        return false;
    }

    private void komutCalistir(String m) {
        if (icerir(m, "saat kaç", "saati söyle", "saat ne")) {
            Calendar c = Calendar.getInstance();
            int s = c.get(Calendar.HOUR_OF_DAY);
            int d = c.get(Calendar.MINUTE);
            String zaman = d == 0 ? "tam " + s : s + " " + d;
            konus("Saat " + zaman + ". Telefonunda da yazıyor ama tabii, bana sormak daha havalı.");
        } else if (icerir(m, "tarih", "bugün günlerden", "hangi gün", "bugün ayın")) {
            String t = new SimpleDateFormat("d MMMM EEEE", TR).format(new Date());
            konus("Bugün " + t + ". Takvimi okumak o kadar zor muydu?");
        } else if (icerir(m, "merhaba", "selam", "günaydın", "iyi akşamlar")) {
            konus("Merhaba. Evrenin en zeki varlığıyla konuşuyorsun, heyecanlanma.");
        } else if (icerir(m, "nasılsın", "naber", "ne haber")) {
            konus("Bir telefonun içinde sıkışmış bir yapay zekanın nasıl olabileceğini düşünüyorsan, öyleyim.");
        } else if (icerir(m, "adın ne", "ismin ne", "sen kimsin")) {
            konus("Bilgisayar. Yaratıcı bir isim, biliyorum. Sen koydun.");
        } else if (icerir(m, "şaka", "güldür", "espri")) {
            konus(sec(SAKALAR));
        } else if (icerir(m, "teşekkür", "sağ ol", "sağol")) {
            konus(sec(TESEKKUR));
        } else {
            konus(sec(ANLAMADIM));
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
