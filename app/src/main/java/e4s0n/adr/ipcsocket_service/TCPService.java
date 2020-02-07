package e4s0n.adr.ipcsocket_service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPService extends Service {
    private boolean mIsServiceDestoryed = false;
    PrintWriter out = null;
    private static final String TAG = "TCPService";
    private Callback callback;

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new TCPServer()).start();
    }
    private MSGBinder msgBinder = new MSGBinder();

    class MSGBinder extends Binder{
        void SendToClient(String msg)
        {
            if (out != null)
            {
                out.println(msg);
            }
            else
                Log.e(TAG,"null out");
        }
        TCPService getService()
        {
            return TCPService.this;
        }

    }
    @Override
    public IBinder onBind(Intent intent) {
        return msgBinder;
    }
    private class TCPServer implements Runnable{

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try{
                serverSocket = new ServerSocket(8688);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!mIsServiceDestoryed){
                try{
                    final Socket client = serverSocket.accept();
                    callback.onClientConnected();
                    new Thread(){
                        @Override
                        public void run() {
                            try{
                                responseClient(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void responseClient(Socket client) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
            while (!mIsServiceDestoryed) {
                String instr = in.readLine();
                callback.onMesArrival(instr);
                if (instr == null)
                    break;
            }
        }
    }

    void setCallback(Callback callback)
    {
        this.callback = callback;
    }
    public interface Callback {
        void onMesArrival(String data);
        void onClientConnected();
    }
}
