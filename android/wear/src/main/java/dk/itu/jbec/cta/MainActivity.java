package dk.itu.jbec.cta;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

import dk.itu.jbec.sharedservices.CryptoService;

public class MainActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, CryptoService.class));
        finish();

    }

}
