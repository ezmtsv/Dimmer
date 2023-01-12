---------------------------------
local my_pin_nummber = 4
local pin_PWM = 3;
pwm.setup(pin_PWM, 1000, 0)
pwm.start(pin_PWM)
tmr_pwm = tmr.create()
local ex_string = require 'func_STR'
local  time_delay_last = 0     
local  time_delay = 0         
_G.tm_delay = 0
----------------debug
--------------------
gpio.mode (my_pin_nummber, gpio.OUTPUT)
gpio.serout (my_pin_nummber, gpio.HIGH, {500000,300000}, 5, 1) -- 5 кол-во повторов
----------------- WIFI----------------------
local conf_var = {}
conf_var = ex_string.get_init_CONF()
package.loaded['func_STR']=nil
print(conf_var.cmd)
--dofile("WI_FI_reconect.lua")

if rawequal(conf_var.cmd, 'cmd_= "work"') == false then
    dofile("WI_FI_reconect.lua") print("__reconect")
    else 
    print("___config")
    dofile("server_def.lua")
    dofile("WI_FI_config.lua")   
end
function set_PWM()
    time_delay = _G.tm_delay   -- максимум 10000 мкс для 100Гц, глобальная переменная
    time_delay = time_delay / 10
--    print("_G.tm_delay ".._G.tm_delay)
--    time_delay = math.floor(time_delay)
--    if time_delay>1023 then time_delay = 1023 end

    if time_delay ~= time_delay_last then
        if time_delay == 1000 then
         time_delay = 1023
         _G.tm_delay = 10230
--gpio.mode (pin_PWM, gpio.OUTPUT)
--        gpio.write (pin_PWM, gpio.HIGH)
        end   
        print("set PWM "..time_delay)
        pwm.setduty(pin_PWM,time_delay)
        time_delay_last = time_delay
    end
    
end
    tmr_pwm:register(10, tmr.ALARM_AUTO, function() set_PWM() end) 
    tmr_pwm:start()
    collectgarbage()
