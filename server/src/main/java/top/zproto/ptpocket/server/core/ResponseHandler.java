package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.MessageToByteEncoder;
import top.zproto.ptpocket.server.entity.Response;

public class ResponseHandler extends MessageToByteEncoder<Response> {
    public static final ResponseHandler instance = new ResponseHandler();
    private static final Responder responder = Responder.instance;

    @Override
    protected void encode(ChannelHandlerContext ctx, Response msg, ByteBuf out) throws Exception {
        // 处理输出
        responder.respond(msg, out);
    }
}
