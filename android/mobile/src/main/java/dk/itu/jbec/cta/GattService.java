package dk.itu.jbec.cta;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dk.itu.jbec.sharedservices.Crypto.CryptoException;
import dk.itu.jbec.sharedservices.CryptoService;

/**
 * Created by jbec on 26/04/2017.
 */

public class GattService extends Service {


    private static final String TAG = GattService.class.getSimpleName();

    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate");

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {

            //TODO THIS IS NOT PROPER!
            return;
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }


        Intent i = new Intent(this, CryptoService.class);
        bindService(i, mConnection, BIND_AUTO_CREATE);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null) {
            String serviceid = intent.getStringExtra("serviceid");
            String userid = intent.getStringExtra("userid");

            if (serviceid != null && userid != null) {
                completeRegistration(serviceid, userid);
            }
        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy");

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }

        unregisterReceiver(mBluetoothReceiver);

        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    boolean mBounded;
    CryptoService.CryptoBinder mCryptoBinder;
    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mCryptoBinder = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            mCryptoBinder = (CryptoService.CryptoBinder) service;
        }
    };


    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }

        }
    };

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Authentication Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(AuthenticationProfile.AUTHENTICATION_SERVICE))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {

        Log.i(TAG, "stopAdvertising");

        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(AuthenticationProfile.createAuthenticationService());
    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        Log.i(TAG, "stopServer");
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    private void completeRegistration(String serviceId, String userid){
        try {
            mCryptoBinder.register(serviceId, userid, new ResultCallback<CryptoService.RegistrationResult>() {
                @Override
                public void onResult(@NonNull CryptoService.RegistrationResult registrationResult) {
                    Log.i(TAG, "Now notifying change");

                    BluetoothGattCharacteristic register = mBluetoothGattServer
                            .getService(AuthenticationProfile.AUTHENTICATION_SERVICE)
                            .getCharacteristic(AuthenticationProfile.REGISTER);

                    byte[] y = registrationResult.getY();
                    byte[] p = registrationResult.getP();
                    byte[] g = registrationResult.getG();

                    byte[] msg = new byte[y.length + p.length + g.length];

                    System.arraycopy(y, 0, msg, 0, y.length);
                    System.arraycopy(p, 0, msg, y.length, p.length);
                    System.arraycopy(g, 0, msg, y.length + p.length, g.length);

                    Log.i(TAG, "Printing reg array content: " +  Arrays.toString(msg));

                    register.setValue(msg);

                    for (BluetoothDevice device : mRegisteredDevices) {
                        mBluetoothGattServer.notifyCharacteristicChanged(device, register, false);
                    }
                }
            });
        } catch (CryptoException e) {

            e.printStackTrace();

        }
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                mRegisteredDevices.remove(device);
            } else {
                Log.i(TAG, "ERROR!");
            }

        }

        @Override
        public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId, int offset, final BluetoothGattCharacteristic characteristic) {

            Log.i(TAG, "onCharacteristicReadRequest with offset: " + offset);

            if(AuthenticationProfile.REGISTER.equals(characteristic.getUuid())){

                 mBluetoothGattServer.sendResponse(device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                characteristic.getValue());


            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }

        }

        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device, int requestId, final BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i(TAG, "onCharacteristicWriteRequest");

            if (AuthenticationProfile.REGISTER.equals(characteristic.getUuid())) {

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }

                BluetoothGattDescriptor sid = characteristic.getDescriptor(AuthenticationProfile.SERVICE_IDENTITY);
                BluetoothGattDescriptor uid = characteristic.getDescriptor(AuthenticationProfile.USER_IDENTITY);

                if (sid.getValue() != null && uid.getValue() != null) {

                    requestRegistration(new String(sid.getValue()), new String(uid.getValue()));
                    sid.setValue(null);
                    sid.setValue(null);

                }

            } else if (AuthenticationProfile.AUTHENTICATE.equals(characteristic.getUuid())) {

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }

                final byte[] sid = characteristic.getDescriptor(AuthenticationProfile.SERVICE_IDENTITY).getValue();
                final byte[] uid = characteristic.getDescriptor(AuthenticationProfile.USER_IDENTITY).getValue();
                byte[] challenge = characteristic.getDescriptor(AuthenticationProfile.CHALLENGE).getValue();

                if (sid != null && uid != null && challenge != null) {

                    try {

                        mCryptoBinder.authenticate(new String(sid), new String(uid), challenge, new ResultCallback<CryptoService.AuthenticationResult>() {
                            @Override
                            public void onResult(@NonNull CryptoService.AuthenticationResult authenticationResult) {
                                Log.i(TAG, "Now notifying change");

                                BluetoothGattCharacteristic authenticate = mBluetoothGattServer
                                        .getService(AuthenticationProfile.AUTHENTICATION_SERVICE)
                                        .getCharacteristic(AuthenticationProfile.AUTHENTICATE);

                                authenticate.setValue(authenticationResult.getReponse());
                                Log.i(TAG, Arrays.toString(authenticationResult.getReponse()));
                                Log.i(TAG, "" + new BigInteger(1, authenticationResult.getReponse()).longValue());

                                for (BluetoothDevice device : mRegisteredDevices) {
                                    mBluetoothGattServer.notifyCharacteristicChanged(device, authenticate, false);
                                }

                                requestAuthentication(new String(sid), new String(uid));

                            }
                        });

                    } catch (CryptoException e) {

                        e.printStackTrace();

                    } finally {
                        characteristic.getDescriptor(AuthenticationProfile.SERVICE_IDENTITY).setValue(null);
                        characteristic.getDescriptor(AuthenticationProfile.USER_IDENTITY).setValue(null);
                        characteristic.getDescriptor(AuthenticationProfile.CHALLENGE).setValue(null);
                    }
                }

            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (AuthenticationProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        returnValue);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {


            Log.i(TAG, "onDescriptorWriteRequest");

            if (AuthenticationProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                byte[] returnValue;

                if (mRegisteredDevices.contains(device)) { returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; }
                else { returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE; }

                descriptor.setValue(value);

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            returnValue);
                }
            }

            else if(AuthenticationProfile.WRITABLE_DESCRIPTORS.contains(descriptor.getUuid())){

                descriptor.setValue(value);

                Log.i(TAG, "value is: " + new String(value));
                Log.i(TAG, "array is: " + Arrays.toString(value));

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }


            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }

        }
    };

    private int notificationId = 1;

    private void requestRegistration(String serviceid, String userid){

        String title = "Approve registration";
        String text = "with " + serviceid;

        // Build intent for notification content
        Intent viewIntent = new Intent(this, DialogActivity.class);
        viewIntent.putExtra("serviceid", serviceid);
        viewIntent.putExtra("userid", userid);

        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(this, 0, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.common_google_signin_btn_text_dark)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setGroup("registration")
                        .setContentIntent(viewPendingIntent)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Issue the notification with notification manager.
        notificationManager.notify(notificationId++, notificationBuilder.build());

    }

    private void requestAuthentication(String serviceid, String userid){

        String title = "Authenticated";
        String text = "with " + serviceid;

        // Build intent for notification content
        //Intent viewIntent = new Intent(this, ViewEventActivity.class);

        /*
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(this, 0, viewIntent, 0);
        */

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(dk.itu.jbec.sharedservices.R.drawable.common_google_signin_btn_icon_dark)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setGroup("authentication");

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Issue the notification with notification manager.
        notificationManager.notify(0, notificationBuilder.build());

        Context context;
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationExpiration.class);
        intent.setAction("com.your.package.action.CANCEL_NOTIFICATION");
        intent.putExtra("notification_id", 0);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // note: starting with KitKat, use setExact if you need exact timing
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + 15000), pi);

    }

}
