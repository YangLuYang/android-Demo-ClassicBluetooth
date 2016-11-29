package com.simple.classicbluetooth;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    public BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> paireDevices;
    private BluetoothSocket socket = null;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;
    private int mState = 0;

    public static final UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private List<BluetoothDevice> list;
    private ListView listView;
    private ListViewAdapter adapter;
    private ProgressDialog dialog;

    private Context context = this;

    public static final int STATE_NONE = 0;       // 初始状态
    public static final int STATE_LISTEN = 1;     // 等待连接
    public static final int STATE_CONNECTING = 2; // 正在连接
    public static final int STATE_CONNECTED = 3;  // 已经连接上设备



    private IntentFilter filter;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!list.contains(device)){
                    adapter.addData(device);
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                dialog.dismiss();
                Toast.makeText(context,"扫描完毕",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final android.os.Handler mHandler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.MESSAGE_TOAST:
                    if(null!=context){
                        Toast.makeText(context,msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initAdapter();
        start();

        //注册
        filter = getIntentFilter();
        registerReceiver(receiver,filter);

    }

    private void initView() {
        Button scan = (Button)findViewById(R.id.btn_scan);
        scan.setOnClickListener(this);

        Button send = (Button)findViewById(R.id.btn_send);
        send.setOnClickListener(this);

        list = new ArrayList<BluetoothDevice>();
        listView = (ListView)findViewById(R.id.listView);
        adapter = new ListViewAdapter(MainActivity.this);
        adapter.setDataSource(list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    private void initAdapter() {
        //1、获取BluetoothAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //2、判断是否支持蓝牙，并打开蓝牙
        if(mBluetoothAdapter == null ||!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }
        checkBluetoothPermission();
        //将配过对的设备加入list
        paireDevices = mBluetoothAdapter.getBondedDevices();
        if(paireDevices.size()>0){
            for(BluetoothDevice device: paireDevices){
                adapter.addData(device);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            //启用
        }else if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED){
            //未启用
            Toast.makeText(MainActivity.this,"请启用蓝牙",Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /*
       校验蓝牙权限
      */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_ENABLE_BT);
            } else {
                //具有权限

            }
        } else {
            //系统不高于6.0直接执行

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //同意权限
                Toast.makeText(context,"获取蓝牙权限成功",Toast.LENGTH_LONG).show();

            } else {
                // 权限拒绝
                Toast.makeText(context,"没有蓝牙权限",Toast.LENGTH_LONG).show();
            }
        }
    }



    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        //获取MAC地址，用于连接
        String address = list.get(i).getAddress();
        Log.i(TAG,address);
        BluetoothDevice btDev = mBluetoothAdapter.getRemoteDevice(address);
        connect(btDev);
    }

    private void connect(BluetoothDevice device){
        setState(STATE_CONNECTING);
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    private IntentFilter getIntentFilter(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        return intentFilter;
    }

    @Override
    protected void onDestroy() {
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        //取消广播注册
        unregisterReceiver(receiver);
        stop();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_scan:
                //如果蓝牙功能被关闭则要求打开
                if(mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF||
                        mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
                }
                //开始扫描,在dialog中处理后退键事件，取消扫描
                mBluetoothAdapter.startDiscovery();
                dialog = new ProgressDialog(context);
                dialog.setMessage("正在扫描...");
                dialog.setCancelable(true);
                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        if(i == KeyEvent.KEYCODE_BACK){
                            Log.i(TAG,"Back down");
                            dialog.dismiss();
                            mBluetoothAdapter.cancelDiscovery();
                            Log.i(TAG,"Cancel Discovery");
                        }
                        return false;
                    }
                });
                dialog.show();
                break;
            case R.id.btn_send:
                this.write(new String("hello").getBytes());
                break;
        }
    }

    //用于蓝牙连接的线程
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;


        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                //尝试建立安全的连接
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i(TAG,"获取 BluetoothSocket失败");
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.i(TAG,"socket连接失败");
                setState(STATE_LISTEN);
                Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.TOAST,"Socket连接失败");
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }

            synchronized (MainActivity.this){
                mConnectThread = null;
            }
            //启动用于传输数据的线程connectedThread
            connected(mmSocket);
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //连接完成后启动ConnectedThread
    public synchronized void connected(BluetoothSocket socket){

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }


        setState(STATE_CONNECTED);
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

    }

    //蓝牙连接完成后进行输入输出流的绑定
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            while(mState == STATE_CONNECTED){
                try {
                    // Read from the InputStream
                    Scanner in = new Scanner(mmInStream,"UTF-8");
                    String str = in.nextLine();
                    Log.i(TAG,"read: "+str);
                    //利用handle传递数据
                    Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.TOAST,str);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    //用于接收连接请求
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("name",
                            MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);

            BluetoothSocket socket = null;

            // 在没有连接上的时候accept
            while (mState!=3) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (MainActivity.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // 准备通信
                                connected(socket);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");

        }

        public void cancel() {
            Log.d(TAG, "Socket cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed", e);
            }
        }
    }

    //发送字符串
    private void write(byte[] out){
        ConnectedThread r = null;
        try{
            r = mConnectedThread;
            r.write(out);
        }catch (NullPointerException e){
            Toast.makeText(context,"无法发送",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if(mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

    }

}
