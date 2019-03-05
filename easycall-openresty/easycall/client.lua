local cjson = require("cjson")
local struct  = require("utils.struct")
local msgpack = require("utils.msgpack")
local node_mgr = require("easycall.node_mgr")

local _M = {}
local mt = { __index = _M }

_M.max_idle_time = 1800*1000
_M.max_head_len = 2 * 1024 * 1024
_M.max_body_len = 2 * 1024 * 1024
_M.format_json = 1
_M.format_msgpack = 0
_M.code_server_err = 1002

function _M.new(self,zklist,pool_size,timeout,lb_type)
  local nm = node_mgr:new({serv_list=zklist,timeout=timeout})
  local obj = {
    nm = nm,
    pool_size = pool_size,
    timeout = timeout,
    lb_type = lb_type
  }
  return setmetatable(obj, mt),nil
end

function _M.request(self,format,head,body_data)
  local lb_type = self.lb_type
  if head['routeKey'] then
    lb_type = node_mgr.lb_type_hash
  end
  local node,err = node_mgr.get_node(self.nm,head['service'],lb_type,head['routeKey'])
  if not node then
    return nil,nil,nil,err
  end
  local sock = ngx.socket.tcp()
  sock:settimeout(self.timeout)
  local ok,err = sock:connect(node)
  if not ok then 
    return nil,nil,nil,err
  end

  local prefetch = ''
  local head_data = ''

  if format == 0 then
    head_data = msgpack.pack(head)
    local head_len = string.len(head_data)
    local body_len = string.len(body_data)
    prefetch = struct.pack(">BBII",0x2,format,head_len,body_len)
  else
    head_data = cjson.encode(head)
    local head_len = string.len(head_data)
    local body_len = string.len(body_data)
    prefetch = struct.pack(">BBII",0x2,format,head_len,body_len)
  end

  local n,err = sock:send(prefetch)
  if err then
    sock:close()
    return nil,nil,nil,err
  end

  local n,err = sock:send(head_data)
  if err then
    sock:close()
    return nil,nil,nil,err
  end

  local n,err = sock:send(body_data)
  if err then
    sock:close()
    return nil,nil,nil,err
  end

  local n,err = sock:send(string.char(0x3))
  if err then
    sock:close()
    return nil,nil,nil,err
  end
  
  local prefetch,err = sock:receive(10)
  if err then
    sock:close()
    return nil,nil,nil,err
  end
  
  local stx,format,head_len,body_len = struct.unpack(">BBII",prefetch)

  if stx ~= 0x2 then
    sock:close()
    return nil,nil,nil,"pkg:invalid stx"
  end
  if head_len > self.max_head_len or body_len > self.max_body_len then
    sock:close()
    return nil,nil,nil,"pkg:invalid headlen or bodylen"
  end

  local head_data,err = sock:receive(head_len)
  if err then
    sock:close()
    return nil,nil,nil,err
  end
  local body_data,err = sock:receive(body_len)
  if err then
    sock:close()
    return nil,nil,nil,err
  end
  local etx,err = sock:receive(1)

  if string.byte(etx) ~= 0x3 then
    sock:close()
    return nil,nil,nil,"pkg:invalid etx"
  end

  if err then
    sock:close()
    return nil,nil,nil,err
  end

  if format == 0 then
    head = msgpack.unpack(head_data)
  else
    head = cjson.decode(head_data)
  end
  sock:setkeepalive(self.max_idle_time,self.pool_size)
  return format,head,body_data,nil
end

return _M