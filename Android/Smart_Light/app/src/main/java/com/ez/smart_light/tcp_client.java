package com.ez.smart_light;

/**
 * Created by evan on 24.05.2019.
 */

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by evan on 05.10.2017.
 */

public class tcp_client {
    String tag= "TAG";
    String status_SERVER = "Client created!";
    static String message_Log = "";
    private String mServerMessage = "";
    static String mes;

    private BufferedReader mBufferIn;

    boolean mRun = false;
    static boolean status_socket = false;
    static boolean flag_send = false;

    static Socket socket;
    char[] buff = new char[1024];
    int size;
    connect_TCP_client tcp_NET;
    MainActivity mainobj;

    tcp_client(String SERV_IP, int Port_serv){
        tcp_NET = new connect_TCP_client(SERV_IP, Port_serv);
        tcp_NET.execute();
    }
    //////////////////////////////////////////////////////////
    class connect_TCP_client extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;
        connect_TCP_client(String addr, int port){
            dstAddress = addr;
            dstPort = port;
        }
        @Override
        protected Void doInBackground(Void... arg) {
            mRun = true;
            while (mRun) {      //пока флаг mRun=true крутимся в цикле
//////////////////////////соединяемся с сокетом.........
                status_SERVER = "подключаюсь..";
                try {
//                    socket = new Socket(dstAddress, dstPort);       //подключение к серверу
                    SocketAddress socketAddress = new InetSocketAddress(dstAddress, dstPort);
                    socket = new Socket();
                    socket.connect(socketAddress, 2000);  // подключение к серверу, 2сек timeout
                    status_socket = true;
                    Log.d(tag, "Connect OK!");
                    status_SERVER = "Соединение установлено";
                } catch (SocketException rnt) {
                    Log.d(tag, "Runtime: ");
                    status_socket = false;
                    if (isCancelled()) return null;
                } catch (UnknownHostException e) {
                    Log.d(tag, "UnknownHostException");
                    status_socket = false;
                    status_SERVER = "Невозможно подключиться к серверу.";
                } catch (IOException ee) {
                    Log.d(tag, "Connect FAIL!");
                    status_socket = false;
                    status_SERVER = "Невозможно подключиться к серверу..";
                } catch (Exception eex) {
                    status_socket = false;
                    Log.d(tag, "Exception eex");
                }
///////////////////////////////////////////////////////////////////
//////////////////слушаем сервер в случае успешного коннекта........
                if (status_socket) {
                    try {
                        mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Log.d(tag, "wait read...");
                        //in this while the client listens for the messages sent by the server
///////////////////////////////////////////////////////////////////////
                        size = mBufferIn.read(buff);        // ждем когда в буфере появятся данные и считываем колличество принятых символов и само сообщение в буфер buff
                        Log.d(tag, "данные приняты: " + size);
                        mainobj.size_bufIN = size;
                        Log.d(tag, "команда: " + buff[67]+" "+Integer.toString(buff[68])+" "+buff[69]+buff[70]);
 //                       Log.d(tag, "команда: " + Integer.toString(buff[67])+" "+Integer.toString(buff[68])+" "+Integer.toString(buff[69])+Integer.toString(buff[70]));
                        mServerMessage = "";
                        for (int i = 0; i < size; i++)
                            mServerMessage += buff[i];
                        mes = mServerMessage;
                        if(size!=-1){
                            mainobj.data_read = true;
                            //                           mainobj.pars_data(mes);
                        }

///////////////////////////////////////////////////////////////////////
                    } catch (Exception r) {
                        Log.d(tag, "ERR recieve!");
                        status_socket = false;
                    }
                    if (!flag_send && size != -1) {             // если сообщение было принято, отправляем ответ. Если получили подтверждение отправленого сообщения, то ничего не отправляем
                        //                       mes = "message_recieved: " + mes;        // -1 приходит в случае, если отвалился сервер
//                        send_m("message_OK");
                    }
                }
//                if(!status_socket)close_socket();
                close_socket();
                Log.d(tag, "end mRun cycle");
            }
            ////////////////////////////////////////////////////////
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            Log.d(tag, "onPostExecute");
            super.onPostExecute(result);
            status_SERVER = "Соединение закрыто";
        }
        @Override
        protected void onCancelled() {
            super.onCancelled();
            status_SERVER = "Соединение закрыто";
            Log.d(tag, "Cancel");
        }
    }

    //////////////////////////////////////////////////////////
    public void tcp_close(){
        if(mRun) {
            mRun = false;
            close_socket();
            //           try{socket.close();}catch(Exception e){Log.d(tag, "chanel : close");}
            Log.d(tag, "chanel ASYNCTASK: " + tcp_NET.isCancelled());
            tcp_NET.cancel(true);
            Log.d(tag, "chanel ASYNCTASK: " + tcp_NET.isCancelled());
        }
//        Log.d(tag, "status MRUN: " + mRun);
    }
    void send_m(String mmm){
        try {
            byte[] buf = new byte[1024];
            buf = mmm.getBytes();
            OutputStream sout = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(sout);
            dataOutputStream.write(buf);

            Log.d(tag, "message_sended: " + mmm);
            if(flag_send){
                mmm = "client: "+mmm+"\n";
                message_Log = mmm+message_Log;
            }
        }
        catch (IOException e){Log.d(tag, "message_not sended");}
        catch (Exception e){Log.d(tag, "Exception message_not sended");}
    }
    void close_socket(){
        if (socket != null) {
            try {
                socket.close(); Log.d(tag, "socket close");
            } catch (IOException e) {
                e.printStackTrace();  Log.d(tag, "socket close FAIL");
            }
        }
        status_socket = false;
        flag_send = false;
        Log.d(tag, "socket NULL");
    }
    void send_staus_socket(final String[] status){
        status[0] = status_SERVER;
        if(mainobj.data_read){status[1] = mes;}   //если принято сообщение, передаем его в MainActivity
        else{status[1] = "::";}
    }
/////////////////////////////////////////////////////////

}

