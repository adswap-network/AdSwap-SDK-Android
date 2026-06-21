package com.network.adswap_sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class AdSwap {
    private static String pubId = null;

    // IL TUO URL NETLIFY
    private static final String BASE_URL = "https://adswap.netlify.app/ad.html";

    public static class AdStyle {
        public String bgColor = null;
        public String titleColor = null;
        public String descColor = null;

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
            // FIX INTERSTITIAL: Usare Theme_Translucent per garantire la trasparenza senza rompere il fullscreen
            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WebView webView = new WebView(activity);
            setupWebView(webView, activity, dialog);

            String url = BASE_URL + "?pubId=" + pubId + "&format=interstitial&category=" + category + "&platform=android";
            webView.loadUrl(url);

            dialog.setContentView(webView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            dialog.show();
        });
    }

    public static void showBanner(Activity activity, FrameLayout container, String category, AdStyle style) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        activity.runOnUiThread(() -> {
            WebView webView = new WebView(activity);
            setupWebView(webView, activity, null);

            String url = BASE_URL + "?pubId=" + pubId + "&format=banner&category=" + category + "&platform=android";
            if (style != null) {
                if (style.bgColor != null) url += "&bg=" + style.bgColor;
                if (style.titleColor != null) url += "&title=" + style.titleColor;
                if (style.descColor != null) url += "&desc=" + style.descColor;
            }

            webView.loadUrl(url);

            container.removeAllViews();
            container.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        });
    }

    private static void setupWebView(WebView webView, Activity activity, final Dialog dialog) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // ==========================================
        // FIX SEGNALAZIONE: Popup Nativo Android Reale
        // ==========================================
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(activity)
                        .setTitle("Report Ad")
                        .setMessage(message)
                        .setPositiveButton("Confirm", (d, which) -> result.confirm())
                        .setNegativeButton("Cancel", (d, which) -> result.cancel())
                        .setCancelable(false)
                        .show();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String clickedUrl = request.getUrl().toString();
                if (clickedUrl.startsWith("http") && !clickedUrl.contains("ad.html")) {
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
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                });
            }
        }, "AdSwapAndroid");
    }
}