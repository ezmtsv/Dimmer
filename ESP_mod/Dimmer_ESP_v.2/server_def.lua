local var = {}
answ = require ('answer_mod')
local bright = 100 -- соответствует _G.tm_delay = 
local set_link = 17 
local mode_light = 53
local res_com = 53
local flag_aswer = false
local flag_aswer_count = 1
local sock_def = nil
_G.tm_delay = 0
sv=net.createServer(net.TCP)

_G.flag_link_serv_def = false
_G.flag_stop_serv = false

local tmr1 = tmr.create()
local tmr3 = tmr.create()
------------------------------
--[[
        ]]
------------------------------
--Create Server

function create_serv_def() 
    tmr1:register(350, tmr.ALARM_AUTO, function() status_def_ESP() end) -- вызов функции status_def_ESP() каждые 250 mсек
    wifi.setmode(wifi.STATIONAP)    -- устанавливаем режим точка доступа
    cfg={}
--    cfg.ssid="Dimmer_EZ" -- задаем имя сети
	cfg.ssid="Smart_Home_EZ" -- задаем имя сети
    cfg.pwd="12345678"      -- задаем пароль сети, WiFi точка не поднимется, если пароль короче 8 символов. По умолчанию IP адрес точки всегда 192.168.4.1
    wifi.ap.config(cfg)     -- загружаем настройки
    cfg = nil
    print ("Start server DEF!")
end  
--------------------------------------------------
function receiver(sck, data)
    pcall(function ()
        res_com = string.byte(data, 69)
		if res_com ~= 100 then
            bright = string.byte(data, 75)
            print(" ".._G.tm_delay.." "..bright)
--			_G.tm_delay =  100 * (100 - bright)
            _G.tm_delay =  -0.01461*bright*bright*bright+2.037*bright*bright-118.6*bright+6100
--			_G.tm_delay = (-0.0192*bright*bright*bright)+(4.0542*bright*bright)-(296.59*bright)+8317
            
        end
        print("принято: "..data)
        flag_aswer = true flag_aswer_count = 1
    end)
    if res_com == set_link then
       print("comand set_link "..res_com) 
       pcall(function () answ.save_newdata(data) end)
        if string.byte(data, 62) ~= 51 then                 -- 51 - это сервер по умолчанию
            node.restart()
        end
    end
    collectgarbage()    --сборщик мусора
end
--------------------------------
function def_config(sck)
    _G.flag_link_serv_def = true
--    print("connect with DIMMER_EZ!"..tostring(sock_def))
	print("connect with Smart_Home_EZ!"..tostring(sock_def))
--	tmr1:register(350, tmr.ALARM_AUTO, function() status_def_ESP() end) -- вызов функции status_def_ESP() каждые 250 mсек
--	tmr1:start()
end
--------------------------------
function def_ser_disc(sck)
--    print("OFF connection with DIMMER_EZ!")
	print("OFF connection with Smart_Home_EZ!")
    sock_def = nil
	tmr1:stop() -- останавливаем таймер 1 в случае потери связи с клиентом
end
---------------------------------
if sv then
    sv:listen(8888, function(conn, pl) 
        conn:on("receive", receiver) print(pl) 
        conn:on("connection", def_config) tmr1:start()         -- вызывается при подключении к серверу
        conn:on("disconnection", def_ser_disc) print(pl)    -- вызывается при отключении от сервера
--        conn:on("sent", def_ser_sent)print(pl)
        sock_def = conn             -- сохраняем сокет в переменной sock_def
        collectgarbage()    --сборщик мусора
    end)
end
--------------------------------------
function status_def_ESP()    
        local str1 = "_"
        if flag_aswer == true then
            flag_aswer_count = flag_aswer_count + 1
            if flag_aswer_count > 2 then 
                flag_aswer_count = 1
                var = _G.tm_delay
                print("var = "..var)
                flag_aswer = false
                local dimUTF8 = {res_com, bright}
                pcall(function () str1 = str1..answ.utf8_from(dimUTF8) end)
                err = pcall(function () sock_def:send(answ.pars_data(str1)) end)   -- отправка ответа клиенту
                print(str1)
            end 
        end 
        str1 = nil
        collectgarbage() 
end
function clear_mem()
    answ = nil
    answer_mod = nil
    package.loaded['answer_mod']=nil 
    ex_str = nil
    func_STR = nil
    package.loaded['func_STR']=nil
    INIT_config = nil
    def_config = nil
    def_ser_disc = nil
    create_serv_def = nil
    receiver = nil
    status_def_ESP = nil
    collectgarbage() 
end
function status_server_stop()
    if _G.flag_stop_serv then
        pcall(function () sv:close() end)
        _G.flag_stop_serv = nil
        _G.flag_link_serv_def = nil
        print("Server def STOP!")
        clear_mem()
		tmr1:stop() 
		tmr3:stop()
    end
end
create_serv_def()
create_serv_def = nil
	tmr3:register(250, tmr.ALARM_AUTO, function() status_server_stop() end)
	tmr3:start()
collectgarbage() 
--------------------
