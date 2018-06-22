package com.example.lado.torbrowser;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.ParcelFileDescriptor;
import android.text.format.Formatter;
import android.webkit.WebView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public class VpnService extends android.net.VpnService{
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    WebView webView;

    Builder builder = new Builder();

    // Services interface
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

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (mThread != null) {
            mThread.interrupt();
        }
        super.onDestroy();
    }
}
