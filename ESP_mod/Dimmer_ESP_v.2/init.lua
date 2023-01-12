print ( "Waiting ...")
local mytmr = tmr.create()
local pin_PWM = 3
gpio.mode (pin_PWM, gpio.OUTPUT)
gpio.write (pin_PWM, gpio.LOW)
mytmr:register (10000, tmr.ALARM_SINGLE, function() print ( "Starting ..."); 
    print ( "Start esp_init");
    dofile ( "main.lua")
end)
mytmr:start()
