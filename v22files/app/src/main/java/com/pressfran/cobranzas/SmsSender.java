package com.pressfran.cobranzas;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;

public class SmsSender {
    public static boolean send(Context context, DbHelper db, Client c, Loan l,
                               String eventKey, String eventType, String message) {
        String phone = c.phone == null ? "" : c.phone.trim().replaceAll("[^0-9+]", "");
        if (phone.length() < 8) return false;

        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        long logId = db.logSmsAttempt(l.id,c.id,eventKey,eventType,phone,message);
        if (logId <= 0) return false;

        try {
            SmsManager manager;
            int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                manager = SmsManager.getSmsManagerForSubscriptionId(subId);
            else manager = SmsManager.getDefault();

            ArrayList<String> parts = manager.divideMessage(message);
            ArrayList<PendingIntent> sent = new ArrayList<>();
            ArrayList<PendingIntent> delivered = new ArrayList<>();
            for (int i=0;i<parts.size();i++) {
                if (i==parts.size()-1) {
                    Intent si=new Intent(context,SmsStatusReceiver.class).setAction(SmsStatusReceiver.ACTION_SENT)
                            .putExtra("log_id",logId);
                    Intent di=new Intent(context,SmsStatusReceiver.class).setAction(SmsStatusReceiver.ACTION_DELIVERED)
                            .putExtra("log_id",logId);
                    int base=(int)(logId%100000);
                    sent.add(PendingIntent.getBroadcast(context,400000+base,si,
                            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
                    delivered.add(PendingIntent.getBroadcast(context,500000+base,di,
                            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
                } else {
                    sent.add(null); delivered.add(null);
                }
            }
            manager.sendMultipartTextMessage(phone,null,parts,sent,delivered);
            db.updateSmsStatus(logId,"EN COLA","Enviado al sistema Android");
            return true;
        } catch (Exception e) {
            db.updateSmsStatus(logId,"ERROR",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));
            return false;
        }
    }
}
