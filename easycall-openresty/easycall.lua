local mp = require("resty.msgpack")
easycall = {}
function easycall.say(msg)
  local my_data = {this = {"is",4,"test"}}
  local encoded = mp.pack(my_data)
  local decoded = mp.unpack(encoded)  
  ngx.say(msg.."starjiang")
end


return easycall