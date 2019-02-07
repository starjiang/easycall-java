package com.github.easycall.core.util;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

public class ZkStringSerializer implements ZkSerializer {
		
	public byte[] serialize(Object data) throws ZkMarshallingError {
		return ((String)data).getBytes();
	}
	
	public Object deserialize(byte[] bytes) throws ZkMarshallingError {
		return new String(bytes);
	}
}
