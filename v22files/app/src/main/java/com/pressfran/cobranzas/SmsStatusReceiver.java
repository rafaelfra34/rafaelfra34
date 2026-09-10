package com.pressfran.cobranzas;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class SmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_SENT="com.pressfran.cobranzas.SMS_SENT";
    public static final String ACTION_DELIVERED="com.pressfran.cobranzas.SMS_DELIVERED";

    @Override public void onReceive(Context context, Intent intent) {
        long logId=intent.getLongExtra("log_id",0);
        if(logId<=0)return;
        DbHelper db=new DbHelper(context);
        if(ACTION_DELIVERED.equals(intent.getAction())){
            db.updateSmsStatus(logId,getResultCode()==Activity.RESULT_OK?"ENTREGADO":"SIN CONFIRMAR ENTREGA",
                    "Resultado de entrega: "+getResultCode());
            return;
        }
        String status,detail;
        switch(getResultCode()){
            case Activity.RESULT_OK: status="ENVIADO"; detail="Aceptado por la red móvil"; break;
            case SmsManager.RESULT_ERROR_NO_SERVICE: status="ERROR"; detail="Sin servicio móvil"; break;
            case SmsManager.RESULT_ERROR_RADIO_OFF: status="ERROR"; detail="Radio móvil apagada"; break;
            case SmsManager.RESULT_ERROR_NULL_PDU: status="ERROR"; detail="Error de red/PDU"; break;
            default: status="ERROR"; detail="Código de envío: "+getResultCode();
        }
        db.updateSmsStatus(logId,status,detail);
    }
}
