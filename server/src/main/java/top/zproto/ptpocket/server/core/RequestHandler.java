package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import top.zproto.ptpocket.server.entity.Command;

import java.util.List;

public class RequestHandler extends ByteToMessageDecoder {
    public final static RequestHandler instance = new RequestHandler();
    private final CommandParser parser = CommandParser.instance;

    // 解析请求
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Command c = parser.parse(ctx.channel(), in);
        Main.submitCommand(c);
    }
}
