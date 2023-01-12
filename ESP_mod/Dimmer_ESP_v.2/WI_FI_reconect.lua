---------------------
flag_wifi_con = false  
answ = require ('answer_mod')
ex_str = require 'func_STR'
str_cmd = "_"
dimUTF8 = {17, 0}
tmr1 = tmr.create()
tmr0 = tmr.create()
tmr4 = tmr.create()
local count = 0

pcall(function () str_cmd = str_cmd..answ.utf8_from(dimUTF8) end)

conf = {}
conf = ex_str.get_init_CONF()

sv=net.createServer(net.TCP)
function sendIP_for_android()
	pcall(function () sock_def:send(answ.pars_data(str_cmd)) end) 
--    print("_G.init_word_for_ANDROID ".._G.init_word)
end
--[[
-----------debug
print(wifi.ap.getip())
print(wifi.sta.getip())
sock_def:send("send test")
answ = require ('answer_mod')
print(answ.pars_data("OK_save"))
print(string.sub(conf.pass, 8, #conf.pass-1))
print(tostring(_G.flag_link_serv_def))
tmr.stop(4)
st_ESP()
--]]                
----------------------

function def_config(sck)
--    print("connect with DIMMER_EZ!"..tostring(sock_def))
	print("connect with Smart_Home_EZ!"..tostring(sock_def))
--    tmr.alarm(1, 1000, 1, function() sendIP_for_android() end)     -- вызов функции sendIP_for_android() каждые 1000 mсек
--	tmr1:register(1000, tmr.ALARM_AUTO, function() sendIP_for_android() end)
--	tmr1:start()
end
------------------
if sv then
    sv:listen(8888, function(conn, pl) 
        conn:on("connection", def_config) tmr1:start()         -- вызывается при подключении к серверу
--        conn:on("disconnection", def_ser_disc) print(pl)    -- вызывается при отключении от сервера
--        conn:on("sent", def_ser_sent)print(pl)
        sock_def = conn             -- сохраняем сокет в переменной sock_def
        collectgarbage()    --сборщик мусора
    end)
end
--------------------------------------
function create_serv_def() 
    tmr1:register(1000, tmr.ALARM_AUTO, function() sendIP_for_android() end)
    wifi.setmode(wifi.STATIONAP)    -- устанавливаем режим точка доступа
    cfg={}
--    cfg.ssid="Dimmer_EZ" -- задаем имя сети
	cfg.ssid="Smart_Home_EZ" -- задаем имя сети
    cfg.pwd="12345678"      -- задаем пароль сети, WiFi точка не поднимется, если пароль короче 8 символов. По умолчанию IP адрес точки всегда 192.168.4.1
    wifi.ap.config(cfg)     -- загружаем настройки
    cfg = nil
    print ("Start server DEF__!")
end  
------------------
function reload_init_W(ip_adr_ser)
    pcall(function () _G.init_word = answ.initanswer_data(ip_adr_ser, flag_serv) end)
    print("_G.init_word ".._G.init_word)
end
------------------
function st_ESP() 
     if flag_wifi_con == false then      -- получаем IP адрес
       local ip_adr =  wifi.sta.getip()
       if ip_adr ~= nil then            -- IP адрес получен
        print("получен IP "..ip_adr)
        reload_init_W(ip_adr)
        reload_init_W = nil
        flag_wifi_con = true            -- при установки этого флага больше не заходим сюда
--        tmr.stop(0)
		tmr0:stop()
		create_serv_def() 
        ex_str.write_new_data("val.lua", 'cmd_= "', "work")   
        package.loaded['func_STR']=nil  
       else
         print("ожидание получения IP..."..count)
         count = count +1
         if count>60 then
           print("сброс модуля"..count)
           ex_str.write_new_data("val.lua", 'SSID= "', "Smart_Home_EZ")
           ex_str.write_new_data("val.lua", 'mode= "', "servDEF")
           ex_str.write_new_data("val.lua", 'cmd_= "', "work")
 --print(string.sub(conf.pass, 8, #conf.pass-1))
           node.restart()
         end
       end 
    else
--         print("ожидание подключения к сети ")
    end 
        
end
----------------
function init_server()
    status_server_def = nil   -- освобождает память  
	   	
       wifi.setmode(wifi.STATION)
       wifi.sta.config{ssid = string.sub(conf.ssid, 8,#conf.ssid-1), pwd = string.sub(conf.pass, 8, #conf.pass-1)}
       wifi.sta.autoconnect(1)
--       tmr.alarm(0, 3000, 1, function() st_ESP() end)     -- вызов функции status_ESP() каждые 3 сек 
	tmr0:register(3000, tmr.ALARM_AUTO, function() st_ESP() end)
	tmr0:start()
end
---------------
function st_server_def()
    if _G.flag_link_serv_def==nil then
--        tmr.stop(4) 
		tmr4:stop()
		init_server() print("init_serv_")
    else print(tostring(_G.flag_link_serv_def))    
    end
end

--tmr.alarm(4, 500, 1, function() st_server_def() end) 
	tmr4:register(500, tmr.ALARM_AUTO, function() st_server_def() end)
	tmr4:start()
	
