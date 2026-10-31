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
            "Z
