local easy_client = require ("easycall.client")
local helpers = require("utils.helpers")
local cjson = require("cjson")

local zkservers = {"127.0.0.1:2181","127.0.01:2181"}
local pool_size = 200
local timeout = 2000
local format = 1 --json serilize

local h, err = ngx.req.get_headers(0,true)
local head = {}
for k, v in pairs(h) do
    if  helpers.startwith(k,"X-Easycall-") then
      head[string.sub(k,12)] = v
    end
end

ngx.req.read_body()
local body_data = ngx.req.get_body_data()

if not body_data then
  ngx.status = 400
  ngx.log(ngx.ERR,"request:",cjson.encode(head),",failed:postbody is empty")
  return
end
if string.len(body_data) > 2 * 1024 *1024  then
  ngx.status = 400
  ngx.log(ngx.ERR,"request:",cjson.encode(head),",failed:postbody is too big")
  return
end

local client = easy_client:new(zkservers,pool_size,timeout)
local rformat,rhead,rbody_data,err = client:request(format,head,body_data)
if err then
  ngx.log(ngx.ERR,"request:",cjson.encode(head),",failed:",err)
  ngx.status = 500
  ngx.header["X-Easycall-ret"] = 1002
  ngx.header["X-Easycall-msg"] = err
  return
else
  if rhead.ret ~= 0 then
    ngx.status = 500
    ngx.header["X-Easycall-ret"] = rhead.ret
    ngx.header["X-Easycall-msg"] = rhead.msg
    return
  else
    ngx.header["X-Easycall-ret"] = rhead.ret
    ngx.header["X-Easycall-msg"] = rhead.msg
    ngx.print(rbody_data)
  end
end


