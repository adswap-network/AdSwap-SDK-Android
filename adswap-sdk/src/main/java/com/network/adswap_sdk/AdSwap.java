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

    private static final String BASE_URL = "https://adswap.netlify.app/ad.html";

    public static class AdStyle {
        public String bgColor = "1e293b";
        public String titleColor = "ffffff";
        public String descColor = "94a3b8";
        public int forcedWidthDp = -1;
        public int forcedHeightDp = -1;

        public AdStyle setBackgroundColor(String hexCode) { this.bgColor = hexCode.replace("#", ""); return this; }
        public AdStyle setTitleColor(String hexCode) { this.titleColor = hexCode.replace("#", ""); return this; }
        public AdStyle setDescColor(String hexCode) { this.descColor = hexCode.replace("#", ""); return this; }
        public AdStyle setWidth(int dp) { this.forcedWidthDp = dp; return this; }
        public AdStyle setHeight(int dp) { this.forcedHeightDp = dp; return this; }
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
            setupWebView(webView, activity, dialog, null);

            String url = BASE_URL + "?pubId=" + pubId + "&format=interstitial&category=" + category;
            webView.loadUrl(url);

            dialog.setContentView(webView);
            dialog.show();
        });
    }

    public static void showBanner(Activity activity, FrameLayout container, String category, AdStyle style) {
        if (pubId == null) throw new IllegalStateException("AdSwap must be initialized first");

        activity.runOnUiThread(() -> {
            WebView webView = new WebView(activity);
            setupWebView(webView, activity, null, container);

            String url = BASE_URL + "?pubId=" + pubId + "&format=banner&category=" + category;
            if (style != null) {
                url += "&bg=" + style.bgColor + "&title=" + style.titleColor + "&desc=" + style.descColor;

                float density = activity.getResources().getDisplayMetrics().density;
                if (style.forcedWidthDp > 0) {
                    ViewGroup.LayoutParams p = container.getLayoutParams();
                    p.width = (int) (style.forcedWidthDp * density);
                    container.setLayoutParams(p);
                }
                if (style.forcedHeightDp > 0) {
                    ViewGroup.LayoutParams p = container.getLayoutParams();
                    p.height = (int) (style.forcedHeightDp * density);
                    container.setLayoutParams(p);
                }
            }

            webView.loadUrl(url);

            container.removeAllViews();
            container.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        });
    }

    private static void setupWebView(WebView webView, Activity activity, Dialog dialog, FrameLayout container) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String clickedUrl = request.getUrl().toString();
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

            @JavascriptInterface
            public void resizeBanner(final int cssHeightPx) {
                if (container != null) {
                    activity.runOnUiThread(() -> {
                        // LA MAGIA È QUI: Trasformiamo i pixel CSS di JS in Pixel Fisici di Android!
                        float density = activity.getResources().getDisplayMetrics().density;
                        int physicalPixels = (int) (cssHeightPx * density);

                        ViewGroup.LayoutParams containerParams = container.getLayoutParams();
                        containerParams.height = physicalPixels;
                        container.setLayoutParams(containerParams);
                    });
                }
            }
        }, "AdSwapAndroid");
    }
}