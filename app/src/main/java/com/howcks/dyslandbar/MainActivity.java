package com.howcks.dyslandbar;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

/**
 * The small control panel for the first DySlandBar build.
 *
 * <p>This module deliberately uses platform widgets only. That keeps the first CI build
 * independent of third-party Maven dependencies while the product UI is being designed.</p>
 */
public final class MainActivity extends Activity {
    private static final String PREFS = "dyslandbar_preferences";
    private static final String PREF_OVERLAY_ENABLED = "overlay_enabled";

    private SharedPreferences preferences;
    private TextView serviceStatus;
    private Switch overlaySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#090B14"));
        getWindow().setNavigationBarColor(Color.parseColor("#090B14"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildContent());
        refreshServiceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serviceStatus != null) {
            refreshServiceStatus();
        }
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#090B14"));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(32));
        scrollView.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = textView("DySlandBar", 30, Color.parseColor("#F4F2FF"));
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        header.addView(heading, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView version = textView("MVP  •  0.1", 12, Color.parseColor("#090B14"));
        version.setGravity(Gravity.CENTER);
        version.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        version.setBackground(roundBackground("#8CE8D2", 24));
        header.addView(version, new LinearLayout.LayoutParams(dp(86), dp(32)));
        page.addView(header);

        TextView subtitle = textView(
                "Uma cápsula expressiva para transformar o recorte da câmera em uma área útil.",
                16,
                Color.parseColor("#A9ABC2"));
        subtitle.setLineSpacing(0f, 1.15f);
        page.addView(subtitle, margins(0, 8, 0, 0));

        CapsulePreviewView preview = new CapsulePreviewView(this);
        preview.setContentDescription("Prévia da cápsula DySlandBar");
        page.addView(preview, margins(0, 22, 0, 0, dp(268)));

        LinearLayout setupCard = card();
        setupCard.addView(textView("Ativação", 19, Color.parseColor("#F4F2FF")));
        setupCard.addView(textView(
                "O serviço de acessibilidade é usado somente para desenhar a cápsula e preparar gestos rápidos. O projeto inicial não lê o conteúdo das telas.",
                14,
                Color.parseColor("#A9ABC2")), margins(0, 8, 0, 0));

        overlaySwitch = new Switch(this);
        overlaySwitch.setText("Mostrar cápsula sobre outros apps");
        overlaySwitch.setTextSize(15);
        overlaySwitch.setTextColor(Color.parseColor("#F4F2FF"));
        overlaySwitch.setChecked(preferences.getBoolean(PREF_OVERLAY_ENABLED, false));
        overlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(PREF_OVERLAY_ENABLED, isChecked).apply();
            DySlandAccessibilityService service = DySlandAccessibilityService.getInstance();
            if (service != null) {
                service.setOverlayEnabled(isChecked);
            }
            refreshServiceStatus();
        });
        setupCard.addView(overlaySwitch, margins(0, 12, 0, 0));

        serviceStatus = textView("Verificando o serviço…", 13, Color.parseColor("#A9ABC2"));
        serviceStatus.setLineSpacing(0f, 1.1f);
        setupCard.addView(serviceStatus, margins(0, 6, 0, 0));

        Button accessibilityButton = actionButton("Abrir configurações de acessibilidade");
        accessibilityButton.setOnClickListener(view -> openAccessibilitySettings());
        setupCard.addView(accessibilityButton, margins(0, 16, 0, 0));
        page.addView(setupCard, margins(0, 24, 0, 0));

        LinearLayout roadmapCard = card();
        roadmapCard.addView(textView("Base pronta para evoluir", 19, Color.parseColor("#F4F2FF")));
        roadmapCard.addView(textView(
                "A compilação do APK já está automatizada. Controles de mídia, widgets, downloads, códigos de barras, gestos e temas Material You entram nas próximas etapas, com implementações reais e permissões explícitas.",
                14,
                Color.parseColor("#A9ABC2")), margins(0, 8, 0, 0));
        page.addView(roadmapCard, margins(0, 16, 0, 0));

        TextView footer = textView("Feito para Android 8.0 ou superior  •  sem dependências externas", 12,
                Color.parseColor("#6F728A"));
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, margins(0, 22, 0, 0));

        return scrollView;
    }

    private void refreshServiceStatus() {
        boolean enabled = isAccessibilityServiceEnabled();
        boolean overlayRequested = preferences.getBoolean(PREF_OVERLAY_ENABLED, false);
        if (enabled && overlayRequested) {
            serviceStatus.setText("Serviço ativo. A cápsula pode aparecer sobre outros apps.");
            serviceStatus.setTextColor(Color.parseColor("#8CE8D2"));
        } else if (enabled) {
            serviceStatus.setText("Serviço ativo. Ligue o interruptor para mostrar a cápsula.");
            serviceStatus.setTextColor(Color.parseColor("#A9ABC2"));
        } else {
            serviceStatus.setText("Ainda não ativo. Toque no botão abaixo e habilite “Cápsula DySlandBar”.");
            serviceStatus.setTextColor(Color.parseColor("#FFCF86"));
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (manager == null) {
            return false;
        }

        List<AccessibilityServiceInfo> services =
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        String serviceName = new ComponentName(this, DySlandAccessibilityService.class)
                .getClassName();
        for (AccessibilityServiceInfo service : services) {
            if (service.getResolveInfo() != null && service.getResolveInfo().serviceInfo != null) {
                String packageName = service.getResolveInfo().serviceInfo.packageName;
                String className = service.getResolveInfo().serviceInfo.name;
                if (getPackageName().equals(packageName) && serviceName.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
            // Every supported Android version exposes this settings screen. If a vendor
            // removes it, leaving the app open is safer than crashing the control panel.
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundBackground("#151827", 22));
        return card;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(Color.parseColor("#090B14"));
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setMinHeight(dp(48));
        button.setBackground(roundBackground("#8CE8D2", 16));
        return button;
    }

    private TextView textView(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        return margins(left, top, right, bottom, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private GradientDrawable roundBackground(String color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** A dependency-free visual preview of the capsule and its first interaction surfaces. */
    private final class CapsulePreviewView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        CapsulePreviewView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float density = getResources().getDisplayMetrics().density;
            float width = getWidth();
            float height = getHeight();

            paint.setShader(new LinearGradient(0, 0, width, height,
                    Color.parseColor("#242A48"), Color.parseColor("#111522"), Shader.TileMode.CLAMP));
            rect.set(0, 0, width, height);
            canvas.drawRoundRect(rect, 28 * density, 28 * density, paint);
            paint.setShader(null);

            // The camera capsule.
            paint.setColor(Color.parseColor("#05060B"));
            rect.set(18 * density, 18 * density, width - 18 * density, 70 * density);
            canvas.drawRoundRect(rect, 28 * density, 28 * density, paint);
            paint.setColor(Color.parseColor("#353B5A"));
            rect.set(width / 2 - 40 * density, 23 * density, width / 2 + 40 * density, 28 * density);
            canvas.drawRoundRect(rect, 4 * density, 4 * density, paint);

            paint.setColor(Color.parseColor("#8CE8D2"));
            canvas.drawCircle(42 * density, 44 * density, 7 * density, paint);
            paint.setColor(Color.parseColor("#171A29"));
            canvas.drawCircle(42 * density, 44 * density, 3 * density, paint);
            paint.setColor(Color.parseColor("#9CA7FF"));
            canvas.drawCircle(width - 42 * density, 44 * density, 7 * density, paint);

            drawText(canvas, "DySlandBar", 22 * density, 112 * density, 18, Color.parseColor("#F4F2FF"), true);
            drawText(canvas, "Tudo no seu ritmo", 22 * density, 137 * density, 13, Color.parseColor("#A9ABC2"), false);

            paint.setColor(Color.parseColor("#8CE8D2"));
            canvas.drawCircle(31 * density, 173 * density, 5 * density, paint);
            drawText(canvas, "Música pronta para tocar", 44 * density, 178 * density, 13,
                    Color.parseColor("#D7D8EA"), false);
            drawText(canvas, "◀     ▶     ▶", width - 110 * density, 178 * density, 12,
                    Color.parseColor("#8CE8D2"), true);

            paint.setColor(Color.parseColor("#303652"));
            rect.set(22 * density, height - 40 * density, width - 22 * density, height - 36 * density);
            canvas.drawRoundRect(rect, 3 * density, 3 * density, paint);
            paint.setColor(Color.parseColor("#8CE8D2"));
            rect.set(22 * density, height - 40 * density, width * 0.62f, height - 36 * density);
            canvas.drawRoundRect(rect, 3 * density, 3 * density, paint);
        }

        private void drawText(Canvas canvas, String value, float x, float baseline, int size,
                              int color, boolean bold) {
            paint.setShader(null);
            paint.setColor(color);
            paint.setTextSize(size * getResources().getDisplayMetrics().density);
            paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
            canvas.drawText(value, x, baseline, paint);
        }
    }
}
