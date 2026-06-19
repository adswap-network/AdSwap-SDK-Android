package com.network.adswap_sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class AdSwap {
    private static String pubId = null;

    // INSERISCI QUI IL TUO SITO NETLIFY (Es. https://iltuosito.netlify.app/ad.html)
    private static final String BASE_URL = "https://adswap.netlify.app/ad.html";

    // Classe per gestire la grafica passata dallo sviluppatore
    public static class AdStyle {
        public String bgColor = "1e293b";
        public String titleColor = "ffffff";
        public String descColor = "94a3b8";

        public AdStyle setBackgroundColor(String hexCode) { this.bgColor = hexCode.replace("#", ""); return this; }
        public AdStyle setTitleColor(String hexCode) { this.titleColor = hexCode.replace("#", ""); return this; }
        public AdStyle setDescColor(String hexCode) { this.descColor = hexCode.replace("#", ""); return this; }
    }

    public static void initialize(String publisherId) {
        pubId = publisherId;
    }

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

    // Mostra Banner Nativo con supporto agli Stili
    public static void showBanner(Activity activity, FrameLayout container, String category, AdStyle style) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        activity.runOnUiThread(() -> {
            WebView webView = new WebView(activity);
            setupWebView(webView, activity, null);

            // Passa i parametri di stile all'URL
            String url = BASE_URL + "?pubId=" + pubId + "&format=banner&category=" + category;
            if (style != null) {
                url += "&bg=" + style.bgColor + "&title=" + style.titleColor + "&desc=" + style.descColor;
            }
            webView.loadUrl(url);

            container.removeAllViews();
            container.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)); // Adatta l'altezza perfettamente al contenitore!
        });
    }

    // Costruttore interno della WebView
    private static void setupWebView(WebView webView, Activity activity, Dialog dialog) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Aggiunto WebChromeClient per supportare operazioni JS avanzate senza crash
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String clickedUrl = request.getUrl().toString();
                // Se è un link esterno (non la pagina di Netlify), aprilo nel browser vero
                if (clickedUrl.startsWith("http") && !clickedUrl.contains("netlify.app/ad.html")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedUrl));
                    activity.startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void closeAd() {
                activity.runOnUiThread(() -> {
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                });
            }
        }, "AdSwapAndroid");
    }
}