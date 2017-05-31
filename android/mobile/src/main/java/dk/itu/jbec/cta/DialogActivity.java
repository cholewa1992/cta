package dk.itu.jbec.cta;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class DialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        Intent intent = getIntent();
        final String serviceid = intent.getStringExtra("serviceid");
        final String userid = intent.getStringExtra("userid");

        if(serviceid != null && userid != null) {


            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("Do you want to register with " + serviceid + "?");
            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(DialogActivity.this, GattService.class);
                            intent.putExtra("serviceid", serviceid);
                            intent.putExtra("userid", userid);

                            startService(intent);

                            finish();
                        }
                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        } else {
            finish();
        }
    }
}
