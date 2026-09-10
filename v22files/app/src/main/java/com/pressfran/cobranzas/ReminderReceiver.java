package com.pressfran.cobranzas;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID="pressfran_cobranzas";

    @Override public void onReceive(Context context,Intent intent){
        try{
            createChannel(context);
            DbHelper db=new DbHelper(context);
            SharedPreferences sp=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
            String alias=sp.getString("alias","");
            boolean autoSms=sp.getBoolean("sms_auto_enabled",false);
            boolean pre=sp.getBoolean("sms_pre_due",true);
            boolean dueEnabled=sp.getBoolean("sms_due",true);
            boolean moraEnabled=sp.getBoolean("sms_mora",true);
            boolean commitmentEnabled=sp.getBoolean("sms_commitment",true);

            LocalDate today=LocalDate.now();
            int id=1000;

            for(Loan l:db.getActiveLoans()){
                Client c=db.getCustomer(l.clientId);
                if(c==null)continue;

                LocalDate due=LocalDate.parse(l.dueDate);
                boolean dueToday=due.equals(today);
                boolean preTomorrow=due.minusDays(1).equals(today);
                boolean overdue=MessageBuilder.overdueDays(l)>0;
                boolean commitmentToday=today.toString().equals(l.commitmentDate);

                String eventType=null,eventKey=null;
                if(commitmentEnabled && commitmentToday){
                    eventType="COMPROMISO";
                    eventKey="COMPROMISO:"+today;
                }else if(moraEnabled && overdue){
                    eventType="MORA";
                    eventKey="MORA:"+today;
                }else if(dueEnabled && dueToday){
                    eventType="VENCIMIENTO";
                    eventKey="VENCIMIENTO:"+due;
                }else if(pre && preTomorrow){
                    eventType="PREVIO";
                    eventKey="PREVIO:"+due;
                }

                if(autoSms && c.smsEnabled && eventType!=null &&
                        context.checkSelfPermission(Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED &&
                        !db.wasSmsSent(l.id,eventKey)){
                    String sms=MessageBuilder.buildSms(c,l,alias,eventType);
                    SmsSender.send(context,db,c,l,eventKey,eventType,sms);
                }

                if(dueToday||overdue||commitmentToday||preTomorrow)
                    showNotification(context,c,l,MessageBuilder.build(c,l,alias),id++);
            }
        }catch(Exception ignored){
        }finally{
            ReminderScheduler.scheduleDaily(context);
        }
    }

    private void createChannel(Context context){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationManager nm=context.getSystemService(NotificationManager.class);
            NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"Vencimientos y cobranzas",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Recordatorios automáticos de cuotas PressFran");
            nm.createNotificationChannel(ch);
        }
    }

    private void showNotification(Context context,Client c,Loan l,String message,int id){
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            String digits=c.phone==null?"":c.phone.replaceAll("\\D","");
            String encoded=URLEncoder.encode(message,StandardCharsets.UTF_8.toString());
            Uri uri=Uri.parse((digits.isEmpty()?"https://wa.me/?text=":"https://wa.me/"+digits+"?text=")+encoded);
            Intent open=new Intent(Intent.ACTION_VIEW,uri);
            PendingIntent pi=PendingIntent.getActivity(context,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            String shortText=MessageBuilder.overdueDays(l)>0?"Atraso "+MessageBuilder.overdueDays(l)+" días · "+MessageBuilder.money(MessageBuilder.amountDue(l)):"Cuota "+MessageBuilder.money(MessageBuilder.currentInstallmentPending(l));
            android.app.Notification.Builder b=new android.app.Notification.Builder(context,CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("PressFran · "+c.name)
                    .setContentText(shortText)
                    .setStyle(new android.app.Notification.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            nm.notify(id,b.build());
        }catch(Exception ignored){}
    }
}
