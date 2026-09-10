package com.pressfran.cobranzas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class ReminderScheduler {
    public static void scheduleDaily(Context context){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(context,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(context,101,i,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        int hour=context.getSharedPreferences("settings",Context.MODE_PRIVATE).getInt("sms_hour",8);
        int minute=context.getSharedPreferences("settings",Context.MODE_PRIVATE).getInt("sms_minute",0);
        hour=Math.max(0,Math.min(23,hour));minute=Math.max(0,Math.min(59,minute));

        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,hour);
        cal.set(Calendar.MINUTE,minute);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);

        am.cancel(pi);
        long t=cal.getTimeInMillis();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
            if(am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t,pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t,pi);
        }else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t,pi);
        }else{
            am.setExact(AlarmManager.RTC_WAKEUP,t,pi);
        }
    }
}
