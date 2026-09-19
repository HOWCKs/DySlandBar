package com.howcks.dyslandbar;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

/**
 * Minimal, transparent accessibility integration for the first build.
 *
 * <p>The service does not inspect window content or notification text. Its current job is
 * limited to placing a small accessibility overlay when the user explicitly enables it.
 * Gesture and event-driven modules can be added without changing the CI packaging setup.</p>
 */
public final class DySlandAccessibilityService extends AccessibilityService {
    private static volatile DySlandAccessibilityService instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlayView;

    public static DySlandAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo != null) {
            serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_VIEW_SCROLLED;
            serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            serviceInfo.notificationTimeout = 100;
            // The service has no key-event or window-content flags in the first build.
            serviceInfo.flags = 0;
            setServiceInfo(serviceInfo);
        }

        boolean enabled = getSharedPreferences("dyslandbar_preferences", MODE_PRIVATE)
                .getBoolean("overlay_enabled", false);
        if (enabled) {
            showOverlay();
        }
    }

    /** Enables or removes the capsule without requesting a second overlay permission. */
    public void setOverlayEnabled(boolean enabled) {
        mainHandler.post(() -> {
            if (enabled) {
                showOverlay();
            } else {
                removeOverlay();
            }
        });
    }

    private void showOverlay() {
        if (overlayView != null || windowManager == null) {
            return;
        }

        TextView capsule = new TextView(this);
        capsule.setText("DySlandBar  •  pronto");
        capsule.setTextSize(12);
        capsule.setTextColor(Color.parseColor("#F4F2FF"));
        capsule.setGravity(Gravity.CENTER);
        capsule.setPadding(dp(16), 0, dp(16), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#E605060B"));
        background.setStroke(dp(1), Color.parseColor("#558CE8D2"));
        background.setCornerRadius(dp(24));
        capsule.setBackground(background);
        capsule.setContentDescription("Cápsula DySlandBar");

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                dp(42),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = dp(18);

        try {
            windowManager.addView(capsule, params);
            overlayView = capsule;
        } catch (WindowManager.BadTokenException | SecurityException ignored) {
            // The service may be revoked while the settings screen is closing.
            overlayView = null;
        }
    }

    private void removeOverlay() {
        if (overlayView == null || windowManager == null) {
            return;
        }
        try {
            windowManager.removeView(overlayView);
        } catch (IllegalArgumentException ignored) {
            // It was already removed by the system.
        } finally {
            overlayView = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Deliberately do not inspect event content. The first build only owns the visual shell.
    }

    @Override
    public void onInterrupt() {
        // No ongoing operation needs cancellation yet.
    }

    @Override
    public void onDestroy() {
        mainHandler.post(this::removeOverlay);
        instance = null;
        super.onDestroy();
    }
}
