package com.network.adswap_sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class AdSwap {
    private static String pubId = null;

    private static final String BASE_URL = "https://adswap.netlify.app/ad.html";

    // 1. Inizializzazione
    public static void initialize(String publisherId) {
        pubId = publisherId;
    }

    // 2. Mostra Interstitial a Schermo Intero
    public static void showInterstitial(Activity activity, String category) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        activity.runOnUiThread(() -> {
            Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WebView webView = new WebView(activity);
            setupWebView(webView, activity, dialog);

            String url = BASE_URL + "?pubId=" + pubId + "&format=interstitial&category=" + category;
            webView.loadUrl(url);

            dialog.setContentView(webView);
            dialog.show();
        });
    }

    // 3. Mostra Banner Nativo in un contenitore
    // Mostra Banner Nativo in un contenitore riempiendo interamente lo spazio assegnato
    public static void showBanner(Activity activity, FrameLayout container, String category) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        activity.runOnUiThread(() -> {
            WebView webView = new WebView(activity);
            setupWebView(webView, activity, null);

            String url = BASE_URL + "?pubId=" + pubId + "&format=banner&category=" + category;
            webView.loadUrl(url);

            // FIX CRITICO: Cambiato da WRAP_CONTENT a MATCH_PARENT per evitare il collasso a 0px.
            // Il controllo delle dimensioni effettive spetta al FrameLayout nell'XML del publisher.
            container.removeAllViews(); // Pulisce eventuali testi di placeholder ("Banner goes here...")
            container.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        });
    }

    // 4. Motore Interno (Non visibile agli sviluppatori)
    private static void setupWebView(WebView webView, Activity activity, Dialog dialog) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Se l'utente clicca l'annuncio, apri il browser di sistema di Android
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                activity.startActivity(intent);
                return true;
            }
        });

        // Ascolta il comando Javascript per chiudere il popup
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void closeAd() {
                activity.runOnUiThread(() -> {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                });
            }
        }, "AdSwapAndroid");
    }
}
