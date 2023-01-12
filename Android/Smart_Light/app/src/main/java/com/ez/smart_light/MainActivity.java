package com.ez.smart_light;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

///////////////////////


import static android.os.AsyncTask.Status.FINISHED;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{
    private ImageView prg_img;
    private ImageView prg_imgsun;
    private View item_img = null;
    private TextView txt_light;
    private TextView txt_percent;
    private TextView room_name;
    private TextView status_net;
    private TextView name_serv;
    private LinearLayout ml;
    private LinearLayout ml_bar;
    private ImageView menu;
    static ProgressBar progressBar;

    boolean touchFlag = false;
    static boolean data_read = false;
    boolean connect_server = false;
    boolean mRun = true;
    static boolean thr_start = false;
    boolean initESP_ON = false;
    static boolean flag_dialog = false;
    static boolean wait_ENDSENDTCP = false;
    boolean new_link_create = true;

    String tag = "TAG";
    String netSSID_cur = "";
    String [] str_tmp = new String[2];
    static String status_SERV = "status_SERVER";
    private String mServerMessage = "";
    String port, SERVER_IP;
    String cur_data_cl;
    String name_SSID = "";
    String pass_SSID = "";
    String stat_info_prog = "";
    String resiev_IP = "";
    String str_mess = "";

    static char[] bufTCPout = new char[192];
    static char[] bufTCPin = new char[192];

    int eX, eY, poseY, width, hight, step;
    int alpha_init = 170;
    static int count_wait_answer = 0;       // счетчик ожидания ответа от сервера
    int port_int;
    static int count_req = 1;
    static int scalegr;
    int count_start_req = 0;
    int count_lost_connect = 0;
    static int cmd_send;
    static int SECOND_;
    int step_progr = 0;
    static int size_bufIN = 0;
    int cnt_0_25_sec = 0;
    /////////////////////////////////////////////////
    final int req_data = 0x34;              //запрос телеметрии                                 ////десятич.с. 52
    final int synchro = 0x64;               // синхронизировать время                           ////десятич.с. 100
    static int set_link	= 0x11;             // применить настройки сети                         ////десятич.с. 17
    static int mode_light = 0x35;           // применить настройки яркости                      ////десятич.с. 53
    static int cmd_none = 0x12;             // пустая команда, ничего не отправлять             ////десятич.с. 18
    final int net_NOT_found = 0;
    final int net_dimmer_NOT_found = 1;
    final int wait_scan_net = 2;
    char mode_serv = '1';
/////////////////////////////////////////////////

    static float scale_X;
    static float scale_Y;

    List<String> name_adr;
    int num_save_ip;
    int select_pos;
    int new_select_pos;
    boolean new_IP_found;

    SharedPreferences sPref;
    tcp_client clientTCP;
    Timer timer;
    TimerTask mTimerTask;
    Timer timer2;
    TimerTask timereq2;
    final Context cntx = this;
    Telemetry tele;
    BroadcastReceiver wifi_BroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //////////////////////////////////
        ml = (LinearLayout) findViewById(R.id.ml);
        ml_bar = (LinearLayout) findViewById(R.id.ml_bar);
        txt_light = (TextView)findViewById(R.id.txt_light);
        txt_percent = (TextView) findViewById(R.id.txt_percent);
        room_name = (TextView) findViewById(R.id.room_name);
        status_net = (TextView) findViewById(R.id.status_net);
        name_serv = (TextView) findViewById(R.id.name_serv);
        prg_img = (ImageView) findViewById(R.id.progress_img);
        prg_imgsun = (ImageView) findViewById(R.id.Sun_y);
        menu = (ImageView)findViewById(R.id.menu);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        menu.setOnTouchListener(this);
        prg_img.setOnTouchListener(this);
        room_name.setOnLongClickListener(click_room_name);
        name_serv.setOnLongClickListener(click_name_serv);
        //////////////////////////////////
        // получение ширины текущего разрешения
        WindowManager w = getWindowManager();// объект менеджер окна
        Display d = w.getDefaultDisplay();
        width = d.getWidth();
        hight = d.getHeight();
        step = width/100;                    // вычисление размера 1 процента в пикселях, всего 100(от 5 до 100)
        double tmp_X, tmp_Y;
        tmp_X = (double)width;
        tmp_Y = (double)hight;
        scale_X = (float)(tmp_X/1080);
        scale_Y = (float)(tmp_Y/1920);
        Log.d(tag, "width " + width);
        Log.d(tag, "hight " + hight);
        ////////////////////////////////////
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  // убирание системной строки с экрана
        ml.setBackground(createLayerDrawable(R.drawable.dimmer_fon, 1, 1)); /// установка бэкграунда нужного разрешения
        ml_bar.setBackground(createLayerDrawable(R.drawable.bar_m, 1, (float)0.072)); /// установка бэкграунда нужного разрешения ( по горизонтали вместо 1 было 0,12, размер бэкграунда сохранялся при этом, но картинка сильно искажалась)
        //       ml_bar.setBackgroundResource(R.drawable.bar_m); /// установка бэкграунда нужного разрешения (слабые девайсы при этом могут тормозить)
        menu.setImageDrawable(createLayerDrawable(R.drawable.menu_pic, (float)0.12, (float)0.072));
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  // убирание системной строки с экрана
        txt_percent.setTextSize(TypedValue.COMPLEX_UNIT_PX, 80*scale_Y);            // размер текста в пикселях
        txt_light.setTextSize(TypedValue.COMPLEX_UNIT_PX, 80*scale_Y);            // размер текста в пикселях
        room_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 80*scale_Y);            // размер текста в пикселях
        status_net.setTextSize(TypedValue.COMPLEX_UNIT_PX, 56*scale_Y);            // размер текста в пикселях
        name_serv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 56*scale_Y);            // размер текста в пикселях
        ////////////////////////////////////
        prg_img.setImageAlpha(alpha_init);
        prg_imgsun.setImageAlpha(255 - alpha_init);
        txt_percent.setText(" "+Integer.toString(100 - (int)(((float)alpha_init/255)*100))+"%");

        set_pos_but(room_name, 90*scale_X, 1*scale_Y);
        set_pos_but(status_net, 90*scale_X, 100*scale_Y);
        set_pos_but(progressBar, 800*scale_X, 100*scale_Y);
        set_pos_but(name_serv, 90*scale_X, 200*scale_Y);
        ///////////////////////////////////debug
//        String key = "12345678";
//        Log.d(tag, String.format("\"%s\"", key));   // форматирование дает значение в кавычках
        /////////////////////////////////////
        ///////////////////////////инициализация подключения TCP ip
        SERVER_IP = read_config_str("server_IP");
        port = read_config_str("server_port");
        select_pos = read_config_int("select_pos");
        num_save_ip = read_config_int("num_save_ip");           // читаем кол-во сохраненных IP
        Log.d(tag, " num_save_ip "+num_save_ip +" SERVER_IP "+SERVER_IP);   //
        if(select_pos == 0)select_pos = 1;
        if(SERVER_IP.equals("")){SERVER_IP = "192.168.4.1";}
        port = "8888";

//        SERVER_IP = "192.168.4.1";  ///DEBUG
        port_int = Integer.parseInt(port);
        clientTCP = new tcp_client(SERVER_IP, port_int);
//        IP_adr.setText("IP server "+SERVER_IP+" port "+ port_int);
        count_req = read_config_int("num_req");
        if(read_config_str("new_name_obj"+select_pos).equals("")){ room_name.setText("Объект "+select_pos);}
        else{ room_name.setText(read_config_str("new_name_obj"+select_pos)); }
        Log.d(tag, " room_name "+room_name);   //
        cur_data_cl = cur_data();
        ///////////////////Для получения инфо о подключенной сети WIFI
        wifi_BroadcastReceiver = new WIFI_BroadcastReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION); //.NETWORK_IDS_CHANGED_ACTION);   //.NETWORK_STATE_CHANGED_ACTION
        this.registerReceiver(wifi_BroadcastReceiver, filter);
        //////////////////////////////////////////
        /////////////////////////////////////
        // обработка события касания экрана ..........
        View root = findViewById(android.R.id.content).getRootView();
        root.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (touchFlag) {
                    System.err.println("Display If  Part ::->" + touchFlag);

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            Log.d(tag, "onTouch DOWN oncreate");
                            eY = (int) event.getY();
                            poseY = eY;
                            if(item_img.getId() == R.id.menu){
                                Log.d(tag, "PUSH menu!!!");
                                showPopupMenu(item_img);                /// вывод меню
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            eX = (int) event.getX();
                            eY = (int) event.getY();
                            if(item_img.getId() != R.id.menu)showLight.sendEmptyMessage(0);
                            //func_req_data(mode_light);
                            if(item_img.getId() != R.id.menu)cmd_send = mode_light;
                            break;
                        case MotionEvent.ACTION_UP:
                            Log.d(tag, "onTouch UP oncreate");
                            if(item_img.getId() != R.id.menu) {
                                //   func_req_data(mode_light);
                                //while(wait_ENDSENDTCP)func_req_data(mode_light);
                                cmd_send = mode_light; wait_ENDSENDTCP = true;
                                Log.d(tag, "room_name.getX() " + room_name.getX()+" room_name.getY() "+room_name.getY());
                            }
                            touchFlag = false;
                            break;
                    }
                }
                return true;
            }
        });
        ///////////////////////////////////////////
 //       get_name_ssid();                // получаем имя сети
//////////////////////////////////таймер, работает с TCP соединением
        timer = new Timer();
        mTimerTask = new MyTimerTask();
        try{timer.schedule(mTimerTask, 250, 250);}catch(Exception c){;}
        ///////////////////////////////////////////////
        timer2 = new Timer();
        timereq2 = new TimerTask2();
        if(read_config_int("saved_show_help") ==0 ){try{timer2.schedule(timereq2, 2000);}catch(Exception cx){;} } // одноразовый запуск таймера через 2сек
        ////////////////////////////////////////////////
        //command = req_data_serv;
        cmd_send = req_data;            // установка команды на запрос телеметрии
        for(int a = 0; a< bufTCPout.length; a++){ bufTCPout[a] = '_'; }
        cmd_send = synchro;
        name_serv.setText("Сервер "+SERVER_IP);
        //////////////////////////////////////////

    }
    //////////////////////////////////////////
//////////////////////////////END ONCREATE
//////////////////////////////////////////
    public class WIFI_BroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            NetworkInfo nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            nwInfo.getState();

            WifiManager wifiManager = (WifiManager) context.getSystemService (Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();

            List<WifiConfiguration> listOfConfigurations = wifiManager.getConfiguredNetworks();
            for (int index = 0; index < listOfConfigurations.size(); index++) {
                WifiConfiguration configuration = listOfConfigurations.get(index);
                if (configuration.networkId == info.getNetworkId()) {
                    netSSID_cur = configuration.SSID;
                    netSSID_cur = netSSID_cur.replace("\"", "");
                    //return configuration.SSID;
                    Log.d(tag, ", ssid "+netSSID_cur);
                    //return ssid;
                }
            }
        }

    }

    //////////////////////////////////////////
    View.OnLongClickListener click_room_name = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            dialog_show(6);
            return false;
        }
    };
    //////////////////////////////////////////
    View.OnLongClickListener click_name_serv = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            dialog_show(5);
            return false;
        }
    };

    //////////////////////////////////////////////////////
    /* работает только до андр.7 версии
    String get_name_ssid(){
        String name = "";
        netSSID_cur = getCurrentSsid(this);                                     // получаем имя сети
        try {
            char[] tmp_char = new char[netSSID_cur.length() - 2];                   // объявляем массив символов длиной netSSID_cur.length()-2
            netSSID_cur.getChars(1, netSSID_cur.length() - 1, tmp_char, 0);           // копируем имя сети в массив символов исключая 1 и последний символы(кавычки)
            String tmp_str = new String(tmp_char);                                  // инициализируем новую строку элементами массива символов
            netSSID_cur = tmp_str;
            name = netSSID_cur;
            Log.d(tag, "name SSID " + netSSID_cur);
        }catch(Exception tt){  Log.d(tag, "NOt connection WIFI! "); }
        return name;
    }
    private String getCurrentSsid(Context context) {
        String ssid = null;
        int netID;
        try {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    ssid = connectionInfo.getSSID();  netID = connectionInfo.getNetworkId();
                    Log.d(tag, "netID = "+ netID);
                }
            }
        }catch(Exception e){ Log.d(tag, "Exception getCurrentSsid!"); }
        return ssid;
    }
    */
    //////////////////////////////////////////
    private Drawable createLayerDrawable(int ID_drw, float x, float y) {     //получаем объект Drawable из ресурсов (id = "ID_drw") нужной ширины "x"  и высоты "y"
        float xx = (float)width*x;
        float yy = (float)hight*y;
        Bitmap bitm = BitmapFactory.decodeResource(getResources(), ID_drw);
        Bitmap btm = bitm.createScaledBitmap(bitm, (int)xx, (int)yy, true);
        BitmapDrawable drawable0 = new BitmapDrawable(getResources(), btm);
//    BitmapDrawable drawable0 = (BitmapDrawable) getResources().getDrawable(
//            R.drawable.bg_main1920);
        Log.d(tag, "widht "+btm.getWidth()+" hight "+btm.getHeight());

        return drawable0;
    }
    //////////////////////////////////////////
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchFlag = true;
                item_img = v;
                Log.d(tag, "onTouch DOWN");
                break;
            case MotionEvent.ACTION_UP:
                touchFlag = false;
                Log.d(tag, "onTouch UP");
                break;
            default:
                break;
        }
        return false;
    }
    //////////////////////////////////////////////////////////
    Handler showLight = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int alpha;
            if((eY - poseY)<100&&(eY - poseY)>-100) {          // от точки прикосновения вверх и вниз будет работать только на 100 пикселей
                scalegr = eX / step;
                if(scalegr>100)scalegr = 100;
                alpha = 3 * scalegr;
                alpha = 300-alpha; if(alpha>255)alpha = 255;
                prg_img.setImageAlpha(alpha);
                prg_imgsun.setImageAlpha(255 - alpha);
                Log.d(tag, "scalegr = " + scalegr);
                txt_percent.setText(" "+scalegr+"%");
            }
//            try { Thread.sleep(70); } catch (Exception ex_cr) { Log.d(tag, "Exception ex_cr"); }
        }
    };
    //////////////////////////////////////////////////////////
    /////////////////////сохранение и чтение сетевых настроек.......
    void saved_config(String str, int par){
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt(str, par);
        ed.commit();
    }
    void saved_config(String str, String par){
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(str, par);
        //Log.d(tag, "saved_config_func");
        ed.commit();
    }
    String read_config_str(String str) {
        String tmp = "";
        sPref = getPreferences(MODE_PRIVATE);
        tmp = sPref.getString(str, "");
        return tmp;
    }
    int read_config_int(String str){
        int tmp = 0;
        sPref = getPreferences(MODE_PRIVATE);
        tmp = sPref.getInt(str, 0);
        return tmp;
    }
    ////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu); // Для Android 4.0
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.menu_1:
                        dialog_show(1);
                        break;
                    case R.id.menu_2:
                        dialog_show(2);
                        break;
                    case R.id.menu_3:
                        dialog_show(3);
                        break;
                    case R.id.menu_4:
                        dialog_show(4);
                        break;
                    case R.id.menu_5:
                        close_TCP();
                        finish();
                        break;
                    case R.id.menu_6:
                        dialog_show(7);   // сброс настроек по умолчанию
                        break;
                    case R.id.menu_7:
                        Intent intent;
                        String[] TO = {"evan77@ro.ru"};
                        //              String[] CC = {"evan77@bk.ru"};

                        intent = new Intent(Intent.ACTION_SEND);
                        intent.setData(Uri.parse("mailto:"));
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_EMAIL, TO);
                        //              intent.putExtra(Intent.EXTRA_CC, CC);
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Smart Light, обратная связь");
                        intent.putExtra(Intent.EXTRA_TEXT, "Вопросы по работе приложения и модуля:\n\n");
                        try {
                            startActivity(Intent.createChooser(intent, "Отправка письма..."));
//                            finish();
                            Log.d(tag, "Finished sending email...");
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(MainActivity.this, "There is no email client installed", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return true;
            }
        });
        MenuPopupHelper menuHelper = new MenuPopupHelper(this, (MenuBuilder) popupMenu.getMenu(), v);
        menuHelper.setForceShowIcon(true);
        menuHelper.setGravity(Gravity.END); menuHelper.show();

    }
    /////////////////////////////////////////////////
//////////////////////////////////////////////////////////отключение-подключение к WIFI сети
    void off_wifi(Context context){
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiConfiguration wc = new WifiConfiguration();
            boolean b=wifi.isWifiEnabled();
            if(b){
                wifi.setWifiEnabled(false);
                Toast.makeText(context,"yes", Toast.LENGTH_SHORT).show(); //wifi.enableNetwork(1, true); getCurrentSsid(cntx);
                ////////////////////////////////////////debug
                ///////////////////////////////////////
            }else {

                wifi.setWifiEnabled(true);
                Toast.makeText(context, "no", Toast.LENGTH_SHORT).show();
            } //Log.d("WifiPreference", "enableNetwork returned " + b );
        } catch (Exception e) { e.printStackTrace(); }
    }
    void wifi_reconnect_net(Context context, String ssid, String key){

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";
//        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        if(key.equals("open")){;}
///        else { conf.preSharedKey = String.format("\"%s\"", key);}
        else { conf.preSharedKey = "\"" + key + "\"";}
        Log.d(tag, "conf. "+conf.SSID + "  "+conf.preSharedKey);
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                                                                        // вариант подключения к нужной сети, но почему-то нет подкл. к серверу
        wifiManager.addNetwork(conf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
        /*
        int netId = wifiManager.addNetwork(conf);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
*/
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    void show_txt_ex(){
        Toast.makeText(this,"Введены не все данные!", Toast.LENGTH_SHORT).show();
    }
    /////////////////////////////////////////////////
    boolean getIPfor_client(String str){ // возвращает true при корректном IP и заполняет массив bufTCPout символами адреса
        //////////////////////////////
        boolean ok = false;
        char [] ipserv = new char[str.length()+1];
        char [] tmp_ip = new char[4];
        try {
            str.getChars(0, str.length(), ipserv, 0);
            ipserv[str.length()] = '.';
            for (int i = 0, t = 0, j = 0; i < str.length()+1; i++) {
                tmp_ip[t] = ipserv[i];
                if (ipserv[i] != '.') {
                    t++;
                } else {
                    switch (t) {
                        case 1:
                            bufTCPout[19 + j] = '0';
                            bufTCPout[19 + 1 + j] = '0';
                            bufTCPout[19 + 2 + j] = tmp_ip[0];
                            break;
                        case 2:
                            bufTCPout[19 + j] = '0';
                            bufTCPout[19 + 1 + j] = tmp_ip[0];
                            bufTCPout[19 + 2 + j] = tmp_ip[1];
                            break;
                        case 3:
                            bufTCPout[19 + j] = tmp_ip[0];
                            bufTCPout[19 + 1 + j] = tmp_ip[1];
                            bufTCPout[19 + 2 + j] = tmp_ip[2];
                            break;
                    }
                    t = 0;
                    j += 3;
                }
            }
            Log.d(tag, "ip_serv " + bufTCPout[19] + bufTCPout[20] + bufTCPout[21] + bufTCPout[22] + bufTCPout[23] + bufTCPout[24] + bufTCPout[25] + bufTCPout[26] + bufTCPout[27] + bufTCPout[28] + bufTCPout[29] + bufTCPout[30]);
            ok = true;
            //////////////////////////////
        }catch(Exception t){ Log.d(tag, "Exception getIPfor_client");  ok = false; }
        return ok;
    }
    /////////////////////////////////////////////////
    String gotIP(String str){ // преобразует IP формата 192.168.004.001 к формату 192.168.4.1
        //////////////////////////////
        String strIP = "";
        int j = 0;
        char [] ipserv = new char[str.length()+1];
        char [] tmp_ip = new char[4];
        try {
            str.getChars(0, str.length(), ipserv, 0);
            ipserv[str.length()] = '.';
            for (int i = 0, t = 0; i < str.length()+1; i++) {
                tmp_ip[t] = ipserv[i]; t++;
                if (ipserv[i] == '.') {
                    if(tmp_ip[0]=='0'){ j = 1; }
                    if(tmp_ip[1]=='0' && j == 1) { j = 2; }
                    if(tmp_ip[2]=='0' && j == 2) { j = 3; }
                    switch(j){
                        case 0:
                            strIP = strIP +tmp_ip[0]+tmp_ip[1]+ tmp_ip[2];
                            break;
                        case 1:
                            strIP = strIP + tmp_ip[1]+ tmp_ip[2];
                            break;
                        case 2:
                            strIP = strIP +tmp_ip[2];
                            break;
                        case 3:
                            strIP = strIP+'0';
                            break;
                    }
                    if(i !=15)strIP =strIP + '.';
                    //               strIP =strIP + tmp_ip[0]+tmp_ip[1]+ tmp_ip[2];
                    t = 0; j =0;
                }
            }
//        Log.d(tag, " strIP "+strIP);

            //////////////////////////////
        }catch(Exception t){ Log.d(tag, "Exception gotIPfor");  }
        return strIP;
    }
    // Изменение высоты ListView в зависимости от количества элементов, чтобы вместить в ScrollView
// в параметрах передаём listView для определения высоты
    public void setListViewHeightBasedOnChildren(ListView listView) {
        ArrayAdapter listAdapter = (ArrayAdapter) listView.getAdapter();

        int totalHeight = 0;
        // проходимся по элементам коллекции
        for(int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            // получаем высоту
            totalHeight += listItem.getMeasuredHeight();
        }

        // устанавливаем новую высоту для контейнера
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
    ///////////
    /////////////////////////////////////////////////resiev_IP
    void dialog_show(int dialog) {
        flag_dialog = true;
        final AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (dialog) {
            case 1:                                                                         /// настройки подключения к сети
                LinearLayout dev_conf;
                dev_conf = (LinearLayout) getLayoutInflater().inflate(R.layout.device_config, null);
                final EditText login_ssid = dev_conf.findViewById(R.id.config_SSID);
                final EditText pass_ssid = dev_conf.findViewById(R.id.config_pasw);
                final Button apply_conf = dev_conf.findViewById(R.id.button2);
//                final Button apply_serv = dev_conf.findViewById(R.id.but_serv);
//            final Button apply_client = dev_conf.findViewById(R.id.but_client);
//                final Button apply_def = dev_conf.findViewById(R.id.but_def);
                final ProgressBar progrbar = dev_conf.findViewById(R.id.progrBar);
                final TextView info_prog = dev_conf.findViewById(R.id.info_prog);
                builder.setView(dev_conf);
                alert = builder.create();
                alert.show();
 //               if(new_link_create){ show_txt_toast("Перезагрузите приложение!");}
                cmd_send = cmd_none;
                if(SERVER_IP.equals("192.168.4.1")){ ; }
                else{ close_TCP(); SERVER_IP = "192.168.4.1"; name_serv.setText("IP server "+SERVER_IP); }

 //               apply_serv.setTextColor(Color.RED);
//            apply_client.setTextColor(getResources().getColor(R.color.GREY));

                //////////////////////////////////
                final Handler info_NET = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        String mes = "";
                        switch(msg.what){
                            case net_NOT_found:
                                mes = "Нет подключения к сети WIFI!";
                                break;
                            case net_dimmer_NOT_found:
                                //    mes = "Сеть \"Dimmer_EZ\" не найдена!";
                                mes = "Сеть \"Smart_Home_EZ\" не найдена!";
                                break;
                            case wait_scan_net:
                                mes = "Секундочку...";
                                break;
                        }
                        show_txt_toast(mes);
                    }
                };
                //////////////////////////////////
 //               wifi_reconnect_net(this, "Smart_Home_EZ", "12345678");
 //               get_name_ssid();

                if(netSSID_cur != null ){                              // проверяем, если подключение WIFI
                    login_ssid.setText(netSSID_cur);
                    //               screen_mes_wait.sendEmptyMessage(0);
                    info_NET.sendEmptyMessage(wait_scan_net);
                    // if(netSSID_cur.equals("Dimmer_EZ")){ ; }            // проверяем, если подключение к сети Dimmer_EZ. Если да, то ничего не делаем
                    if(netSSID_cur.equals("Smart_Home_EZ")){ ; }            // проверяем, если подключение к сети Smart_Home_EZ. Если да, то ничего не делаем
                    else {                                              // подключения к сети нет
                        login_ssid.setText(netSSID_cur);
                        //wifi_reconnect_net(this, "Dimmer_EZ", "12345678");
                        wifi_reconnect_net(this, "Smart_Home_EZ", "12345678");
//                        try { Thread.sleep(1500); } catch (Exception ex_cr) { Log.d(tag, "Exception ex_cr"); }  // попытка повторного подключения и пауза 1,5сек
                        //////////////////////////
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String sr = "";
                                boolean sr_OK = false;

                                cnt_0_25_sec = 0;
                                while(!netSSID_cur.equals("Smart_Home_EZ")){
                                    if(cnt_0_25_sec>1200){                                          //// 5 мин на попытку подключения
                                        SERVER_IP = read_config_str("server_IP");
                                        info_NET.sendEmptyMessage(net_dimmer_NOT_found);
                                        alert.dismiss();
                                        break;
                                    }
                                    if(cnt_0_25_sec%40 == 0){                                       /// раз в 10 сек попытка переподключения
                                        if(!sr_OK) {
                                            wifi_reconnect_net(cntx, "Smart_Home_EZ", "12345678");
                                            Log.d(tag, "reconnect..." + netSSID_cur);
                                          //  info_NET.sendEmptyMessage(wait_scan_net);
                                            sr_OK = true;
                                        }
                                    }else{ sr_OK = false;}
                                }
                                Log.d(tag, "reconnect...cnt_0_25_sec ="+cnt_0_25_sec);

                            }
                        }).start();
                        //////////////////////////
                    }
                }
                else{ info_NET.sendEmptyMessage(net_NOT_found); alert.dismiss(); }

                final Handler handlinfo_startESP = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        info_prog.setText(stat_info_prog);
                    }
                };
                View.OnClickListener apply_config_handle = new View.OnClickListener() {           // отработка пункта меню применить настройки
                    public void onClick(View v) {
                        /////////////////////////////////////
                        try {
                            if(mode_serv != '3') {                              // если не выбран режим AP
                                name_SSID = login_ssid.getText().toString();
                                pass_SSID = pass_ssid.getText().toString();
                            }else{
                                //name_SSID = "Dimmer_EZ";
                                name_SSID = "Smart_Home_EZ";
                                pass_SSID = "12345678";
//                            mode_serv = '1';
                                getIPfor_client("192.168.4.1");
                            }
                        }catch(Exception e){
                            show_txt_ex();
                        }
                        final Handler handlinfo_startESP = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                info_prog.setText(stat_info_prog);
                            }
                        };
                        /////////////////////////////////////
                        if( name_SSID.length() != 0 && pass_SSID.length() == 8 ) {                      // пароль должен быть 8 символов
                            if(!connect_server || !netSSID_cur.equals("Smart_Home_EZ")){ show_txt_toast("Подождите подключения устройства!"); }
                            else {
                                ////////////////////////////////////
                                if (!thr_start) {                            //чтобы не плодить потоки
                                    initESP_ON = true;                       // при получении ответа о успешном сохранении данных, этот бит нужно сбросить
                                    progrbar.setVisibility(View.VISIBLE);
                                    name_SSID.getChars(0, name_SSID.length(), bufTCPout, 35);
                                    pass_SSID.getChars(0, pass_SSID.length(), bufTCPout, 52);
                                    bufTCPout[51] = (char) name_SSID.length();
                                    bufTCPout[61] = mode_serv;
                                    //                     getIPfor_client();
                                    ////////////////////////////////
                                    cmd_send = set_link;
                                    count_req++;
                                    Log.d(tag, "set link send command!");
                                    ////////////////////////////////
                                    Log.d(tag, "lenght SSID = " + (int) bufTCPout[51] + " len = " + name_SSID.length());

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            thr_start = true;
                                            int step_progr = 0;
                                            cnt_0_25_sec = 0;
                                            new_link_create = false;
                                            boolean cr_OK = false;
                                            try {
                                                while (initESP_ON) {                                            // в потоке, ожидаем получение подтверждения принятой МК команды set_link, после подтверждения будет сброшен бит initESP_ON

                                                    //Thread.sleep(400);
                                                    if (cnt_0_25_sec % 4 == 0 && !cr_OK) {
                                                        cr_OK = true;
                                                        if (step_progr == 1) {
                                                            stat_info_prog = "Обновление" + "\n" + "параметров.";
                                                        }
                                                        if (step_progr == 2) {
                                                            stat_info_prog = "Обновление" + "\n" + "параметров..";
                                                        }
                                                        if (step_progr == 3) {
                                                            stat_info_prog = "Обновление" + "\n" + "параметров...";
                                                        }
                                                        if (step_progr == 4) {
                                                            stat_info_prog = "Обновление" + "\n" + "параметров....";
                                                        }
                                                        if (step_progr == 5) {
                                                            stat_info_prog = "Обновление" + "\n" + "параметров.....";
                                                        }
                                                        if (step_progr == 6) {
                                                            stat_info_prog = "Обновление" + "\n" + "параметров......";
                                                        }
                                                        step_progr++;
                                                        handlinfo_startESP.sendEmptyMessage(0);
                                                        if (step_progr > 6) {
                                                            step_progr = 0;
                                                        }
                                                        //////////////////////////////////////////////
                                                        //get_name_ssid();
                                                        if (netSSID_cur != null) {
                                                            //if(netSSID_cur.equals("Dimmer_EZ")){ ; }            // проверяем, если подключение к сети Dimmer_EZ.
                                                            if (netSSID_cur.equals("Smart_Home_EZ")) {
                                                                ;
                                                            }            // проверяем, если подключение к сети Smart_home_EZ.
                                                            else {                                              // подключения к сети нет
                                                                //wifi_reconnect_net(cntx, "Dimmer_EZ", "12345678");}
                                                                if (cnt_0_25_sec % 40 == 0)
                                                                    wifi_reconnect_net(cntx, "Smart_Home_EZ", "12345678");  ///раз в 10 сек попытка переподключния
                                                            }
                                                        }
                                                    } else {
                                                        cr_OK = false;
                                                    }
                                                    //////////////////////////////////////////////
                                                }
                                                if (mode_serv != '3') {
                                                    saved_config("server_IP", gotIP(SERVER_IP));
                                                } else {
                                                    saved_config("server_IP", "192.168.4.1");
                                                }
                                            } catch (Exception ee) {
                                                Log.d(tag, "Exception sleep");
                                            }
                                            //////////////////////////////////////////////////////////
                                            thr_start = false;
                                            alert.dismiss();
                                            //////////////////////////////////////////////////////////
                                        }
                                    }).start();

                                }
                            }
                        }
                        else {
                            show_txt_toast("Данные введены не корректно!");
//                            off_wifi(cntx);
                        }
                        ////////////////////////////////////
/*                    initESP_ON = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                    while (initESP_ON) {                                            // в потоке, ожидаем получение подтверждения принятой МК команды set_link, после подтверждения будет сброшен бит initESP_ON
                        try { Thread.sleep(400); } catch (Exception ex_cr) { Log.d(tag, "Exception sleep 400"); }
                        if (step_progr == 1) {
                            stat_info_prog = "Обновление"+ "\n"+"параметров.";
                        }
                        if (step_progr == 2) {
                            stat_info_prog = "Обновление"+ "\n"+"параметров..";
                        }
                        if (step_progr == 3) {
                            stat_info_prog = "Обновление"+ "\n"+"параметров...";
                        }
                        if (step_progr == 4) {
                            stat_info_prog = "Обновление"+ "\n"+"параметров....";
                        }
                        if (step_progr == 5) {
                            stat_info_prog = "Обновление"+ "\n"+"параметров.....";
                        }
                        if (step_progr == 6) {
                            stat_info_prog = "Обновление"+ "\n"+"параметров......";
                        }
                        step_progr++;
                        handlinfo_startESP.sendEmptyMessage(0);
                        if (step_progr > 6) { step_progr = 0; initESP_ON = false;
                            alert.dismiss();
                        }
                        Log.d(tag,"step_progr = "+step_progr);
                    }
                        }
                    }).start();*/
                        /////////////////////////////////////
                    }

                };
                apply_conf.setOnClickListener(apply_config_handle);

                break;
            case 2:                                                                     // устновка яркости с клавиатуры
                LinearLayout set_bright;
                set_bright = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView v_head_br = set_bright.findViewById(R.id.Head_v);
                final TextView v_txt_br = set_bright.findViewById(R.id.txt_v);
                final EditText new_br = new EditText(MainActivity.this);
                final Button newOK = new Button(this);
                final Button newNO = new Button(this);
                LinearLayout new_lay = new LinearLayout(this);
                new_lay.setOrientation(LinearLayout.HORIZONTAL);
                new_lay.addView(newNO); newNO.setText("Отмена");
                new_lay.addView(newOK); newOK.setText("Применить");
                new_br.setHint(Integer.toString(scalegr));
                set_bright.addView(new_br); set_bright.addView(new_lay);
                new_br.setInputType(InputType.TYPE_CLASS_NUMBER); // цифровая клавиатура, вместо энтер на клаве будет крыжик применить
                LinearLayout.LayoutParams new_lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
                new_lp.weight = 1;
                newNO.setLayoutParams(new_lp);
                newOK.setLayoutParams(new_lp);

                v_head_br.setText("Smart Light");
                v_txt_br.setText("Новое значение яркости");         //

                builder.setView(set_bright);
                alert = builder.create();
                alert.show();
                View.OnClickListener apply_yes = new View.OnClickListener() {           //
                    public void onClick(View v) {
                        if(new_br.getText().toString().equals("")){
                            show_txt_toast( "Вы не указали новое значение яркости!" );
                        }else{
                            int Bright = Integer.parseInt(new_br.getText().toString());
                            eY = 100; poseY = 50; eX = Bright * step;
                            showLight.sendEmptyMessage(0);
                            cmd_send = mode_light; wait_ENDSENDTCP = true;
                        }

                        alert.dismiss();
                    }
                };
                newOK.setOnClickListener(apply_yes);
                View.OnClickListener apply_no = new View.OnClickListener() {           //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                newNO.setOnClickListener(apply_no);
                break;
            case 3:                                                                 // о приложении
                LinearLayout about_app;
                about_app = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView v_head = about_app.findViewById(R.id.Head_v);
                final TextView v_txt = about_app.findViewById(R.id.txt_v);

                v_head.setText("Smart Light");
                v_txt.setText(R.string.about);

                builder.setView(about_app);
                alert = builder.create();
                alert.show();
            /*
            builder.setTitle("Smart Dimmer").setMessage(R.string.about);
            alert = builder.create();
            alert.show();
            */
                break;
            case 4:                                                                     // вывод справки
                LinearLayout help_app;
                help_app = (LinearLayout) getLayoutInflater().inflate(R.layout.help, null);
                final Button butok = help_app.findViewById(R.id.butOK);
                builder.setView(help_app);
                alert = builder.create();
                alert.show();
                View.OnClickListener ok = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                butok.setOnClickListener(ok);
                break;
            case 5:                                                             // установка нового IP, включение IP из списка
                final EditText new_ip = new EditText(MainActivity.this);
                final Button newIPbutOK = new Button(this);
                final Button newIPbutNO = new Button(this);
                ScrollView scroll_v = new ScrollView(this);
                LinearLayout l_scrl = new LinearLayout(this);
                l_scrl.setOrientation(LinearLayout.VERTICAL);
                newIPbutOK.setText("Применить");
                newIPbutNO.setText("Отмена");
                new_ip.setInputType(InputType.TYPE_CLASS_TEXT); // вместо энтер на клаве будет крыжик применить
                LinearLayout ll=new LinearLayout(this);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.addView(newIPbutNO);
                ll.addView(newIPbutOK);

//            newIPbutNO.setX(120);       // смещение по X
//            newIPbutOK.setX(250);       // смещение по X
/////////////////////////
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(

                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
//            lp.setMargins(0,0,0,0);
                lp.weight = 1;                 // установить вес 1
                newIPbutNO.setLayoutParams(lp);
//            lp.setMargins(0,0,0,5);
                newIPbutOK.setLayoutParams(lp);
//////////////////////////
                final String tmpstr;
                name_adr = new ArrayList<String>();
                num_save_ip = read_config_int("num_save_ip");           // читаем кол-во сохраненных IP
                if(num_save_ip != 0){                                   // заполняем список сохраненными IP
                    for(int dr = 0; dr<num_save_ip; dr++){
                        //name_adr.add(read_config_str("server_IP"+Integer.toString(dr+1))+"    -    "+read_config_str("new_name_obj"+(dr+1)));
                        ////////////////////////////////////////////
                        String sstr = read_config_str("new_name_obj"+(dr+1));
                        if(sstr.equals("")){ sstr = "Объект "+(dr+1); }
                        name_adr.add(read_config_str("server_IP"+Integer.toString(dr+1))+"    -    "+sstr);
                        ////////////////////////////////////////////
                        Log.d(tag, "new_name_obj"+(+dr+1)+" .. "+read_config_str("new_name_obj"+(dr+1)));
                    }

                }

                ////////////////////////////////////////////
                final ListView lst_ipadr = new ListView(MainActivity.this);
//                String[] name_adr = { "Иван", "Марья", "Петр" };
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, name_adr);
//                final ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, name_adr);
                lst_ipadr.setAdapter(adapter);
                new_IP_found = true;

                LinearLayout lay_forIP;
                lay_forIP = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView txtIP = lay_forIP.findViewById(R.id.txt_v);
                final TextView txtHEADER = lay_forIP.findViewById(R.id.Head_v);
                txtHEADER.setText("Smart Light");
                txtIP.setText("Установка нового IP адреса");
                lay_forIP.addView(new_ip);
//                lay_forIP.addView(lst_ipadr);
                l_scrl.addView(lst_ipadr);
                setListViewHeightBasedOnChildren(lst_ipadr);        // выставляем отбражение всех элементов списка
                l_scrl.addView(ll);
                scroll_v.addView(l_scrl);
                lay_forIP.addView(scroll_v);

                tmpstr = SERVER_IP;
                new_ip.setHint(SERVER_IP);

                builder.setView(lay_forIP);
                alert = builder.create();
                alert.show();

                View.OnClickListener OKIP = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        close_TCP();
                        SERVER_IP = new_ip.getText().toString();
                        if(SERVER_IP.equals(""))SERVER_IP = tmpstr;
                        else{
                            /////////////////////////////////////////////////////
                            if(SERVER_IP.equals(read_config_str("server_IP"))){ new_IP_found = false;}          // если в памяти у же есть этот IP, то сбрасываем флаг new_IP_found - сохранять не будем
                            for(int ip = 0; ip<num_save_ip ; ip++){
                                if(SERVER_IP.equals(read_config_str("server_IP"+Integer.toString(ip+1)))){ new_IP_found = false;}        //// если в памяти у же есть этот IP, то сбрасываем флаг new_IP_found - сохранять не будем
                            }
                            /////////////////////////////////////////////////////          SERVER_IP != tmpstr
                            if(new_IP_found) {
                                num_save_ip++;
                                saved_config("num_save_ip", num_save_ip);
                                select_pos = num_save_ip; saved_config("select_pos", select_pos);           // текущая позиция из списка
                                saved_config("server_IP"+Integer.toString(num_save_ip), SERVER_IP);
                            }
                        }
                        saved_config("server_IP", SERVER_IP);                   // сохраняем IP для следующего старта приложения
                        name_serv.setText("IP server "+SERVER_IP);                 // " port "+ port_int
                        room_name.setText("Объект "+num_save_ip);
                        alert.dismiss();
                    }
                };
                newIPbutOK.setOnClickListener(OKIP);
                View.OnClickListener NOIP = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                newIPbutNO.setOnClickListener(NOIP);
                ///////////////////////////////////////
                lst_ipadr.setOnItemClickListener(new AdapterView.OnItemClickListener() {    // еще один вариант обработчика нажатия на пункт списка
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        Log.d(tag, "itemClick: position = " + position + ", id = "
                                + id+ " всего адресов в списке "+ " ::: "+num_save_ip);
                        close_TCP();
//                        SERVER_IP =  name_adr.get(position);
                        SERVER_IP = read_config_str("server_IP"+(position+1));
                        select_pos = position+1; saved_config("select_pos", select_pos);           // текущая позиция из списка
                        saved_config("server_IP", SERVER_IP);                           // сохраняем IP для следующего старта приложения
                        name_serv.setText("IP server "+SERVER_IP);                         //" port "+ port_int
                        if(read_config_str("new_name_obj"+select_pos).equals("")){ room_name.setText("Объект "+select_pos);}
                        else{ room_name.setText(read_config_str("new_name_obj"+select_pos)); }
                        alert.dismiss();
                    }
                });
                ////////////////////////
                lst_ipadr.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {    // еще один вариант обработчика нажатия на пункт списка
                    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                                   int position, long id) {
 /*                       int newposID = 1;
                        for(int ip = 0; ip<position ; ip++){                 // перезаписываем IP адреса
                            saved_config("server_IP"+newposID, name_adr.get(ip));
                            newposID++;
                                                       Log.d(tag, name_adr.get(ip)+"  "+newposID);
                        }
                        for(int ip = position+1; ip<num_save_ip ; ip++){                 // перезаписываем IP алреса
                            saved_config("server_IP"+newposID, name_adr.get(ip));
                            newposID++;
                                                        Log.d(tag, name_adr.get(ip)+"  "+newposID);
                        }
                        num_save_ip--;
                        saved_config("num_save_ip", num_save_ip);               // сохраняем новое количество IP
                       // name_adr.remove(position);
//                        adapter.remove(name_adr.get(position));
//                        setListViewHeightBasedOnChildren(lst_ipadr);
                        //adapter.notifyDataSetChanged();

                        Log.d(tag, "Удаление пункта списка "+position);
                        alert.dismiss();*/

                        select_pos = position;
                        dialog2_show(1);
                        alert.dismiss();
                        return true;
                    }

                });
                ////////////////////////
                break;
            case 6:                                                             // перименование подключенного объекта, включение объекта из списка
                /////////////////////////
                ScrollView scroll_vn = new ScrollView(this);
                LinearLayout l_scrln = new LinearLayout(this);
                l_scrln.setOrientation(LinearLayout.VERTICAL);
                final Button butNO = new Button(this);
                butNO.setText("Отмена");
                List<String> name_adrn;

                name_adr = new ArrayList<String>();
                name_adrn = new ArrayList<String>();
                String tmp_str = "";
                num_save_ip = read_config_int("num_save_ip");           // читаем кол-во сохраненных IP
                if(num_save_ip != 0){                                   // заполняем список сохраненными IP
                    for(int dr = 0; dr<num_save_ip; dr++){
                        name_adr.add(read_config_str("server_IP"+Integer.toString(dr+1)));
                        tmp_str = read_config_str("new_name_obj"+Integer.toString(dr+1));
                        if(tmp_str.equals(""))tmp_str = "Объект "+(dr+1);
                        name_adrn.add(tmp_str);
                    }
                }

                final ListView lst_ipadrn = new ListView(MainActivity.this);
                final ArrayAdapter<String> adaptern = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, name_adrn);
                lst_ipadrn.setAdapter(adaptern);

                LinearLayout lay_forIPn;
                lay_forIPn = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView txtIPn = lay_forIPn.findViewById(R.id.txt_v);
                final TextView txtHEADERn = lay_forIPn.findViewById(R.id.Head_v);
                txtHEADERn.setText("Smart Light");
                txtIPn.setText("Выбор объекта");

                l_scrln.addView(lst_ipadrn);
                setListViewHeightBasedOnChildren(lst_ipadrn);        // выставляем отбражение всех элементов списка
                scroll_vn.addView(l_scrln);
                lay_forIPn.addView(scroll_vn);
                l_scrln.addView(butNO);

                builder.setView(lay_forIPn);
                alert = builder.create();
                alert.show();

                View.OnClickListener chanel = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                butNO.setOnClickListener(chanel);
                lst_ipadrn.setOnItemClickListener(new AdapterView.OnItemClickListener() {    // еще один вариант обработчика нажатия на пункт списка
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        Log.d(tag, "itemClick: position = " + position + ", id = "
                                + id+ " всего адресов в списке "+ " ::: "+num_save_ip);
                        close_TCP();
                        SERVER_IP =  name_adr.get(position);
                        select_pos = position+1; saved_config("select_pos", select_pos);           // текущая позиция из списка
                        saved_config("server_IP", SERVER_IP);                           // сохраняем IP для следующего старта приложения
                        name_serv.setText("IP server "+SERVER_IP);                         //" port "+ port_int
                        if(read_config_str("new_name_obj"+select_pos).equals("")){ room_name.setText("Объект "+select_pos);}
                        else{ room_name.setText(read_config_str("new_name_obj"+select_pos)); }
                        alert.dismiss();
                    }
                });
                ////////////////////////
                lst_ipadrn.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {    // еще один вариант обработчика нажатия на пункт списка
                    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                                   int position, long id) {
                        new_select_pos = position+1;
//                        select_pos = position+1;
                        Log.d(tag, " pos_до переименования "+select_pos+", pos_после переименования "+ new_select_pos);
                        dialog2_show(2);
                        alert.dismiss();
                        return true;
                    }

                });
                /*
                final EditText new_name = new EditText(MainActivity.this);
                final Button newbutOK = new Button(this);
                final Button newbutNO = new Button(this);

                newbutOK.setText("Применить");
                newbutNO.setText("Отмена");
                new_name.setInputType(InputType.TYPE_CLASS_TEXT); // вместо энтер на клаве будет крыжик применить
                LinearLayout lll=new LinearLayout(this);
                lll.setOrientation(LinearLayout.HORIZONTAL);
                lll.addView(newbutNO);
                lll.addView(newbutOK);

                LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                lpp.weight = 1;                 // установить вес 1
                newbutNO.setLayoutParams(lpp);
                newbutOK.setLayoutParams(lpp);

                LinearLayout lay_forname;
                lay_forname = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView txt_n = lay_forname.findViewById(R.id.txt_v);
                final TextView txtH_n = lay_forname.findViewById(R.id.Head_v);
                txtH_n.setText("Smart Light");
                txt_n.setText("Новое название объекта");
                new_name.setHint("Гостинная");
                lay_forname.addView(new_name);
                lay_forname.addView(lll);
                builder.setView(lay_forname);
                alert = builder.create();
                alert.show();
                ////////////////////////
                View.OnClickListener YEScreate = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        String str_new_obj;
                        str_new_obj = new_name.getText().toString();
                        room_name.setText(str_new_obj);
                        saved_config("new_name_obj"+select_pos, str_new_obj);
                        alert.dismiss();
                    }
                };
                newbutOK.setOnClickListener(YEScreate);
                View.OnClickListener NOcreate = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                newbutNO.setOnClickListener(NOcreate);
                */
                ////////////////////////
                break;
            case 7:                                                             // // сброс настроек по умолчанию
                LinearLayout reset_lay;
                reset_lay = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView vr_head = reset_lay.findViewById(R.id.Head_v);
                final TextView vr_txt = reset_lay.findViewById(R.id.txt_v);
                final Button butRES = new Button(this);
                butRES.setText("Настройки по умолчанию");
                butRES.setTextColor(getResources().getColor(R.color.RED));
                butRES.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50*scale_Y);
                reset_lay.addView(butRES);

                vr_head.setText("Smart Light");
                vr_txt.setText("Внимание! Все настройки подключения к сети будут удалены, дальнейшая работа " +
                        "модуля будет возможна только в сети Smart_Home_EZ!\n");
                vr_txt.setTextColor(getResources().getColor(R.color.RED));
                builder.setView(reset_lay);
                alert = builder.create();
                alert.show();

                View.OnClickListener reset_def = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        //////////////////////////////
                        if (status_SERV.equals("Соединение установлено")) {
                            name_SSID = "Smart_Home_EZ";
                            pass_SSID = "12345678";
                            mode_serv = '3';
                            getIPfor_client("192.168.4.1");
                            name_SSID.getChars(0, name_SSID.length(), bufTCPout, 35);
                            pass_SSID.getChars(0, pass_SSID.length(), bufTCPout, 52);
                            bufTCPout[51] = (char) name_SSID.length();
                            bufTCPout[61] = mode_serv;
                            cmd_send = set_link;
                            count_req++;
                        }else {show_txt_toast("Нет подключения к сети WIFI");}

                        //////////////////////////////
                        alert.dismiss();
                    }
                };
                butRES.setOnClickListener(reset_def);
                break;
            case 8:
                LinearLayout viewST;
                viewST = (LinearLayout) getLayoutInflater().inflate(R.layout.lay_set_tmp, null);
                final TextView st_txt = viewST.findViewById(R.id.fonlay_txt);
                final TextView edit_st = viewST.findViewById(R.id.settmp_txt);
                final LinearLayout lay111 = viewST.findViewById(R.id.lay_for_txtedit);
                final TextView new_yes = viewST.findViewById(R.id.text_ye);
                final TextView new_NO = viewST.findViewById(R.id.text_no);
                st_txt.setText("Для перехода в меню настроек кликните пиктограмму \"домика\" в верхнем правом углу экрана.\n\nПоказывать это напоминание при старте?");
                lay111.removeView(edit_st);

                builder.setView(viewST);
                alert = builder.create();
                alert.show();
                View.OnClickListener handl_newyes = new View.OnClickListener() {        // да
                    public void onClick(View v) {
                        saved_config("saved_show_help", 0);
                        alert.dismiss();
                    }
                };
                new_yes.setOnClickListener(handl_newyes);
                View.OnClickListener handl_newNO = new View.OnClickListener() {          // нет
                    public void onClick(View v) {
                        saved_config("saved_show_help", 1);
                        alert.dismiss();
                    }

                };
                new_NO.setOnClickListener(handl_newNO);
                break;
        }
        flag_dialog = false;
    }
    ////////////////////////////////////
    void dialog2_show(int dialog) {
        flag_dialog = true;
        final AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (dialog) {
            case 1:
                final Button newIPbutOK = new Button(this);
                final Button newIPbutNO = new Button(this);
                newIPbutOK.setText("Да");
                newIPbutNO.setText("Отмена");
                LinearLayout lay_forIP;
                lay_forIP = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView txtIP = lay_forIP.findViewById(R.id.txt_v);
                final TextView txtHEADER = lay_forIP.findViewById(R.id.Head_v);
                txtHEADER.setText("Smart Light");
                String nameobj = read_config_str("new_name_obj"+Integer.toString(select_pos+1));
                if(nameobj.equals(""))nameobj = Integer.toString(select_pos+1);
                txtIP.setText("Удалить объект "+nameobj+"?");
//Log.d(tag, "del - "+read_config_str("new_name_obj"+Integer.toString(select_pos+1))+ "  select_pos = "+select_pos);
                lay_forIP.addView(newIPbutOK); lay_forIP.addView(newIPbutNO);

                builder.setView(lay_forIP);
                alert = builder.create();
                alert.show();

                View.OnClickListener OKIP = new View.OnClickListener() {        //
                    public void onClick(View v) {
//////////////////
                        int newposID = 1;
//                        if(select_pos>0) {
                        for (int ip = 0; ip < select_pos; ip++) {                 // перезаписываем IP адреса
//                                saved_config("server_IP" + newposID, name_adr.get(ip));
                            newposID++;
                            Log.d(tag, name_adr.get(ip) + "  " + newposID);
                        }

                        for (int ip = select_pos + 1; ip < num_save_ip; ip++) {                 // перезаписываем IP алреса
                          //  saved_config("server_IP" + newposID, name_adr.get(ip));
                            saved_config("server_IP" + newposID, read_config_str("server_IP"+Integer.toString(ip+1)));
                            saved_config("new_name_obj"+newposID, read_config_str("new_name_obj"+Integer.toString(ip+1)));
                            newposID++;
                            Log.d(tag, name_adr.get(ip) + "  " + newposID);
                        }
                        saved_config("new_name_obj"+newposID, "");
                        num_save_ip--;
                        saved_config("num_save_ip", num_save_ip);               // сохраняем новое количество IP
                        /*
                        for (int ip = select_pos + 1; ip < num_save_ip; ip++) {                 // перезаписываем IP алреса
                            Log.d("LOG1", "server_IP " + newposID+"__"+read_config_str("server_IP"+Integer.toString(ip+1)));
                            Log.d("LOG1", "new_name_obj"+newposID+"__"+read_config_str("new_name_obj"+Integer.toString(ip+1)));
                            newposID++;
      //                      Log.d("LOG1", name_adr.get(ip) + "  " + newposID);
                        }
                        num_save_ip--;
                        Log.d("LOG1", "num_save_ip "+num_save_ip);               // сохраняем новое количество IP*/
//                        }
                        // name_adr.remove(position);
//                        adapter.remove(name_adr.get(position));
//                        setListViewHeightBasedOnChildren(lst_ipadr);
                        //adapter.notifyDataSetChanged();
//                        String nameobj = read_config_str("new_name_obj"+Integer.toString(select_pos+1));
//                        if(nameobj.equals(""))
                        Log.d(tag, "Удаление пункта списка "+select_pos);
//////////////////
                        alert.dismiss();
                    }
                };
                newIPbutOK.setOnClickListener(OKIP);
                View.OnClickListener NOIP = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                newIPbutNO.setOnClickListener(NOIP);
                ///////////////////////////////////////
                break;
            case 2:
                final EditText new_name = new EditText(MainActivity.this);
                final Button newbutOK = new Button(this);
                final Button newbutNO = new Button(this);

                newbutOK.setText("Применить");
                newbutNO.setText("Отмена");
                new_name.setInputType(InputType.TYPE_CLASS_TEXT); // вместо энтер на клаве будет крыжик применить
                LinearLayout lll=new LinearLayout(this);
                lll.setOrientation(LinearLayout.HORIZONTAL);
                lll.addView(newbutNO);
                lll.addView(newbutOK);

                LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                lpp.weight = 1;                 // установить вес 1
                newbutNO.setLayoutParams(lpp);
                newbutOK.setLayoutParams(lpp);

                LinearLayout lay_forname;
                lay_forname = (LinearLayout) getLayoutInflater().inflate(R.layout.fon_munu, null);
                final TextView txt_n = lay_forname.findViewById(R.id.txt_v);
                final TextView txtH_n = lay_forname.findViewById(R.id.Head_v);
                txtH_n.setText("Smart Light");
                txt_n.setText("Новое название объекта");
                new_name.setHint("Гостинная");
                lay_forname.addView(new_name);
                lay_forname.addView(lll);
                builder.setView(lay_forname);
                alert = builder.create();
                alert.show();
                ////////////////////////
                View.OnClickListener YEScreate = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        String str_new_obj;
                        str_new_obj = new_name.getText().toString();
                        saved_config("new_name_obj"+ new_select_pos, str_new_obj);
//                        room_name.setText(str_new_obj);
                        str_new_obj = read_config_str("new_name_obj"+select_pos);
                        if(str_new_obj.equals(""))str_new_obj = "Объект "+select_pos;
                        room_name.setText(str_new_obj);

                        alert.dismiss();
                    }
                };
                newbutOK.setOnClickListener(YEScreate);
                View.OnClickListener NOcreate = new View.OnClickListener() {        //
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                };
                newbutNO.setOnClickListener(NOcreate);
                break;
            case 3:

                break;
        }
        flag_dialog = false;
    }
    ////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////
    void pars_data(String str){
///////////////////////////////////
// команда отправлена 68 байтом, но после перезагрузки димер отправляет её  36 байтом
//////////////////////////////////
        int num_r;

        if(size_bufIN > 191) {
            try {
                str.getChars(0, bufTCPin.length, bufTCPin, 0); // копирование символов строки в массив bufTCPin
                num_r = ((int) bufTCPin[46]) | (((int) bufTCPin[47]) << 8);  // получаем номер последнего запроса
                tele = new Telemetry(bufTCPin, count_req);
//        if(tele.status_busy == OKK){ busy = OKK; } else{ busy = NNO; }
                if (cmd_send != req_data) {                  // если была отправлена какая-то команда, отличная от запроса телеметрии, то ждем подтверждения получения ее от сервера и дублируем запросы этой же командой
                    if (cmd_send == tele.COMAND && count_req == tele.count_COMAND) {
                        Log.d(tag, "cmd_send " + cmd_send + ", count_req " + count_req + " tele count_req " + tele.count_COMAND);
                    }
                }
///////////////////////////////////////
                String sstt = "", sst_p = "";
                for (int y = 0; y < 12; y++) {
                    sstt = sstt + bufTCPin[y + 19];
                }
                for (int y = 0; y < 4; y++) {
                    sst_p = sst_p + bufTCPin[y + 31];
                }
                Log.d(tag, "IP_adr " + sstt + "  " + "port_N  " + sst_p);
///////////////////////////////////////

                count_wait_answer = 0;                  // в случае успешного приема сбрасываем счетчик ожидания ответа
                //           }
            } catch (Exception e) {
                Log.d(tag, "Exception pars_data client func");
            }
        }else{
            try{
                ////////////// в штатном режиме модуль отправляет по 38 байт, ниже обработка
                str.getChars(0, size_bufIN, bufTCPin, 0); // копирование символов строки в массив bufTCPin
//                if((bufTCPin[36] == mode_light || bufTCPin[36] == synchro) && cmd_send != set_link ){
                if(bufTCPin[36] == mode_light ){
                    cmd_send = synchro;                                    // debug tmp
                    //                   if(bufTCPin[36] == synchro)cmd_send = req_data;     // debug tmp
                }
                eX = bufTCPin[37]*step;  showLight.sendEmptyMessage(0);
//////////////////////////////////////////////////
                String str_IP_adr = ""+bufTCPin[19] + bufTCPin[20] + bufTCPin[21] + "." + bufTCPin[22] + bufTCPin[23] + bufTCPin[24] + "." + bufTCPin[25] + bufTCPin[26] + bufTCPin[27] + "." + bufTCPin[28] + bufTCPin[29] + bufTCPin[30];
                if (bufTCPin[36] == set_link && !str_IP_adr.equals("___.___.___.___") && !str_IP_adr.equals("192.168.004.001")) {
                    Log.d(tag, "get_command "+bufTCPin[36]+" str_IP_adr "+str_IP_adr);

                    if(!new_link_create) {
                        String sstt = "", sst_p = "";
                        for (int y = 0; y < 12; y++) {
                            sstt = sstt + bufTCPin[y + 19];
                        }
                        for (int y = 0; y < 4; y++) {
                            sst_p = sst_p + bufTCPin[y + 31];
                        }
                        Log.d(tag, "IP_adr " + sstt + "  " + "port_N  " + sst_p);
                        SERVER_IP = "" + bufTCPin[19] + bufTCPin[20] + bufTCPin[21] + "." + bufTCPin[22] + bufTCPin[23] + bufTCPin[24] + "." + bufTCPin[25] + bufTCPin[26] + bufTCPin[27] + "." + bufTCPin[28] + bufTCPin[29] + bufTCPin[30];
                        port = "" + bufTCPin[31] + bufTCPin[32] + bufTCPin[33] + bufTCPin[34];
                        initESP_ON = false;
                        Log.d(tag, "SERVER_IP " + SERVER_IP + " PORT_SERV " + port);
                        ////////////
                        /*
                        saved_config("server_IP" + 1, SERVER_IP);
                        saved_config("num_save_ip", 1);
                        * */
                        num_save_ip++;
                        saved_config("num_save_ip", num_save_ip);                               // сохраняем количество известных IP адресов num_save_ip
                        select_pos = num_save_ip; saved_config("select_pos", select_pos);           // текущая позиция из списка
                        saved_config("server_IP"+Integer.toString(num_save_ip), SERVER_IP);     // сохраняем новое устройство в списке IP адресов
                        ////////////////////////////////
                        ////////////
                        cmd_send = req_data;                // команда управления прошла успешно, в переменную command помещаем команду запроса телеметрии
                        count_req++;
                        new_link_create = true;
                        Log.d(tag, "get_command new_link_create"+ new_link_create);
                        show_txt_toast("Выполнено!");
                    }
                }
                //           saved_config("num_req", count_req);
//////////////////////////////////////////////////
                Log.d(tag,"buf 36 = "+Integer.toString(bufTCPin[36])+" buf 38 = "+Integer.toString(bufTCPin[37]));

            }catch(Exception tt){Log.d(tag, "Exception pars_data client func_2");}
        }
        String ip_a= ""; String p = "";
        ip_a = "" + bufTCPin[19] + bufTCPin[20] + bufTCPin[21] + "." + bufTCPin[22] + bufTCPin[23] + bufTCPin[24] + "." + bufTCPin[25] + bufTCPin[26] + bufTCPin[27] + "." + bufTCPin[28] + bufTCPin[29] + bufTCPin[30];
        p = "" + bufTCPin[31] + bufTCPin[32] + bufTCPin[33] + bufTCPin[34];
        Log.d(tag, "IP "+gotIP(ip_a)+" port "+p);
////////////////////////////////////
        count_wait_answer = 0;
    }
    void close_TCP(){
        clientTCP.tcp_close();
        mRun = false;

    }
    ///////////////////////отображение статуса подключения......
    Handler handlstatus = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            status_net.setText(status_SERV);
            if (status_SERV.equals("подключаюсь..")) {
                count_lost_connect++;
                if (count_lost_connect > 8) {
                    progressBar.setVisibility(View.VISIBLE); // прогрессбар крутится
                }
                if (count_lost_connect > 1000) count_lost_connect = 0;
            } else {
                count_lost_connect = 0;
                progressBar.setVisibility(View.GONE);                                       // прогрессбар  невидим
            }
        }
    };
    ////////////////////////вывод всплывающих сообщений
    void show_txt_toast(String str){
        Toast.makeText(this,str, Toast.LENGTH_SHORT).show();
    }
    /////////////////////////////////////////////
    public void handleMessage(Message msg) {
/*            status.setText(status_SERV);
            if(status_SERV.equals("подключаюсь..")){
                count_lost_connect++;
                if(count_lost_connect>8) {
                    progressBar.setVisibility(View.VISIBLE); // прогрессбар крутится
                }
                if(count_lost_connect>1000)count_lost_connect = 0;
            }
            else {
                count_lost_connect = 0;
                progressBar.setVisibility(View.GONE);                                       // прогрессбар  невидим
            }
        }*/
    }
    ///////////////////////////////////////////////////////////
    String cur_data(){
        Date curTime = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy  HH:mm");  // задаем формат даты
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");  // задаем формат даты
        String sdt_= sdf.format(curTime);
//        txt_dev.setText(sdt_);
        return sdt_;
    }
    void func_req_data(int command){
        int [] dim = new int[64];
        if(command == synchro)count_req++;
        if (count_req > 255) count_req = 1;
        Log.d(tag, "Send_comand, count REQ; " + count_req+", command: "+cmd_send+" scalegr "+scalegr);
//////////////////////////////////////////////////////////////
        if(cmd_send != cmd_none) {
            Send_com send = new Send_com(bufTCPout, dim, command);

            Log.d(tag, "Send_comand = " + (int) bufTCPout[68]);
            str_mess = new String(bufTCPout);    // копирование массива символов в строку
//        clientTCP.send_m(str_mess);
            new Thread(new Runnable() {         // Если запускать без потока, то работает не на всех телефонах
                @Override
                public void run() {
                    clientTCP.send_m(str_mess);
                }
            }).start();
//    try { Thread.sleep(100); } catch (Exception ex_cr) { Log.d(tag, "Exception ex_cr"); }
        }
    }
    ///////////////////////////////////////
    int find_SECOND_(String str_time){          // получение системного времени в секундах
        int sec = 0;
        String hour, min, SEC_;
        char [] tmr= new char[8];
        try{
            str_time.getChars(11, 19, tmr, 0);
            hour = ""+tmr[0]+tmr[1];
            min = ""+tmr[3]+tmr[4];
            SEC_ = ""+ tmr[6]+tmr[7]; //sec = 352;
            sec = (Integer.parseInt(hour))*60*60+Integer.parseInt(min)*60+Integer.parseInt(SEC_);
//            Log.d(tag, "hour  :: "+Integer.parseInt(hour)+", min :: "+Integer.parseInt(min)+", sec :: "+Integer.parseInt(SEC_));
        }
        catch(Exception e){Log.d(tag, "SECOND_ ");}
        return sec;
    }
    /////////////////////////timer/////////////////////////////
    class MyTimerTask extends TimerTask {                                       // таймер 250 мс
        @Override
        public void run() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "dd:MM:yyyy HH:mm:ss", Locale.getDefault());
            final String strDate = simpleDateFormat.format(calendar.getTime());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clientTCP.send_staus_socket(str_tmp);
                    status_SERV = str_tmp[0];
                    if(status_SERV.equals("Соединение установлено")){connect_server = true;}
                    else {connect_server = false;}
                    if(status_SERV.equals("Соединение закрыто")){
                        mRun = false;
                    }
 //Log.d(tag, "status_SERV "+status_SERV+" netSSID_cur "+netSSID_cur);
                    if(data_read){
//                        mServerMessage = mServerMessage+"\n"+str_tmp[1];
                        mServerMessage = str_tmp[1];
                        pars_data(mServerMessage);
                        data_read = false;
                    }
                    else{
/*
                    if(count_wait_answer > 10 && !flag_dialog){                 // более 10 сек от сервера ничего не приходит - закрываем соединение и перезапускаем подкл. к серверу
                        count_wait_answer = 0;
                        Log.d(tag, "count_wait_answer "+count_wait_answer + " req NUM "+count_req);
                        Log.d(tag, "status client_TCP " + clientTCP.tcp_NET.getStatus());
                        if(status_SERV.equals("Соединение установлено")){
                            close_TCP();
                            Log.d(tag, "Соединение закрыто");
                        }
                        if(clientTCP.tcp_NET.getStatus().equals(FINISHED)){
                            clientTCP = new tcp_client(SERVER_IP, port_int);
                            ///////////////////////////////////////////////////////////////////////
                            ////////////////////////
    //                            Timer tim_one;
    //                          TimerTask tmr_one_task;
    //                          tim_one = new Timer();
    //                          tmr_one_task = new TMR_one_t();
    //                          try{tim_one.schedule(tmr_one_task, 3500); }catch(Exception tt){;}  // одноразовый запуск таймера через 3.5 сек

////////////////////////
/////////////////////////////////////////////////////////////////////////
                            Log.d(tag, "Сервер перезапущен!");
                        }
                    }*/
///////////////////////////////////
                        if(clientTCP.tcp_NET.getStatus().equals(FINISHED)){
                            clientTCP = new tcp_client(SERVER_IP, port_int);
                            ///////////////////////////////////////////////////////////////////////
                            ////////////////////////
                            //                            Timer tim_one;
                            //                          TimerTask tmr_one_task;
                            //                          tim_one = new Timer();
                            //                          tmr_one_task = new TMR_one_t();
                            //                          try{tim_one.schedule(tmr_one_task, 3500); }catch(Exception tt){;}  // одноразовый запуск таймера через 3.5 сек

////////////////////////
/////////////////////////////////////////////////////////////////////////
                            Log.d(tag, "Сервер перезапущен!");
                        }
////////////////////////////////////////
                    }
////////////////////////////debug
/*chR = ++chR;
int inTt = (int)chR;
Log.d(tag, "конвертируем символ : "+inTt);*/
////////////////////////////debug
                    handlstatus.sendEmptyMessage(0);
                    count_start_req++;
/////////////////////////////////////////////////////////////////
////       Каждую секунду, в случае не получения подтверждения на отпрвленную команду, дублируется посылка.
/////////////////////////////////////////////////////////////////
                    if(count_start_req == 12) {                       // каждые 3 секунды запрашивается ответ, если нет касания экрана и не получен ответ на предыдущий запрос
                        // if (!touchFlag && count_wait_answer == 0){
                        if (!touchFlag ){
                            //  if (!touchFlag ){
                            if(cmd_send != req_data && cmd_send != mode_light) {
//                                if (connect_server) {
                                func_req_data(cmd_send);      // если команда не запрос телеметрии, то отправка команды серверу
//                                } else { cmd_send = req_data; }
                            }
///////////////////////////////////////
//                            cmd_send = req_data;   //for debug
////////////////////////////////////////////
                        }
                        count_start_req = 0;                // сброс счетчика задающего интервал отправки запроса
//                    count_wait_answer++;                // счетчик секунд
                        SECOND_ = find_SECOND_(strDate);
                        //                Log.d(tag, " Second "+ SECOND_);
                    }
                    /////////////////////////////////////////////
                    if(cmd_send == mode_light) {
                        func_req_data(cmd_send);      //
                        if(wait_ENDSENDTCP){
                            cmd_send = synchro;     // debug tmp
//                             cmd_send = req_data; // debug tmp
                            wait_ENDSENDTCP = false;
                        }
                    }
                    /////////////////////////////////////////////
                }
            });
            cnt_0_25_sec++;
            if(cnt_0_25_sec>2400)cnt_0_25_sec = 0; /// больше 10 мин - сброс
        }
    }
    //////////////////////////////////////////////////////////
    void set_pos_but(View v, float x, float y){                 // если не выполнить эту функцию сначала, то функция v.set.X(float x) работает некорректно
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(

                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins((int)x,(int)y,0,0);
        //                       lp.setMargins(x, y, 0, 0);
        v.setLayoutParams(lp);
    }
    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////
    class TimerTask2 extends TimerTask {

        @Override
        public void run() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "dd:MMMM:yyyy HH:mm:ss a", Locale.getDefault());
            final String strDate = simpleDateFormat.format(calendar.getTime());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                mCounterTextView.setText(strDate);
                    dialog_show(8);
                }
            });
        }
    }
    //////////////////////////////////////////////
    //////////////////////////////////////одноразовый запуск таймера/////////////////////////
    class TMR_one_t extends TimerTask {

        @Override
        public void run() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy  HH:mm:ss");
            final String strDate = simpleDateFormat.format(calendar.getTime());
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if(read_config_str("SINHRO").equals(cur_data_cl)){}
                    else{ cmd_send = synchro; }
                }
            });
        }
    }
/////////////////////////////////////////////////
}
