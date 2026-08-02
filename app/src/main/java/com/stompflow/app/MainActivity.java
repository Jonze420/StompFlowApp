package com.stompflow.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

/**
 * StompFlow — offline WebView wrapper.
 *
 * The entire app (guitar FX chain, drum machine, tuner, presets, settings)
 * ships as a single bundled asset: app/src/main/assets/StompFlow.html.
 * No network connection or remote server is required to run it.
 *
 * The asset is served through WebViewAssetLoader on a virtual
 * "https://appassets.androidplatform.net" origin rather than a raw
 * file:// URL. This matters specifically for microphone access:
 * Chromium's WebView engine treats file:// pages as a non-secure
 * context on many Android versions and will silently refuse
 * getUserMedia() calls. Serving the same asset over a synthetic
 * https origin sidesteps that restriction reliably.
 */
public class MainActivity extends AppCompatActivity {

    private static final String APP_DOMAIN = "appassets.androidplatform.net";
    private static final String APP_URL = "https://" + APP_DOMAIN + "/assets/StompFlow.html";
    private static final int MIC_PERMISSION_REQUEST = 1001;

    private WebView webView;
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(APP_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        setupWebView();
        requestMicrophonePermission();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // Core
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Media / Audio
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Layout
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);

        // Cache — everything StompFlow needs (presets, drum patterns) lives in
        // localStorage inside the page itself, so default caching is enough.
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Route requests for our virtual domain to the bundled assets folder.
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://" + APP_DOMAIN)) {
                    return false; // stay inside the WebView
                }
                return true; // anything else opens in the system browser
            }
        });

        // Handle permissions + mic
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Only auto-grant capture permissions (mic) to StompFlow's own
                // virtual origin. Anything else gets denied rather than blindly trusted.
                String origin = request.getOrigin().toString();
                if (origin.startsWith("https://" + APP_DOMAIN)) {
                    request.grant(request.getResources());
                } else {
                    request.deny();
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                callback.invoke(origin, false, false);
            }
        });

        webView.loadUrl(APP_URL);
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Reload so the page can access the mic
                webView.reload();
            } else {
                Toast.makeText(this,
                        "Microphone permission is required for guitar effects.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // Handle hardware back button — navigate back in WebView history
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
