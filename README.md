# WebBrowserVPN
WebBrowser using VPN

I decided to change a little bit my project and to add VPN.

Thanks for them I can change my IP address.

So how I did it?

Before I added in xml some components like: WebView, TextView and Button.

Next is make and use WebView.
``` java
search = (Button) findViewById(R.id.Go);
enterURL = (EditText) findViewById(R.id.enterURL);
webView = (WebView) findViewById(R.id.webView);

webView.setWebViewClient(new MyBrowser());
```

This is the first step.

The next step is
``` java
    webView.loadUrl("https://google.com");
    webView.getSettings().setBuiltInZoomControls(true);
    webView.getSettings().setDisplayZoomControls(true);
    webView.clearHistory();
    webView.clearCache(true);
    webView.getSettings().setSupportMultipleWindows(true);
    search.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
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
```

So ```java webView.setWebChromeClient(new WebChromeClient()``` this is if you want to add a tab.

```java
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
```

Everything which started form there is to see video contents

```java 
private class MyBrowser extends WebViewClient {...}
```

After we can use VpnService. 

I did this things

```java 
Intent intent = VpnService.prepare(getApplicationContext());
if (intent != null) {
    startActivityForResult(intent, 0);
} else {
    onActivityResult(0, RESULT_OK, null);
}
```

I added this when we press on search button. 

And this is my VpnService.

```java
@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //getting ip by WifiManager
                    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                    String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                    //a. Configure the TUN and get the interface.
                    mInterface = builder.setSession("MyVPNService")
                            .addAddress(ipAddress, 24)
                            .establish();
                    while (true) {
                        //get packet with in
                        //put packet to tunnel
                        //get packet form tunnel
                        //return packet with out
                        //sleep is a must
                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    // Catch any exception
                    e.printStackTrace();
                } finally {
                    try {
                        if (mInterface != null) {
                            mInterface.close();
                            mInterface = null;
                        }
                    } catch (Exception e) {

                    }
                }
            }

        }, "MyVpnRunnable");

        //start the service
        mThread.start();
        return START_STICKY;
    }
```
