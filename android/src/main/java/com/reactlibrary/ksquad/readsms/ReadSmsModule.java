package com.reactlibrary.ksquad.readsms;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReadSmsModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private BroadcastReceiver msgReceiver;

    public ReadSmsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReadSms";
    }

    @ReactMethod
    public void stopReadSMS() {
        try {
            if (reactContext != null && msgReceiver != null) {
                reactContext.unregisterReceiver(msgReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @ReactMethod
    public void startReadSMS(final Callback success, final Callback error) {
        try {
            if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(reactContext, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                msgReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        WritableMap params = Arguments.createMap();
                        final Bundle bundle = intent.getExtras();
                        SmsMessage message = null;
                        String receiver = "";
                        if (bundle != null) {
                            final Object[] pdusObj = (Object[]) bundle.get("pdus");
                            if (pdusObj != null) {
                                for (Object aPdusObj : pdusObj) {
                                    try {
                                        message = SmsMessage.createFromPdu((byte[]) aPdusObj);

                                        // Construct a PduParser instance
                                        Class pduParserClass = Class.forName("com.android.internal.telephony.gsm.SmsMessage$PduParser");
                                        Constructor constructor = pduParserClass.getDeclaredConstructor(byte[].class);
                                        constructor.setAccessible(true);
                                        Object pduParser = constructor.newInstance((byte[]) aPdusObj);

                                        // Get the GsmSmsAddress instance using getAddress
                                        Method method = pduParser.getClass().getDeclaredMethod("getAddress");
                                        method.setAccessible(true);
                                        Object gsmSmsAddress = method.invoke(pduParser);

                                        // Get the recipient address from GsmSmsAddress using getAddressString
                                        Method method2 = gsmSmsAddress.getClass().getMethod("getAddressString");
                                        receiver = (String)method2.invoke(gsmSmsAddress);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        params.putString("message", message.getDisplayMessageBody());
                        params.putString("receiver", receiver);

                        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("received_sms", params);
                    }
                };
                String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
                reactContext.registerReceiver(msgReceiver, new IntentFilter(SMS_RECEIVED_ACTION));
                success.invoke("Start Read SMS successfully");
            } else {
                // Permission has not been granted
                error.invoke("Required RECEIVE_SMS and READ_SMS permission");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
