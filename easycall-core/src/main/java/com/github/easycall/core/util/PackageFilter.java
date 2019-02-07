package com.github.easycall.core.util;
import java.util.List;

import com.github.easycall.core.exception.EasyInvalidPkgException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;


public class PackageFilter extends ByteToMessageDecoder {
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf,List<Object> out) throws Exception 
	{
		if(!buf.isReadable())
		{
			return;
		}
		
		int len = 0;
		while((len = isValidPackage(buf)) > 0 )
		{
			ByteBuf pkgBuf = buf.readSlice(len);
			EasyPackage pkg = EasyPackage.newInstance().decode(pkgBuf);
			out.add(pkg);

		}

		if(len == -2)
		{
			buf.clear();
			throw new EasyInvalidPkgException("invalid package");
		}
		
	}
	/*
	 * -1 not a whole package -2 error package
	 * */
	static private int isValidPackage(ByteBuf buf)
	{
		
		if(!buf.isReadable())
		{
			return -1;
		}
		
		byte stx = 0;
		int offset = buf.readerIndex();
		if(buf.readableBytes() < 11)
		{
			stx = buf.getByte(offset);
			if(stx != EasyPackage.STX )
			{
				return -2;
			}
			return -1;
		}
		
		stx = buf.getByte(offset);
		
		if(stx != EasyPackage.STX)
		{
			return -2;
		}

		byte format = buf.getByte(offset+1);

		if (format!= EasyPackage.FORMAT_MSGPACK && format!= EasyPackage.FORMAT_JSON){
			return -2;
		}

		int headlen = buf.getInt(offset+2);
		
		if(headlen > EasyPackage.HEAD_MAX_LEN)
		{
			return -2;
		}

		int bodylen = buf.getInt(offset+6);

		if(bodylen > EasyPackage.BODY_MAX_LEN)
		{
			return -2;
		}
		
		if(headlen+bodylen+11 <= buf.readableBytes())
		{
			byte etx = buf.getByte(offset+headlen+bodylen+10);
			if(etx != EasyPackage.ETX)
			{
				return -2;
			}
			return headlen+bodylen+11;
		}
		else 
		{
			return -1;
		}
	}

}
