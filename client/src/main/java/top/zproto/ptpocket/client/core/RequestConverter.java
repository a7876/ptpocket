package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.client.entity.Request;

public interface RequestConverter {
    void convert(Request request, ByteBuf buf);
}
