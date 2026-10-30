package com.asistan.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private Model model;
    private SpeechService speechService;
    private TextToSpeech tts;
    private TextView durum;
    private Button dugme;
    private boolean dinliyor = false;

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
                tts.setLanguage(new Locale("tr", "TR"));
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
                durum.setText("Dinliyorum...");
            } catch (Exception e) {
                durum.setText("Hata: " + e.getMessage());
            }
        }
    }

    private void cevapla(String metin) {
        if (metin == null || metin.trim().isEmpty()) return;
        durum.setText("Duydum: " + metin);
        tts.speak("Şunu dedin: " + metin, TextToSpeech.QUEUE_FLUSH, null, "cevap");
    }

    @Override
    public void onResult(String hypothesis) {
        try {
            JSONObject j = new JSONObject(hypothesis);
            cevapla(j.optString("text"));
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
