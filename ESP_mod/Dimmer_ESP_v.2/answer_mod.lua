do
local M = {}
local comand = 0
local set_link = 17
local count_req_last = 0
_G.tm_delay = 0   -- максимум 10000 мкс для 100Гц

conf = {}
local ex_str = require 'func_STR'

--------------------------

function M.utf8_from(t)         -- формирует из массива байт строку 8-битных символов 
  local bytearr = {}
  for i, v in ipairs(t) do
    local utf8byte = v < 0 and (0xff + v + 1) or v
    table.insert(bytearr, string.char(utf8byte))
  end
  return table.concat(bytearr)
end

--------------------------
function M.initanswer_data(ip_adr, flag)  
local id_dev = 6    -- осветительный прибор
local pow = 2       -- мощность до 500 Вт
ip_adr = ip_adr..'.'
local mac_adr = ""
local str1 = ""
local str2 = ""
local str_initB = ""
 if flag then 
	mac_adr = wifi.ap.getmac()
 else
	mac_adr = wifi.sta.getmac()
 end
 print("mac_adr"..mac_adr)
  local m = {}  
  mac_adr:gsub(".",function(c) table.insert(m,c) end)     -- ковертирование строки mac_adr в массив символов
    for i, v in ipairs(m) do
      if rawequal(':', v) == false then str1 = str1..v end
    end  

  mac_adr = str1
  str_initB = "EZAP"..mac_adr..'_'
  if flag then
    id_dev = id_dev + 1
    str_initB = str_initB..id_dev..pow
  else
    str_initB = str_initB..id_dev..pow
  end
  -----------------------------------  приводим IP адрес формата 192.168.0.1 к формату 192 168 000 001
  local n = {}  
  ip_adr:gsub(".",function(c) table.insert(n,c) end)     -- ковертирование строки mac_adr в массив символов
 local  k = 1  
  for i, v in ipairs(n) do
    if rawequal('.', v) == false then k = k+1 
    else  
      if k == 2 then str2 = str2.."00"..string.sub(ip_adr,i-1,i-1)
      elseif k == 3 then str2 = str2.."0"..string.sub(ip_adr,i-2,i-1)
      else str2 = str2..string.sub(ip_adr,i-3,i-1)
      end
      k = 1
    end
  end    
  str_initB = str_initB..str2
  -------------------------------------
------------------------DEBUG
--print(wifi.ap.getmac())
--print(wifi.sta.getmac())		
------------------------------------------ 		
		return str_initB
end
--------------------------
function M.pars_data(data)
--[[ если data не задано, то фунуция сформирует строку с концовкой _nil_, если
задано, но строка короче 192 симв., то вся строка добавится в конец, если >192симв., то в конец
добавятся символы с 36 по 192
]]		

    local str_tmp2 = ""
    local data_return = ""
    if data==nil then 
        str_tmp2 = "_nil_" 
        else 
        if #data>191 then pcall(function () str_tmp2 = string.sub(data, 36, 192) end) 
        else str_tmp2 = data
        end
    end
    data_return = _G.init_word.."8888"..str_tmp2
--------------DEBUG-------
--print("str"..data_return)
--print("______debug func!____")
----------------------------		
	return data_return
end
------------------------DEBUG
--print(M.pars_data(""))
--print(M.initanswer_data("192.168.0.5", true))
------------------------------------------
--[[function M.debug_func()
    data = "debug_func"
    print("______debug func!____")
    return data
end]]
------------------------------------
function M.save_newdata(data)
	local comand = string.byte(data, 69)
	local count_req = string.byte(data, 68)
    print("save new data "..set_link.." "..count_req_last.." "..count_req)
    if comand == set_link  and count_req_last~=count_req then     
        local new_ssid = string.sub(data, 36, 36 + string.byte(data, 52)-1)
        local new_pass = string.sub(data, 53, 60)					-- 8 символов на пароль
        local new_id_tmp = string.sub(data, 20, 32)
        new_id = ""
        
--[[
        jk = 0
        for i = 1, 4 do
            if i~=4 then 
              new_id = new_id..string.sub(new_id_tmp, 1+jk, 3+jk)..'.'
            else
              new_id = new_id..string.sub(new_id_tmp, 1+jk, 3+jk)
            end
            jk = jk + 3
        end
        ]]
          k = 0
          for j = 1, 4 do
            if rawequal("0", string.sub(new_id_tmp, 1+k, 1+k)) == false then
              new_id = new_id..string.sub(new_id_tmp, 1+k, 3+k)..'.'
              elseif rawequal("0", string.sub(new_id_tmp, 2+k, 2+k)) == false then
              new_id = new_id..string.sub(new_id_tmp, 2+k, 3+k)..'.'  
              else new_id = new_id..string.sub(new_id_tmp, 3+k, 3+k)..'.'
            end  
            k = k+3
          end
          new_id = string.sub(new_id, 1, #new_id-1)
                  
        ex_str.write_new_data("val.lua", 'SSID= "', new_ssid)
        ex_str.write_new_data("val.lua", 'PASS= "', new_pass)
        ex_str.write_new_data("val.lua", 'ipad= "', new_id)
 print(new_id) 
 print("lenght ssid "..#new_ssid)
 print(new_pass) 
 print("lenght pass "..#new_pass)
			if string.byte(data, 62) == 49 then
				ex_str.write_new_data("val.lua", 'mode= "', "serv")
				ex_str.write_new_data("val.lua", 'cmd_= "', "save")	
			elseif string.byte(data, 62) == 51 then
				ex_str.write_new_data("val.lua", 'mode= "', "servDEF")
print("serv")
			end
--			ex_str.write_new_data("val.lua", 'inwd= "', _G.init_word)
			count_req_last = count_req
		elseif comand == 0 then

    end
--    print("M.save_newdata!")
end	

---------------------------------------
return M

end
