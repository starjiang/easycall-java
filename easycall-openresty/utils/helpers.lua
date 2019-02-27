local _M = {}

function _M.startwith(str,substr)
  if str == nil or substr == nil then
    return  false
  end
  local s,e  = string.find(str,substr,1,true) 
  if s ~= 1 then
    return  false
  else
    return  true
  end
end

return _M
