package e4s0n.adr.ipcsocket_service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    private List<Msg> msgList = new ArrayList<Msg>();
    private EditText inputText;
    private Button send;
    private RecyclerView msgRecyclerView;
    private  MsgAdapter adapter;
    private Handler handler = null;
    private TCPService.MSGBinder binder;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = (EditText)findViewById(R.id.input_text);
        send = (Button)findViewById(R.id.send);
        send.setEnabled(false);
        msgRecyclerView = (RecyclerView)findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if(!"".equals(content))
                {
                    binder.SendToClient(content);
                    Msg msg = new Msg(content,Msg.TYPE_SENT);
                    msgList.add(msg);
                    adapter.notifyItemInserted(msgList.size()-1);//当有新消息时刷新RecyclerView中的显示
                    msgRecyclerView.scrollToPosition(msgList.size()-1);//将RecyclerView定位到最后一行
                    inputText.setText("");// 清空输入框中的内容
                }
            }
        });
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_RECEIVE_NEW_MSG: {
                        msgList.add(new Msg((String)msg.obj, Msg.TYPE_RECEIVED));
                        adapter.notifyItemInserted(msgList.size()-1);//当有新消息时刷新RecyclerView中的显示
                        msgRecyclerView.scrollToPosition(msgList.size()-1);//将RecyclerView定位到最后一行
                        break;
                    }
                    case MESSAGE_SOCKET_CONNECTED: {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),"连接成功",Toast.LENGTH_SHORT).show();
                            }
                        });
                        send.setEnabled(true);
                        break;
                    }
                    default:
                        break;
                }
            }
        };
        Intent intent = new Intent(this,TCPService.class);
        bindService(intent,this,BIND_AUTO_CREATE);

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (TCPService.MSGBinder)service;
        binder.getService().setCallback(new TCPService.Callback() {
            @Override
            public void onMesArrival(String data) {
                handler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,data).sendToTarget();
            }

            @Override
            public void onClientConnected() {
                handler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
            }

        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }
}
