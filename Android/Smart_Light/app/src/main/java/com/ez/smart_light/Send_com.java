package com.ez.smart_light;

import android.util.Log;

        import android.util.Log;

/**
 * Created by evan on 20.06.2018.
 */

public class Send_com {
    MainActivity Mobj;
    int tmp;
    int set_koef =      0x93;
    int set_link =      0x11;
    int config_mail	=	0x12;     // настройки эл. почты
    String tag = "TAG";
    long crc_send;
    //////////////////////////////////////////////////////////////
    byte server_stat = 1;                       // устройство сервер
    byte client_stat = 0;                       // устройство клиент
    byte termostat_stat = 0b00000010;           // устройство термостат
    byte light_stat = 0b00000110;               // устройство осветительный прибор
    byte power_socet_stat = 0b00000100;         // устройство розетка 220В
    byte operator_stat = 0b00001000;            // оператор
    byte write_stat = 0b00100000;               // запись данных
    byte read_stat = 0b00000000;                // запрос на чтение данных

    byte power_cap0 = 0;                         // мощность подключенного устройства до 100вт
    byte power_cap1 = 1;                         // мощность подключенного устройства от 100 до 200Вт
    byte power_cap2 = 2;                         // мощность подключенного устройства от 200 до 500Вт
    byte power_cap3 = 3;                         // мощность подключенного устройства от 500 до 1000Вт
    byte power_cap4 = 4;                         // мощность подключенного устройства от 1000 до 2000Вт
    byte power_cap5 = 5;                         // мощность подключенного устройства от 2кВт до 4000Вт
    byte power_cap6 = 6;                         // мощность подключенного устройства более 4кВт
    /////////////////////////////////////////////////////////////
    Send_com(char [] buf, int [] buf_int, int cmd){
        int [] buf_dim_int = new int[192];

        buf[0] = 'E'; buf[1] = 'Z'; buf[2] = 'A'; buf[3] = 'P';         // преамбула

        if(cmd != config_mail){ buf[17] = (char)(client_stat | operator_stat | write_stat); }

        buf_dim_int[3+64] = Mobj.count_req & 0xff;
        buf_dim_int[4+64] = cmd & 0xff;
        buf_dim_int[74] = Mobj.scalegr & 0xff;
////////////////////////////////
//////////////////////////////////
        ////////////////////////////////////////////
        for(int i = 0; i<64; i++){
            buf_dim_int[i] = (int)buf[i];
        }
        ////////////////////////////////////////////
        crc_send = CalculateCRC(buf_dim_int, 123); // вычисляем контрольную сумму по 123 байт массива включительно
        buf_dim_int[124] = (int)(crc_send & 0xff); buf_dim_int[125] = (int)((crc_send>>8)&0xff); buf_dim_int[126] = (int)((crc_send>>16)&0xff); buf_dim_int[127] = (int)((crc_send>>24)&0xff);
        Log.d(tag, "CRC paket = "+crc_send+" : " + buf_dim_int[124]+ " : "+ buf_dim_int[125]+ " : "+ buf_dim_int[126]+ " : "+ buf_dim_int[127] );
        ////////////////////////////////////////////////////
        //       for(int j=0; j<50; j++){Log.d(tag,"buf_char["+j+"] = "+buf[j]);}
        ////////////////////////////////////////////////////
        for(int j=64; j<128; j++){
            if(buf_dim_int[j]>127){
                buf_dim_int[j] = buf_dim_int[j]&0x7f;
                buf_dim_int[j+64]= 127;
            } else{
                buf_dim_int[j+64]= 0;
            }

        }
        for(int j=64; j<192; j++){ buf[j] = (char)buf_dim_int[j]; }
        ////////////////////////////////////////
    }
    /////////////////////////////////
    long  CalculateCRC (int[] dim_crc, int size) {
        long  CRC32 = 0;
        long tmp;

        while ( size != 0 ) {
            CRC32 = CRC32 + ((long)dim_crc[size])*size;
            size--;
        }
        CRC32 = CRC32 & 0xffffffff;
        return CRC32;
    }
////////////////////////////////
}
