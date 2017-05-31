package dk.itu.jbec.sharedservices;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

import dk.itu.jbec.sharedservices.Communication.AuthenticationRequest;
import dk.itu.jbec.sharedservices.Communication.AuthenticationResponse;
import dk.itu.jbec.sharedservices.Communication.CommunicationService;
import dk.itu.jbec.sharedservices.Communication.CommunicationServiceException;
import dk.itu.jbec.sharedservices.Communication.RegistrationRequest;
import dk.itu.jbec.sharedservices.Communication.RegistrationResponse;
import dk.itu.jbec.sharedservices.Crypto.Challenge;
import dk.itu.jbec.sharedservices.Crypto.CryptoException;
import dk.itu.jbec.sharedservices.Crypto.DistributedElgamal;
import dk.itu.jbec.sharedservices.Crypto.PrivateKey;
import dk.itu.jbec.sharedservices.Crypto.PublicKey;
import dk.itu.jbec.sharedservices.Storage.KeyInfo;

import static dk.itu.jbec.sharedservices.Communication.CommunicationService.*;


/**
 * Created by wismann on 12/04/2017.
 */

public class CryptoService extends Service implements CommunicationService.ChannelListener {

    private static final String TAG = "CryptoService";
    private final IBinder cryptoBinder = new CryptoBinder();
    private final DistributedElgamal mCrypto;

    private AuthenticationEventHandler eventHandler;

    public CryptoService() throws CryptoException {
        mCrypto = new DistributedElgamal();
    }

    public class CryptoBinder extends Binder {

        public void register(final String serviceIdentifier, final String userId, final ResultCallback<RegistrationResult> resultCallBack) throws CryptoException {
            CryptoService.this.register(serviceIdentifier, userId, resultCallBack);
        }

        public void authenticate(final String serviceIdentifier, final String userId, final byte[] challenge, final ResultCallback<AuthenticationResult> resultCallBack) throws CryptoException {
            CryptoService.this.authenticate(serviceIdentifier, userId, challenge, resultCallBack);

        }

        public void setEventHandler(AuthenticationEventHandler handler){
            CryptoService.this.eventHandler = handler;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent i = new Intent(this, CommunicationService.class);
        bindService(i, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return cryptoBinder;

    }

    boolean mBounded;
    CommunicationService.CommunicationBinder mCommunicationBinder;
    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mCommunicationBinder = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            mCommunicationBinder = (CommunicationService.CommunicationBinder) service;
            mCommunicationBinder.setChannelListener(CryptoService.this);
        }
    };



    public class RegistrationResult implements com.google.android.gms.common.api.Result{

        private final byte[] Y;
        private final byte[] P;
        private final byte[] G;

        public RegistrationResult(byte[] Y, byte[] P, byte[] G) {
            this.Y = Y;
            this.P = P;
            this.G = G;
        }


        @Override
        public Status getStatus() {
            return null;
        }

        public byte[] getG() {
            return G;
        }

        public byte[] getP() {
            return P;
        }

        public byte[] getY() {
            return Y;
        }
    }

    public void register(final String serviceIdentifier, final String userId, final ResultCallback<RegistrationResult> resultCallBack) throws CryptoException {

        Log.i(TAG, "on register");

        if(!mBounded) throw new CryptoException("Communication not ready");

        mCommunicationBinder.openChannel("/register", new ChannelHandler() {

            @Override
            protected void onChannelOpened() {

                Log.i(TAG, "Authenticator register");

                try {

                    if(eventHandler != null && !eventHandler.OnRegisterStarted(serviceIdentifier, userId)){
                        close();
                        return;
                    }

                    PrivateKey key = mCrypto.generateKey();

                    byte[] msg = serialize(new RegistrationRequest(serviceIdentifier, userId, key.getParams()));
                    send(msg);
                    closeOutputStream();

                    RegistrationResponse response = deserialize(readToEnd());
                    PublicKey publicKey = mCrypto.combinePublicKeys(key, response.key);
                    insertKey(serviceIdentifier, userId, key);


                    resultCallBack.onResult(new RegistrationResult(
                            publicKey.getY().toByteArray(),
                            publicKey.getP().toByteArray(),
                            publicKey.getG().toByteArray()
                    ));

                    close();

                    if(eventHandler != null) eventHandler.OnRegisterCompleted(serviceIdentifier, userId);


                } catch (CryptoException | CommunicationServiceException | IOException e) {
                    e.printStackTrace();
                }

            }

        });

    }



    public class AuthenticationResult implements com.google.android.gms.common.api.Result{

        private final byte[] response;
        private AuthenticationResult(byte[] response){
            this.response = response;
        }
        public byte[] getReponse() {
            return response;
        }

        @Override
        public Status getStatus() {
            return null;
        }

    }

    /* Authenticator behavior */
    public void authenticate(final String serviceIdentifier, final String userId, final byte[] challenge, final ResultCallback<AuthenticationResult> resultCallBack) throws CryptoException {

        Log.i(TAG, "on auth");

        if(!mBounded) throw new CryptoException("Communication not ready");

        mCommunicationBinder.openChannel("/auth", new ChannelHandler() {

            @Override
            protected void onChannelOpened() {

                Log.i(TAG, "Authenticator auth");

                try {

                    if(eventHandler != null && !eventHandler.OnAuthenticationStarted(serviceIdentifier, userId)){
                        close();
                        return;
                    }

                    PrivateKey key = readKey(serviceIdentifier, userId);

                    byte[] msg = serialize(new AuthenticationRequest(serviceIdentifier, userId, challenge));

                    /* Using the underlying channel to send message */
                    send(msg);
                    closeOutputStream();

                    /* Computing one part and receiving the other over the channel */
                    byte[] part1 = mCrypto.partialDecrypt(challenge, key);

                    /* Getting response from the sibling */
                    AuthenticationResponse response = deserialize(readToEnd());
                    byte[] part2 = response.cipher;


                    byte[] answer = mCrypto.combineCiphers(part1, part2, challenge, key);

                    resultCallBack.onResult(new AuthenticationResult(answer));

                    close();

                    if(eventHandler != null) eventHandler.OnAuthenticationCompleted(serviceIdentifier, userId);

                } catch (CommunicationServiceException | IOException | CryptoException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    /* Test method! */
    public Challenge makeChallenge(byte[] publickey) throws CommunicationServiceException {

        PublicKey key = deserialize(publickey);

        try { return mCrypto.createChallenge(key); }
        catch (CryptoException e) {
            e.printStackTrace();
            throw new CommunicationServiceException("The challenge could not be created");
        }

    }

    /* Sibling behavior */
    @Override
    public ChannelHandler onChannelOpened(String path) {

        if(path.equals("/auth")) {

            return new ChannelHandler() {

                @Override
                protected void onChannelOpened() {

                    Log.i(TAG, "Sibling Auth");

                    try {

                        AuthenticationRequest request = deserialize(readToEnd());
                        closeInputStream();

                        if(eventHandler != null && !eventHandler.OnAuthenticationStarted(request.serviceIdentifier, request.userId)){
                            close();
                            return;
                        }

                        PrivateKey key = readKey(request.serviceIdentifier, request.userId);

                        byte[] cipher = mCrypto.partialDecrypt(request.challenge, key);
                        byte[] msg = serialize(new AuthenticationResponse(cipher));

                        send(msg);

                        close();

                        if(eventHandler != null) eventHandler.OnAuthenticationCompleted(request.serviceIdentifier, request.userId);

                    } catch (CommunicationServiceException | IOException | CryptoException e) {
                        e.printStackTrace();
                    }

                }
            };
        } else if(path.equals("/register")) {

            return new ChannelHandler() {
                @Override
                protected void onChannelOpened() {

                    Log.i(TAG,"Sibling register");

                    try {

                        RegistrationRequest request = deserialize(readToEnd());
                        closeInputStream();

                        if(eventHandler != null && !eventHandler.OnRegisterStarted(request.serviceIdentifier, request.userId)){
                            close();
                            return;
                        }

                        /* Generating and storing new key */
                        PrivateKey key = mCrypto.generateKey(request.params);
                        insertKey(request.serviceIdentifier, request.userId, key);

                        /* Sending the public key back */
                        byte[] msg = serialize(new RegistrationResponse(key.getPublicKey()));
                        send(msg);

                        close();

                        if(eventHandler != null) eventHandler.OnRegisterCompleted(request.serviceIdentifier, request.userId);


                    } catch (IOException | CommunicationServiceException | CryptoException e) {
                        e.printStackTrace();
                    }

                }
            };

        }

        Log.i(TAG, "Something unexpected happened!");

        return new ChannelHandler() {
            @Override
            protected void onChannelOpened() {

            }
        };
    }




    private byte[] serialize(Object o) throws CommunicationServiceException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {

            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            return bos.toByteArray();

        } catch (IOException e) {

            e.printStackTrace();
            throw new CommunicationServiceException("Object could not be serialized");

        } finally {

            try { bos.close(); }
            catch (IOException ignored) { }

        }
    }

    private <T> T deserialize(byte[] bytes) throws CommunicationServiceException {

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        ObjectInput in = null;
        try {

            in = new ObjectInputStream(bis);
            return (T) in.readObject();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            throw new CommunicationServiceException("Object could not be deserialized");

        } finally {

            try { if (in != null) in.close(); }
            catch (IOException ignored) { }

        }
    }

    private ConcurrentHashMap<String, KeyInfo> keys = new ConcurrentHashMap<>();

    private PrivateKey readKey(String serviceIdentifier, String userId) throws CryptoException {

        String index = serviceIdentifier + ":" + userId;
        if(!keys.containsKey(index)) throw new CryptoException("The key is not present");

        KeyInfo ki = keys.get(index);
        return new PrivateKey(ki.x, ki.y, ki.p, ki.g);

    }
    private void insertKey(String serviceIdentifier, String userId, PrivateKey key){
        String index = serviceIdentifier + ":" + userId;
        KeyInfo ki = new KeyInfo(serviceIdentifier, userId, key.getX(), key.getY(), key.getG(), key.getP());
        keys.put(index, ki);
    }

    public static abstract class AuthenticationEventHandler {
        protected boolean OnRegisterStarted(String serviceid, String userid) { return true; }
        protected boolean OnAuthenticationStarted(String serviceid, String userid) { return true; }
        protected void OnRegisterCompleted(String serviceid, String userid) {}
        protected void OnAuthenticationCompleted(String serviceid, String userid) {}
    }
}

