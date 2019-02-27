local cjson = require("cjson")
local struct  = require("utils.struct")
local msgpack = require("utils.msgpack")
local node_mgr = require("easycall.node_mgr")

local _M = {}
local mt = { __index = _M }

function _M.new(self,zklist,pool_size,timeout)
  local nm = node_mgr:new({serv_list=zklist,timeout=2000})
  local obj = {
    nm = nm,
    pool_size = pool_size,
    timeout = timeout
  }
  return setmetatable(obj, mt),nil
end

function _M.request(self,format,head,body_data)
  
  local node,err = node_mgr.get_node(self.nm,head.service)
  if not node then
    return nil,err
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
  if head_len > 2*1024*1024 or body_len > 2*1024*1024 then
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
  sock:setkeepalive(1800*1000,self.pool_size)
  return format,head,body_data,nil
end

return _M