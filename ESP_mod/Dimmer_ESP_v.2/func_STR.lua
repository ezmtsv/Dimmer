do
local M = {}
local line_init = {}
local count_l = 1
INIT_config = {            -- задаем массив данных для инициализации модуля
    num_S = " ",
    ssid = " ",
    pass = " ",
    vers = " ",
    mod = " ",
    ip_adr = " ",
	init_W = " ", 
	cmd = " ",
}; 
local confinit = INIT_config;
-------------------------функция read_count_line_init() получает кол-во строк(до 99)
function M.read_count_line_init()       -- получаем колличество строк записанных в файле, первая строка файла "val.lua" должна всегда содержать эту информацию
    file.open("val.lua", "r")
    local strr = file.readline()
    local count_symb = #strr - 7
    if count_symb == 1 then
        count_l = string.byte(strr,7) - 48
        else
        count_l = (string.byte(strr,7) - 48)*10
        count_l = count_l + (string.byte(strr,8) - 48)
    end
    file.close()
    if count_l > 99 then count_l = 99
    end
end
-------------------------------------------
function M.get_init_CONF()
    M.read_count_line_init()
    
--    file:read "*a"        -- если прочитать файл в строку, то дальше чтение строк(file.readline()) даст nil
------------------------------------- debug ------
--[[    file.open("val.lua", "w+")      -- запись в файл с удалением всего предыдущего
    file.write("new data in file!")
    file.close()
    ]]
--------------------------------------------------
    file.open("val.lua", "r")
    for count = 1, count_l do
        line_init[count] = file.readline()
        if rawequal(line_init[count], nil) == false then
            line_init[count] = string.sub(line_init[count], 1, #line_init[count]-1)     -- убираем символ перехода на новую строку из строки
        else print("строка "..count.." пустая или не прочитана")
       end
    end
    file.close()
    confinit.ssid = line_init[2]
    confinit.pass = line_init[3]
    confinit.vers = line_init[4]
    confinit.mod  = line_init[5]
    confinit.ip_adr = line_init[6]
	confinit.init_W = line_init[7]
	confinit.cmd = line_init[8]
--    print(confinit.ssid..confinit.pass..confinit.vers..confinit.mod)
    return confinit
end
------------------------------------------
function M.write_new_data(fl, marker, wr_d)   -- модифицируем в файле fl нужную строку, определяемую по маркеру - первые 6 символов строки
-- новое значение строки wr_d
    file.open(fl, "r")
    local data_file =  file.read()  -- читаем файл  в строку
--local byte_data = { string.byte(data_file, 1,-1) } -- конвертируем строку в массив байт
    file.close()  
--print(data_file)  -- debug
    local str_marker = marker
    local offset = 6
    local cn = 1
    local data_cnt = 0
    for i = 1, #data_file do                -- находим номер начального символа  в заменяемой строке
        if rawequal(str_marker, string.sub(data_file, cn, offset+cn)) == true then
			print("OK, offset  = "..cn)
			break
		end 
		cn = cn+1
	end  
	data_cnt = cn

	for i = cn, #data_file do           -- находим номер конечного символа в заменяемой строке
		if rawequal("\n", string.sub(data_file, data_cnt, data_cnt)) == true then
			print("OK, data_cnt  = "..data_cnt)
			break
--    else print(string.sub(data_file,  data_cnt,  data_cnt))
		end 
		data_cnt = data_cnt+1
	end 

	local new_data_file = string.sub(data_file,  1,  cn+offset)..wr_d..string.sub(data_file,  data_cnt-1,  #data_file)
	print(new_data_file)

	f_l = file.open(fl, "w+")
	file.write(new_data_file)
	file.close()

end
------------------------------------------
-- M.get_init_CONF()
-- write_new_data("val.lua", 'SSID= "', "Dlink-dir300")
-- write_new_data("val.lua", 'PASS= "', "EvAn1969")
return M

end
