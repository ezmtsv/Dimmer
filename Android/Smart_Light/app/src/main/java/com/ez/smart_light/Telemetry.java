package com.ez.smart_light;

import android.util.Log;

public class Telemetry {

    static String time_data_serv;
    static char OK = 'O';
    final int OKK = 0x55; //
    final int NNO = 0x33; //

    static String tim_G;
    static int status_busy;
    static int time_MC;
    static int debug1;
    static int count_COMAND;
    static int COMAND;
    String tag = "TAG";

    Telemetry(char[] buf, int num_req){
        time_data_serv = "";
        int hourN, minN, hourD, minD;
        String prefix_hour = "", prefix_min = ""; tim_G = "";
///////////////////////////////////////////////////////////

        int   i = 64+6; int znaki_TMP;
        int air_tmp_int, water_tmp_int;
        int debug, sec, hour, min;
        int time_Night_int, time_Day_int;
        int int_buf_int[] = new int[192];
        int time_work_serv;
        for(int k=64; k<192; k++){ int_buf_int[k] = (int)buf[k]&0xff;  }
        for(int k=128; k<192; k++){ if(int_buf_int[k] == 127)int_buf_int[k-64] = int_buf_int[k-64] +128;  }   // нужно добавить 128, чтобы восстановить число

        String tmp_debag;

        int tmp;
        count_COMAND = int_buf_int[64+3];
        COMAND = int_buf_int[64+4];



        int serv_hour, serv_min, serv_sec;
        serv_hour = time_MC/(60*60); serv_min = (time_MC%(60*60))/60; serv_sec = time_MC%60;
        if(serv_hour<10){ time_data_serv = "0"+ serv_hour; } else { time_data_serv = ""+ serv_hour;}
        if(serv_min<10){ time_data_serv = time_data_serv+ ":0"+ serv_min; } else { time_data_serv = time_data_serv+ ":"+ serv_min;}
        if(serv_sec<10){ time_data_serv = time_data_serv+ ":0"+ serv_sec; } else { time_data_serv = time_data_serv+ ":"+ serv_sec;}
//                time_data_serv = ""+ time_MC/(60*60)+":"+(time_MC%(60*60))/60+":"+time_MC%60;

///////////////////////////////////////////////////////////
    }
    ///////////////////////////////////////////
    String two_symbol_after_point(String symb){
        String res;
        int lenght_str;
        char[] s_buf = new char[100];
        char[] buf = new char[100];
        buf  = symb.toCharArray();      // получение из строки массива символов
        int count_sym_point = 0;
        int count_sym = 0;

        boolean flag_point = false;
        lenght_str = symb.length(); // количество символов в строке
        for(int  tmp = 0; tmp< 100; tmp++){s_buf[tmp] = ' ';}
        /////////////////////округление до 2 знаков после запятой
        for(int i = 0; i< lenght_str; i++){
            s_buf[i]= buf[i];
            count_sym++;
            if(buf[i]== '.'){
                flag_point = true;
            }

            if(flag_point){count_sym_point++;}
            if(count_sym_point >2) break;
        }
        ////////////////////////////
        res= "";
        for(int i = 0; i< count_sym; i++){res = res+s_buf[i];}
        return res;
    }
    /////////////////////////////////////////////////////////////////////////
}

