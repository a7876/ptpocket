package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.common.ResponseType;
import top.zproto.ptpocket.server.entity.Response;

public class Responder implements Protocol, ResponseType {
    static final Responder instance = new Responder();

    public void respond(Response response, ByteBuf out) {

    }
}
