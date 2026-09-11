package com.network.adswap_sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class AdSwap {
    private static String pubId = null;
    private static final String BASE_URL = "https://adswap.netlify.app/ad.html";

    private static FrameLayout overlayContainer;
    private static WebView interstitialWebView;
    private static android.app.Dialog interstitialDialog;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // =========================================================
    // CALLBACK PER INTERSTITIAL
    // =========================================================
    public interface InterstitialCallback {
        void onAdClosed();
    }
    private static InterstitialCallback currentCallback = null;

    public static class AdStyle {
        public String bgColor = null;
        public String titleColor = null;
        public String descColor = null;
        public String borderColor = null;
        public String borderRadius = "0px";

        public AdStyle setBackgroundColor(String hexCode) { this.bgColor = hexCode.replace("#", ""); return this; }
        public AdStyle setTitleColor(String hexCode) { this.titleColor = hexCode.replace("#", ""); return this; }
        public AdStyle setDescColor(String hexCode) { this.descColor = hexCode.replace("#", ""); return this; }
        public AdStyle setBorderColor(String hexCode) { this.borderColor = hexCode.replace("#", ""); return this; }
        public AdStyle setBorderRadius(String radius) { this.borderRadius = radius; return this; }
    }

    public static void initialize(String publisherId) {
        pubId = publisherId;
    }

    // =========================================================
    // INTERSTITIAL METODI (OVERLOADING)
    // =========================================================
    public static void showInterstitial(Activity activity, String category) {
        showInterstitial(activity, category, "android", null);
    }

    public static void showInterstitial(Activity activity, String category, InterstitialCallback callback) {
        showInterstitial(activity, category, "android", callback);
    }

    public static void showInterstitial(Activity activity, String category, String platform, InterstitialCallback callback) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        currentCallback = callback; // Salva la callback per lanciarla alla chiusura

        activity.runOnUiThread(() -> {
            destroyInterstitial();

            overlayContainer = new FrameLayout(activity);
            overlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // Inizialmente trasparente e invisibile per Chromium
            overlayContainer.setBackgroundColor(Color.TRANSPARENT);
            overlayContainer.setAlpha(0f);
            overlayContainer.setVisibility(View.VISIBLE);

            interstitialWebView = new WebView(activity);
            setupWebView(interstitialWebView, activity, true);

            String url = BASE_URL + "?pubId=" + pubId + "&format=interstitial&category=" + category + "&platform=" + platform;
            interstitialWebView.loadUrl(url);

            overlayContainer.addView(interstitialWebView);

            // Crea un Dialog a schermo intero senza titolo
            interstitialDialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            interstitialDialog.setContentView(overlayContainer);
            // Blocca la chiusura tappando fuori o col tasto back fisico: l'utente deve premere la X dell'annuncio
            interstitialDialog.setCancelable(false);

            if (!activity.isFinishing()) {
                interstitialDialog.show();
            }
        });
    }

    private static void destroyInterstitial() {
        try {
            if (interstitialDialog != null) {
                interstitialDialog.dismiss();
                interstitialDialog = null;
            }

            if (overlayContainer != null) {
                overlayContainer.animate().cancel();
                overlayContainer.setVisibility(View.GONE);

                ViewGroup parent = (ViewGroup) overlayContainer.getParent();
                if (parent != null) {
                    parent.removeView(overlayContainer);
                }
                overlayContainer.removeAllViews();
                overlayContainer = null;
            }

            if (interstitialWebView != null) {
                interstitialWebView.stopLoading();
                interstitialWebView.loadUrl("about:blank");
                interstitialWebView.clearHistory();
                interstitialWebView.destroy();
                interstitialWebView = null;
            }

            if (currentCallback != null) {
                currentCallback.onAdClosed();
                currentCallback = null;
            }

        } catch (Exception e) {
            android.util.Log.e("AdSwapSDK", "Errore chiusura interstitial", e);
        }
    }

    // =========================================================
    // BANNER METODI
    // =========================================================
    public static void showBanner(Activity activity, FrameLayout container, String category, AdStyle style) {
        showBanner(activity, container, category, "android", style);
    }

    public static void showBanner(Activity activity, FrameLayout container, String category, String platform, AdStyle style) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        activity.runOnUiThread(() -> {
            if (container.getChildCount() > 0) {
                for (int i = 0; i < container.getChildCount(); i++) {
                    android.view.View v = container.getChildAt(i);
                    if (v instanceof WebView) {
                        container.removeView(v);
                        ((WebView) v).stopLoading();
                        ((WebView) v).clearHistory();
                        ((WebView) v).removeAllViews();
                        ((WebView) v).destroy();
                    }
                }
            }
            container.removeAllViews();

            WebView bannerWebView = new WebView(activity);
            setupWebView(bannerWebView, activity, false);

            String url = BASE_URL + "?pubId=" + pubId + "&format=banner&category=" + category + "&platform=" + platform;

            if (style != null) {
                if (style.bgColor != null) url += "&bg=" + style.bgColor;
                if (style.titleColor != null) url += "&title=" + style.titleColor;
                if (style.descColor != null) url += "&desc=" + style.descColor;
                if (style.borderColor != null) url += "&border=" + style.borderColor;
                if (style.borderRadius != null) url += "&radius=" + style.borderRadius;
            }

            bannerWebView.loadUrl(url);
            container.addView(bannerWebView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        });
    }

    private static void setupWebView(WebView webView, Activity activity, boolean isInterstitial) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String clickedUrl = request.getUrl().toString();
                if (clickedUrl.startsWith("http") && !clickedUrl.contains("ad.html")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedUrl));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("AdSwapSDK", "Impossibile aprire il browser per l'URL: " + clickedUrl, e);
                    }
                    return true;
                }
                return false;
            }
        });

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void closeAd() {
                if (isInterstitial) MAIN.post(() -> destroyInterstitial());
            }

            @JavascriptInterface
            public void adLoaded() {
                if (isInterstitial && overlayContainer != null) {
                    MAIN.post(() -> {
                        overlayContainer.setBackgroundColor(Color.parseColor("#020617"));
                        overlayContainer.animate().alpha(1f).setDuration(300).start();
                    });
                }
            }

            @JavascriptInterface
            public void reportAd(String adId) {
                MAIN.post(() -> {
                    android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
                    layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                    layout.setPadding(64, 64, 64, 64);

                    android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                    bg.setColor(Color.parseColor("#0f172a"));
                    bg.setCornerRadius(44);
                    layout.setBackground(bg);

                    android.widget.TextView title = new android.widget.TextView(activity);
                    title.setText("Report this Ad?");
                    title.setTextColor(Color.WHITE);
                    title.setTextSize(18);
                    title.setGravity(android.view.Gravity.CENTER);
                    title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
                    layout.addView(title);

                    android.widget.TextView message = new android.widget.TextView(activity);
                    message.setText("Do you want to report this ad for inappropriate content?");
                    message.setTextColor(Color.parseColor("#94a3b8"));
                    message.setTextSize(14);
                    message.setGravity(android.view.Gravity.CENTER);
                    android.widget.LinearLayout.LayoutParams msgParams = new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    msgParams.setMargins(0, 24, 0, 48);
                    message.setLayoutParams(msgParams);
                    layout.addView(message);

                    android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(activity);
                    btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    btnLayout.setWeightSum(2);

                    android.widget.Button btnCancel = new android.widget.Button(activity);
                    btnCancel.setText("Cancel");
                    btnCancel.setTextSize(14);
                    btnCancel.setTextColor(Color.WHITE);
                    btnCancel.setAllCaps(false);
                    android.graphics.drawable.GradientDrawable cancelBg = new android.graphics.drawable.GradientDrawable();
                    cancelBg.setColor(Color.parseColor("#1e293b"));
                    cancelBg.setCornerRadius(24);
                    btnCancel.setBackground(cancelBg);
                    android.widget.LinearLayout.LayoutParams cancelParams = new android.widget.LinearLayout.LayoutParams(0, 110, 1);
                    cancelParams.setMargins(0, 0, 12, 0);
                    btnCancel.setLayoutParams(cancelParams);

                    android.widget.Button btnSend = new android.widget.Button(activity);
                    btnSend.setText("Report");
                    btnSend.setTextSize(14);
                    btnSend.setTextColor(Color.WHITE);
                    btnSend.setAllCaps(false);
                    android.graphics.drawable.GradientDrawable sendBg = new android.graphics.drawable.GradientDrawable();
                    sendBg.setColor(Color.parseColor("#ef4444"));
                    sendBg.setCornerRadius(24);
                    btnSend.setBackground(sendBg);
                    android.widget.LinearLayout.LayoutParams sendParams = new android.widget.LinearLayout.LayoutParams(0, 110, 1);
                    sendParams.setMargins(12, 0, 0, 0);
                    btnSend.setLayoutParams(sendParams);

                    btnLayout.addView(btnCancel);
                    btnLayout.addView(btnSend);
                    layout.addView(btnLayout);

                    AlertDialog dialog = new AlertDialog.Builder(activity).setView(layout).create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    }
                    dialog.show();

                    btnCancel.setOnClickListener(v -> dialog.dismiss());
                    btnSend.setOnClickListener(v -> {
                        webView.evaluateJavascript("window.AdSwapSDK.executeReport('" + adId + "');", null);
                        Toast.makeText(activity, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            }

            // GESTORE DEL POPUP INFO NATIVO
            @JavascriptInterface
            public void showInfoDialog(String messageContent) {
                MAIN.post(() -> {
                    android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
                    layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                    layout.setPadding(64, 64, 64, 64);

                    android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                    bg.setColor(Color.parseColor("#0f172a")); // Tema scuro
                    bg.setCornerRadius(44);
                    layout.setBackground(bg);

                    android.widget.TextView title = new android.widget.TextView(activity);
                    title.setText("About this Ad");
                    title.setTextColor(Color.WHITE);
                    title.setTextSize(18);
                    title.setGravity(android.view.Gravity.CENTER);
                    title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
                    layout.addView(title);

                    android.widget.TextView message = new android.widget.TextView(activity);
                    message.setText(messageContent); // Passiamo il testo generato da JS
                    message.setTextColor(Color.parseColor("#94a3b8"));
                    message.setTextSize(14);
                    message.setGravity(android.view.Gravity.LEFT);
                    android.widget.LinearLayout.LayoutParams msgParams = new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    msgParams.setMargins(0, 24, 0, 48);
                    message.setLayoutParams(msgParams);
                    layout.addView(message);

                    android.widget.Button btnOk = new android.widget.Button(activity);
                    btnOk.setText("Got it");
                    btnOk.setTextSize(14);
                    btnOk.setTextColor(Color.WHITE);
                    btnOk.setAllCaps(false);
                    android.graphics.drawable.GradientDrawable okBg = new android.graphics.drawable.GradientDrawable();
                    okBg.setColor(Color.parseColor("#6366f1")); // Tema indigo
                    okBg.setCornerRadius(24);
                    btnOk.setBackground(okBg);

                    layout.addView(btnOk);

                    AlertDialog dialog = new AlertDialog.Builder(activity).setView(layout).create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    }
                    dialog.show();

                    btnOk.setOnClickListener(v -> dialog.dismiss());
                });
            }
        }, "AdSwapAndroid");
    }
}