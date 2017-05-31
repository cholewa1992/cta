package dk.itu.jbec.sharedservices.Communication;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

public class CommunicationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ChannelApi.ChannelListener {

    public static String TAG = "CommunicationService";
    private GoogleApiClient mGoogleApiClient;
    private String mNodeId;
    private ChannelListener listener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        Wearable.ChannelApi.addListener(mGoogleApiClient, this);

        return new CommunicationBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Wearable.ChannelApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        listener = null;
        return super.onUnbind(intent);
    }

    public void setChannelListener(@NonNull ChannelListener listener) {
        this.listener = listener;
    }

    @WorkerThread
    private void openChannel(final String path, final ChannelHandler handler) throws CommunicationServiceException {

        Log.i(TAG, "Now opening channel");

        if(!mGoogleApiClient.isConnected()) throw new CommunicationServiceException("Not connected");

        String nodeId = getNodeId();
        if(nodeId == null) throw new CommunicationServiceException("No node available");

        /* Getting a channel to the node */
        final PendingResult<ChannelApi.OpenChannelResult> openChannelResultPendingResult = Wearable.ChannelApi.openChannel(mGoogleApiClient, nodeId, path);

        /* Getting the data channel */
        openChannelResultPendingResult.setResultCallback(new ResultCallback<ChannelApi.OpenChannelResult>() {
            @Override
            public void onResult(@NonNull ChannelApi.OpenChannelResult openChannelResult) {
                if(openChannelResult.getChannel() != null)
                handler.setGoogleApiClient(mGoogleApiClient);
                handler.setChannel(openChannelResult.getChannel());
            }
        });

    }

    @WorkerThread
    private String getNodeId() {

        if (mNodeId == null) {
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(5, TimeUnit.SECONDS).getNodes();
            if (nodes != null) {
                for (Node node : nodes) {
                    if (node.isNearby()) {
                        mNodeId = node.getId();
                        Log.i(TAG, "Found " + node.getDisplayName());
                        break;
                    }
                }
            }
        }

        return mNodeId;
    }

    @Override
    public void onChannelOpened(final Channel channel) {

        if(listener != null){

            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ChannelHandler handler = listener.onChannelOpened(channel.getPath());
                    handler.setGoogleApiClient(mGoogleApiClient);
                    handler.setChannel(channel);
                    return null;
                }
            };

            task.execute();

        }
    }

    /* Java Garbage that isn't needed */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed");
    }


    @Override
    public void onChannelClosed(final Channel channel, final int closeReason, final int appSpecificErrorCode) {

        switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                Log.d(TAG, "onChannelClosed: Channel closed. Reason: normal close (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_DISCONNECTED:
                Log.d(TAG, "onChannelClosed: Channel closed. Reason: disconnected (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_REMOTE_CLOSE:
                Log.d(TAG, "onChannelClosed: Channel closed. Reason: closed by remote (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_LOCAL_CLOSE:
                Log.d(TAG, "onChannelClosed: Channel closed. Reason: closed locally (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
        }
    }

    @Override
    public void onInputClosed(final Channel channel, final int closeReason, final int appSpecificErrorCode) {

        switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                Log.d(TAG, "onInputClosed: Channel input side closed. Reason: normal close (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_DISCONNECTED:
                Log.d(TAG, "onInputClosed: Channel input side closed. Reason: disconnected (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_REMOTE_CLOSE:
                Log.d(TAG, "onInputClosed: Channel input side closed. Reason: closed by remote (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_LOCAL_CLOSE:
                Log.d(TAG, "onInputClosed: Channel input side closed. Reason: closed locally (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
        }
    }

    @Override
    public void onOutputClosed(final Channel channel, final int closeReason, final int appSpecificErrorCode) {

        switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                Log.d(TAG, "onOutputClosed: Channel output side closed. Reason: normal close (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_DISCONNECTED:
                Log.d(TAG, "onOutputClosed: Channel output side closed. Reason: disconnected (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_REMOTE_CLOSE:
                Log.d(TAG, "onOutputClosed: Channel output side closed. Reason: closed by remote (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_LOCAL_CLOSE:
                Log.d(TAG, "onOutputClosed: Channel output side closed. Reason: closed locally (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
        }
    }

    /* Sub classes */

    public class CommunicationBinder extends Binder {


        public void setChannelListener(ChannelListener listener) {
            CommunicationService.this.setChannelListener(listener);
        }

        public void openChannel(final String path, final ChannelHandler handler){

            new AsyncTask<Void,Void,Boolean>(){

                @Override
                protected Boolean doInBackground(Void... params) {


                    try {
                        CommunicationService.this.openChannel(path, handler);
                    } catch (CommunicationServiceException e) {
                        e.printStackTrace();
                        return false;
                    }

                    return true;

                }
            }.execute();
        }

    }

    public interface ChannelListener {

        ChannelHandler onChannelOpened(String path);
    }

    public static abstract class ChannelHandler {

        private final AtomicReference<Channel> channel = new AtomicReference<>();
        private final AtomicReference<InputStream> in = new AtomicReference<>();
        private final AtomicReference<OutputStream> out = new AtomicReference<>();
        private final AtomicReference<GoogleApiClient> client = new AtomicReference<>();

        private void setChannel(Channel channel){
            this.channel.set(channel);
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    onChannelOpened();
                    return null;
                }
            }.execute();
        }

        private final void setGoogleApiClient(GoogleApiClient client){

            this.client.set(client);
        }


        protected abstract void onChannelOpened();


        @WorkerThread
        protected final InputStream getInputStream() throws CommunicationServiceException {

            if(client.get() == null) throw new CommunicationServiceException("GoogleApiClient is not set");
            if (channel.get() == null) throw new CommunicationServiceException("The channel is not open");

            if(in.get() == null) {
                Channel.GetInputStreamResult result = channel.get().getInputStream(client.get()).await();
                in.set(result.getInputStream());
            }
            return in.get();
        }

        @WorkerThread
        protected final OutputStream getOutputStream() throws CommunicationServiceException {

            if(client.get() == null) throw new CommunicationServiceException("GoogleApiClient is not set");
            if (channel.get() == null) throw new CommunicationServiceException("The channel is not open");

            if(out.get() == null) {
                Channel.GetOutputStreamResult result = channel.get().getOutputStream(client.get()).await();
                out.set(result.getOutputStream());
            }
            return out.get();
        }

        @WorkerThread
        protected final byte[] readToEnd() throws IOException, CommunicationServiceException {
            Log.i(TAG, "Now trying to read");
            InputStream in = getInputStream();
            return IOUtils.toByteArray(in);
        }

        @WorkerThread
        protected final void send(byte[] msg) throws IOException, CommunicationServiceException {
            Log.i(TAG, "Now trying to send");
            OutputStream out = getOutputStream();
            out.write(msg);
            out.flush();
        }

        @WorkerThread
        protected final void closeOutputStream() {
            try { if(out.get() != null) out.get().close(); }
            catch (IOException ignored) { }
            finally { out.set(null); }
        }

        @WorkerThread
        protected final void closeInputStream() {
            try { if(in.get() != null) in.get().close(); }
            catch (IOException ignored) { }
            finally { in.set(null); }
        }

        @WorkerThread
        protected final void close(){
            closeOutputStream();
            closeInputStream();
            channel.get().close(client.get());
            channel.set(null);
        }

    }

}




