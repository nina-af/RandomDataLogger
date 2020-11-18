package com.example.randomdatalogger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;



public class GetIPAddressActivity extends AppCompatActivity {

    private static final String TAG = "GetIPAddressActivity";

    private Button mBtnGetIPAddress;
    private TextView textViewIPAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_i_p_address);

        mBtnGetIPAddress = (Button) findViewById(R.id.buttonGetIPAddress);
        textViewIPAddress = (TextView) findViewById(R.id.textViewIPAddress);

        mBtnGetIPAddress.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipAddress = getIPAddress(true);
                textViewIPAddress.setText(ipAddress);
            }
        });
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {

                Log.d(TAG, "Network interface name: " + intf.getName());

                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {

                        Log.d(TAG, "not loopback address");

                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); (deprecated)
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                Log.d(TAG, "if useIPv4 Network interface name: " + intf.getName());
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                Log.d(TAG, "if !useIPv4 Network interface name: " + intf.getName());
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}