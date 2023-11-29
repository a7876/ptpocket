package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.server.entity.Response;

public interface ResponseProcessor {
    void processResponse(Response response, ByteBuf buf);
}
