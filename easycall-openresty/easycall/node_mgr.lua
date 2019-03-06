local zkclient = require("zkclient.zk")
local cjson = require("cjson")

local _M = {}
local mt = { __index = _M }

_M.lb_type_active = 1
_M.lb_type_random = 2
_M.lb_type_random_weight = 3
_M.lb_type_round_robin = 4
_M.lb_type_hash = 5
_M.round_robin_seq = {}
_M.req_active_list = {}

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

function _M.node_used_count(self,name,node,num)
  local nodes = self.req_active_list[name] or {}
  local node_count = nodes[node] or 0
  node_count = node_count+num
  nodes[node] = node_count
  self.req_active_list[name] = nodes
end

function _M.get_node(self,name,lb_type,route_key)

  math.randomseed(tostring(os.time()):reverse():sub(1, 7))
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
    return self:lb_random(name,serv_list)
  elseif lb_type == self.lb_type_random_weight then
    return self:lb_random_weight(name,serv_list)
  elseif lb_type == self.lb_type_round_robin then
    return self:lb_round_robin(name,serv_list)
  elseif lb_type == self.lb_type_hash then
    return self:lb_hash(name,serv_list,route_key)
  elseif lb_type == self.lb_type_active then
    return self:lb_active(name,serv_list)
  else 
    return nil,"not support lb_type"
  end
end

function _M.get_len(self,table)

  if type(table) ~= "table" then
    return 0
  end

  if #table ~= 0 then
    return #table
  end

  local len = 0
  for k, v in pairs(table) do
    len = len + 1
  end
  return len
end

function _M.lb_active(self,name,serv_list)
  
  local active_nodes = self.req_active_list[name] or {}
  if self:get_len(active_nodes) == 0 then 
    local index = math.random(1,#serv_list)
    return serv_list[index],nil
  end
  local active_list = {}
  local total = 0
  for i= 1,#serv_list do
     local active = active_nodes[serv_list[i]] or 0
     total = total + active
     active_list[i] = active
  end
  local sum = 0
  local weight_list = {}

  for i= 1,#active_list do
    local active = active_list[i] or 0
    sum = sum + (total - active)
    weight_list[i] = total - active
  end

  if sum  == 0 then
    local index = math.random(1,#serv_list)
    return serv_list[index],nil
  end 

  local random = math.random(1,sum)
  for i= 1,#weight_list do
    local weight = weight_list[i] or 0
    random = random - weight
    if random <= 0 then
      return serv_list[i]
    end
  end
  return serv_list[1]
end

function _M.lb_random(self,name,serv_list)
  local index = math.random(1,#serv_list)
  return serv_list[index],nil
end

function _M.lb_random_weight(self,name,serv_list)
  return self:lb_random(name,serv_list)
end

function _M.lb_round_robin(self,name,serv_list)
  local seq = self.round_robin_seq[name] or 0
  seq = seq+1
  self.round_robin_seq[name]= seq
  local index = seq % #serv_list
  return serv_list[index+1],nil
end

function _M.lb_hash(self,name,serv_list,route_key)
  local seq = ngx.crc32_short(route_key)
  local index = math.abs(seq) % #serv_list
  return serv_list[index+1],nil
end

return _M