package dk.itu.jbec.cta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by wismann on 13/05/2017.
 */

public class Autostart extends BroadcastReceiver
{
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context, GattService.class);
        context.startService(intent);
        Log.i("Autostart ", "GattService");
    }
}