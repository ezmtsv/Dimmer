local conf = {}
local ex_str = require 'func_STR'
local answ = require ('answer_mod')
--_G.init_word = ""
conf = ex_str.get_init_CONF()
local flag_serv = false
local tmr2 = tmr.create()
local tmr4 = tmr.create()
-----------------------------

function wifi_user(user)
    func_config = nil
    dofile("WI_FI_user_server.lua")
end
-----------------------------
function func_config()
    print("WI-FI config..")
    local my_pin_nummber = 4
    local mode = string.sub(conf.mod, 8,#conf.mod-1)
    local ip_adr_ser = string.sub(conf.ip_adr, 8,#conf.ip_adr-1)

    if rawequal(mode, "servDEF") then flag_serv = true   end 
    pcall(function () _G.init_word = answ.initanswer_data(ip_adr_ser, flag_serv) end)
    print("_G.init_word ".._G.init_word)
    gpio.write (my_pin_nummber, gpio.HIGH)          -- выкл. синий светодиод
   
    -------------------------
    if _G.flag_link_serv_def == false then             -- если не подключен к точке доступа ни один клиент
--        if rawequal(conf.ssid, 'SSID= "Dimmer_EZ"') == false then
		if rawequal(conf.ssid, 'SSID= "Smart_Home_EZ"') == false then
            print(conf.ssid.."  "..conf.pass)
            _G.flag_stop_serv = true          -- останавливаем сервер
			tmr.delay(1000000)
                tmr4:register(1000, tmr.ALARM_SINGLE, function () wifi_user(flag_serv) end)
                tmr4:start()
         end 
    end
    
		tmr2:register(1100, tmr.ALARM_SINGLE, function () func_config = nil; 
		    conf = nil; wifi_user = nil; require = nil; 
		end)
		tmr2:start()	            
    collectgarbage() 
end
------------------------------
	tmr2:register(25000, tmr.ALARM_SINGLE, function () func_config() end)
	tmr2:start()
------------------------------
print(conf.ssid)
collectgarbage() 
