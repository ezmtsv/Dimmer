local flag_wifi_con = false
local flag_serv_start = false;
local flag_serv_con = false;
local bright = 100 -- соответствует _G.tm_delay = 4000
_G.tm_delay = 0
local flag_aswer = false
local flag_aswer_count = 1
local sock = nil
local mode_light = 53
local tmr0 = tmr.create()
local tmr4 = tmr.create()
local tmr5 = tmr.create()
local tmr6 = tmr.create()
local answ = require ('answer_mod')
local ex_str = require 'func_STR'
----------------debug
--------------------
conf = {}
conf = ex_str.get_init_CONF()
function light_mode()
        local str1 = "_"
        if flag_aswer == true then
            flag_aswer_count = flag_aswer_count + 1
            if flag_aswer_count > 4 then 
                flag_aswer_count = 1
                var = _G.tm_delay
                print("var = "..var)
                flag_aswer = false
                local dimUTF8 = {mode_light, bright}
                pcall(function () str1 = str1..answ.utf8_from(dimUTF8) end)
                err = pcall(function () sock:send(answ.pars_data(str1)) end)   -- отправка ответа клиенту
                print(str1)
            end 
        end 
        str1 = nil
        collectgarbage() 
end    
--------------------------------------
function even_servcon(c)        -- функция обработки события соединения с сервером
    flag_serv_con =true
    print("connect with SERVER!")
--	tmr.alarm(5, 250, 1, function() light_mode() end)     -- вызов функции status_def_ESP() каждые 250 mсек
	tmr5:register(250, tmr.ALARM_AUTO, function() light_mode() end)
	tmr5:start()
end

function even_servdiscon(c)     -- функция обработки события потери связи с сервером
    flag_serv_con = false
--    if sock then sock:close() end
--	tmr.stop(6)
	tmr6:stop()
    print("disconnection")
    print(c)
end

function even_servreceive(c)    -- функция обработки события  приема данных от сервера
    local command = 0
    pcall(function ()
        command = string.byte(c, 69)
        bright = string.byte(c, 75)
        print("принято: "..c)
        print(" ".._G.tm_delay.." "..bright)
        flag_aswer = true flag_aswer_count = 1
--        _G.tm_delay =  61 * (100 - bright)
--		if bright>99 then _G.tm_delay = 3 end
        _G.tm_delay = -0.01461*bright*bright*bright+2.037*bright*bright-118.6*bright+6100
    end)
    command = nil    
    collectgarbage()    --сборщик мусора
end
function reload_init_W(ip_adr_ser)
    pcall(function () _G.init_word = answ.initanswer_data(ip_adr_ser, flag_serv) end)
    print("_G.init_word ".._G.init_word)
end
--------------------------------------
function status_ESP() 
    if flag_wifi_con == false then      -- получаем IP адрес
       local ip_adr =  wifi.sta.getip()
       if ip_adr ~= nil then            -- IP адрес получен
        print("получен IP "..ip_adr)
        reload_init_W(ip_adr)
        reload_init_W = nil
        flag_wifi_con = true            -- при установки этого флага больше не заходим сюда
       else
         print("ожидание получения IP")
       end 
    else
    
        if flag_serv_start == false then        -- после получения IP адрес один раз выполняем код ниже
            sock = net.createConnection(net.TCP, 0)
            sock:on("disconnection", function(sock, c) even_servdiscon(c) end )     -- событие потери связи с сервером
            sock:on("connection", function(sock, c) even_servcon(c)  end )          -- событие соединения с сервером
            sock:on("receive", function(sock, c) even_servreceive(c)  end )         -- событие приема данных от сервера
            print("Start connection SERV!")
            print(ip_adr_ser)
        end   
        flag_serv_start = true
        local excep = false
        if flag_serv_con == false then
                    local ip_adr_ser = string.sub(conf.ip_adr, 8,#conf.ip_adr-1)
                    excep = pcall(function () sock:connect(8888, ip_adr_ser) end) -- обработка исключений pcall(function () f() end), 
                    -- если исключение - вернет false, если выполнится - true
                    if excep == false then
                        print(wifi.sta.status())
                        if wifi.sta.status() == 5 then
                            sock:close()
                            elseif wifi.sta.status() == 3 then flag_wifi_con = false;
                        end
                    end
        end
    end 
    print("work status_ESP__")     
end
--------------------
function init_client()
    status_server_def = nil        
    init_client = nil
       wifi.setmode(wifi.STATION)
       wifi.sta.config{ssid = string.sub(conf.ssid, 8,#conf.ssid-1), pwd = string.sub(conf.pass, 8, #conf.pass-1)}
       wifi.sta.autoconnect(1)
--       tmr.alarm(0, 3000, 1, function() status_ESP() end)     -- вызов функции status_ESP() каждые 3 сек    
	  tmr0:register(3000, tmr.ALARM_AUTO, function() status_ESP() end)
	  tmr0:start()  
end
function status_server_def()
    if _G.flag_link_serv_def==nil then
--        tmr.stop(4) 
		tmr4:stop()
		init_client()
    else print(tostring(_G.flag_link_serv_def))    
    end
end

--tmr.alarm(4, 500, 1, function() status_server_def() end) 
	tmr4:register(500, tmr.ALARM_AUTO, function() status_server_def() end)
	tmr4:start()

collectgarbage()	
