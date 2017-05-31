package dk.itu.jbec.cta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import dk.itu.jbec.sharedservices.CryptoService;

/**
 * Created by wismann on 13/05/2017.
 */

public class Autostart extends BroadcastReceiver
{
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context, CryptoService.class);
        context.startService(intent);
        Log.i("Autostart ", "CryptoService");
    }
}