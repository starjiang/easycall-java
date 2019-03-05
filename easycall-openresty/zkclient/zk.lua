-- Author AlbertXiao

local zk = require "zkclient.zkconn"
local tablen = table.getn

local _M = { __version = "0.01" }

local mt = { __index = _M }

function _M.new(self, config)
    local timeout = config.timeout or 2000
    if not self.inited then
        self.inited = true
        self.robin = 0
    end
    return setmetatable({serv_list=config.serv_list, timeout=timeout}, mt)
end

function _M._get_host(self)
    local serv_list = self.serv_list
    local index = self.robin % tablen(serv_list)  + 1
    self.robin = self.robin + 1
    return serv_list[index]
end

function _M.connect(self)
    local conn = zk:new()
    conn:set_timeout(self.timeout)
    for i=1, #self.serv_list do
        local host = self:_get_host()
        local ok, err = conn:connect(host)
        if not ok then
            ngx.log(ngx.ERR,"connect " .. host .. " error:" ..err..",retry next server")
        else
            self.conn = conn
            return conn,nil
        end
    end
    return nil,"no zk server available"
end

function _M.get_children(self, path)
    local conn = self.conn
    local err = ""
    if not conn then
        conn,err = self:connect()
        if not conn then
            self.conn = nil
            return nil, err
        end
    end
    local res, err = conn:get_children(path)
    if not res then
        conn:close()
        self.conn = nil
        return nil, err
    end
    return res,nil
end

function _M.get_data(self, path)
    local conn = self.conn
    local err = ''
    if not conn then
        conn,err = self:connect()
        if not conn then
            self.conn = nil
            return nil, err
        end
    end
    local res, err = conn:get_data(path)
    if not res then
        conn:close()
        self.conn = nil
        return nil, err
    end
    return res,nil
end

function _M.close(self)
    local conn = self.conn
    if conn then
        self.conn:close()
        self.conn = nil
    end
end

return _M

