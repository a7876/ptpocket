package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.client.entity.Response;

public class ResponseConverter {
    public static final ResponseConverter INSTANCE = new ResponseConverter();
    public Response convert(ByteBuf buf) {
        return null;
        // TODO
    }
}
