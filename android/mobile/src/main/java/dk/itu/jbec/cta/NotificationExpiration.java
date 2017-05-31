package dk.itu.jbec.cta;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationExpiration extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if ("com.your.package.action.CANCEL_NOTIFICATION".equals(action)) {
            int id = intent.getIntExtra("notification_id", -1);
            if (id != -1) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(id);
            }
        }
    }
}
