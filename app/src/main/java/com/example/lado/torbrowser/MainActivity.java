package com.example.lado.torbrowser;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.controller.network.TorServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {
    Button search;
    EditText enterURL;
    WebView webView;
    private static final String TAG = "Main";
    private ProgressDialog progressBar;

    private VideoView mVideoView;
    private RelativeLayout mContentView;
    private FrameLayout mCustomViewContainer;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //class of my vpn
        VpnService vpn = new VpnService();

        search = (Button) findViewById(R.id.Go);
        enterURL = (EditText) findViewById(R.id.enterURL);
        webView = (WebView) findViewById(R.id.webView);

        webView.setWebViewClient(new MyBrowser());

        //using vpnservice
        Intent intent = new Intent(this, VpnService.class);
        startService(intent);
        webView.loadUrl("https://google.com");
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.clearHistory();
        webView.clearCache(true);
        webView.getSettings().setSupportMultipleWindows(true);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if click to search -> using VPN
                Intent intent = VpnService.prepare(getApplicationContext());
                if (intent != null) {
                    startActivityForResult(intent, 0);
                } else {
                    onActivityResult(0, RESULT_OK, null);
                }

                String url = enterURL.getText().toString();
                webView.getSettings().setLoadsImagesAutomatically(true);
                webView.getSettings().setJavaScriptEnabled(true);

                if (savedInstanceState == null) {
                    webView.loadUrl(url);
                }
                webView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
                        WebView.HitTestResult result = view.getHitTestResult();
                        String data = result.getExtra();
                        Context context = view.getContext();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                        context.startActivity(browserIntent);
                        Toast.makeText(context, "New Tab", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            }
        });
        webView.setWebChromeClient(new MyChromeClients());
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar = ProgressDialog.show(MainActivity.this, "WebView Example", "Loading...");
            Log.i(TAG, "Finished loading URL: " + url);
            enterURL.setText(url);
            if (progressBar.isShowing()) {
                progressBar.dismiss();
            }
        }
    }

    private class MyChromeClients extends WebChromeClient implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);

        @SuppressLint("ResourceType")
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (view instanceof FrameLayout) {

                // mainWebView is the view that the video should've played inside.
                webView = (WebView) findViewById(R.id.webView);

                mCustomViewContainer = (FrameLayout) view;
                mCustomViewCallback = callback;

                // mainLayout is the root layout that (ex. the layout that contains the webview)
                mContentView = (RelativeLayout) findViewById(R.layout.activity_main);
                if (mCustomViewContainer.getFocusedChild() instanceof VideoView) {
                    mVideoView = (VideoView) mCustomViewContainer.getFocusedChild();
                    // frame.removeView(video);
                    mContentView.setVisibility(View.GONE);
                    mCustomViewContainer.setVisibility(View.VISIBLE);
                    setContentView(mCustomViewContainer);
                    mVideoView.setOnCompletionListener(this);
                    mVideoView.setOnErrorListener(this);
                    mVideoView.start();

                }
            }
        }

        public void onHideCustomView() {
            if (mVideoView == null) {
                return;
            } else {
                // Hide the custom view.
                mVideoView.setVisibility(View.GONE);
                // Remove the custom view from its container.
                mCustomViewContainer.removeView(mVideoView);
                mVideoView = null;
                mCustomViewContainer.setVisibility(View.GONE);
                mCustomViewCallback.onCustomViewHidden();
                // Show the content view.
                mContentView.setVisibility(View.VISIBLE);
            }
        }

        public void onCompletion(MediaPlayer mp) {
            mp.stop();
            mCustomViewContainer.setVisibility(View.GONE);
            onHideCustomView();
            setContentView(mContentView);
        }

        public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
            setContentView(mContentView);
            return true;
        }
    }

    //if everything is ok -> start using VPN
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, VpnService.class);
            startService(intent);
        }
    }
}
