local zkclient = require("zkclient.zk")
local cjson = require("cjson")

local _M = {}
local mt = { __index = _M }

_M.lb_type_active = 1
_M.lb_type_random = 2
_M.lb_type_random_weight = 3
_M.lb_type_round_robin = 4
_M.lb_type_hash = 5

local round_robin_seq = {}

function _M.new(self,config)
  local zk, err = zkclient:new(config)
  if not zk then
      return nil, err
  end
  local obj = {
    zk = zk
  }
  return setmetatable(obj, mt),nil
end

function _M.get_node(self,name,lb_type,route_key)
  
  local node_cache = ngx.shared.node
  local serv_list_str,err = node_cache:get(name)  
  local serv_list = {}
  if not serv_list_str then
    serv_list,err =  zkclient.get_children(self.zk,"/easycall/services/"..name.."/nodes")
    zkclient.close(self.zk)
    if not serv_list then
      ngx.shared.node:set(name,cjson.encode({}),10,0)
      return nil,"service not found:"..err
    end
    ngx.shared.node:set(name,cjson.encode(serv_list),10,0)
  else
    serv_list = cjson.decode(serv_list_str)
  end

  if #serv_list == 0 then
    return nil,"service have no node"
  end

  if lb_type == self.lb_type_random then
    math.randomseed(tostring(os.time()):reverse():sub(1, 7))
    local index = math.random(1,#serv_list)
    return serv_list[index],nil
  elseif lb_type == self.lb_type_round_robin then
    local seq = round_robin_seq[name] or 0
    seq = seq+1
    round_robin_seq[name]= seq
    local index = seq % #serv_list
    return serv_list[index+1],nil
  elseif lb_type == self.lb_type_hash then
    local seq = ngx.crc32_short(route_key)
    local index = seq % #serv_list
    return serv_list[index+1],nil
  else 
    return nil,"not support lb_type"
  end
end

return _M