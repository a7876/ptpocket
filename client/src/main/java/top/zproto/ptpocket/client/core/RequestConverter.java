package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.client.entity.Request;

/**
 * 请求转换接口
 */
public interface RequestConverter {
    void convert(Request request, ByteBuf buf);
}
