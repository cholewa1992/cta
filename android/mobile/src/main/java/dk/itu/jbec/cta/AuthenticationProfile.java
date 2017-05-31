package dk.itu.jbec.cta;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jbec on 25/04/2017.
 */

public class AuthenticationProfile {

    private static final String TAG = AuthenticationProfile.class.getSimpleName();

    /* Services */
    public static UUID AUTHENTICATION_SERVICE = UUID.fromString("00001506-0000-1000-8000-00805f9b34fb");

    /* Characteristics */
    public static UUID REGISTER    = UUID.fromString("00001a00-0000-1000-8000-00805f9b34fb");
    public static UUID AUTHENTICATE = UUID.fromString("00001a01-0000-1000-8000-00805f9b34fb");

    /* Descriptors */
    public static UUID SERVICE_IDENTITY = UUID.fromString("00002800-0000-1000-8000-00805f9b34fb");
    public static UUID USER_IDENTITY = UUID.fromString("00002801-0000-1000-8000-00805f9b34fb");
    public static UUID CHALLENGE = UUID.fromString("00002802-0000-1000-8000-00805f9b34fb");
    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static Set<UUID> WRITABLE_DESCRIPTORS = new HashSet<>(Arrays.asList(new UUID[]{ SERVICE_IDENTITY, USER_IDENTITY, CHALLENGE }));


    public static BluetoothGattService createAuthenticationService() {

        /* Service */
        BluetoothGattService service = new BluetoothGattService(AUTHENTICATION_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        /* Characteristics */

        BluetoothGattCharacteristic register = new BluetoothGattCharacteristic(REGISTER,
                BluetoothGattCharacteristic.PROPERTY_READ |  BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        BluetoothGattCharacteristic authenticate = new BluetoothGattCharacteristic(AUTHENTICATE,
                BluetoothGattCharacteristic.PROPERTY_READ |  BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        BluetoothGattDescriptor userIdentity = new BluetoothGattDescriptor(USER_IDENTITY, BluetoothGattDescriptor.PERMISSION_WRITE);
        BluetoothGattDescriptor serverIdentity = new BluetoothGattDescriptor(SERVICE_IDENTITY, BluetoothGattDescriptor.PERMISSION_WRITE);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        BluetoothGattDescriptor challenge = new BluetoothGattDescriptor(CHALLENGE, BluetoothGattDescriptor.PERMISSION_WRITE);

        register.addDescriptor(configDescriptor);
        register.addDescriptor(userIdentity);
        register.addDescriptor(serverIdentity);

        authenticate.addDescriptor(configDescriptor);
        authenticate.addDescriptor(userIdentity);
        authenticate.addDescriptor(serverIdentity);
        authenticate.addDescriptor(challenge);

        service.addCharacteristic(authenticate);
        service.addCharacteristic(register);

        return service;
    }

}
