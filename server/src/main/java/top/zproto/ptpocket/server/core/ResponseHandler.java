package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import top.zproto.ptpocket.server.entity.Response;

import java.io.IOException;

public class ResponseHandler extends MessageToByteEncoder<Response> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Response msg, ByteBuf out) throws Exception {
        // 处理输出
        msg.respond(out);
    }
}
