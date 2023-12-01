package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 响应处理handler
 */
public class ResponseAcceptor extends ByteToMessageDecoder {
    static final ResponseAcceptor INSTANCE = new ResponseAcceptor();
    private static final ResponseConverter converter = ResponseConverter.INSTANCE;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ctx.channel().attr(Client.KEY).get().offerResponse(converter.convert(in));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(Client.KEY).get().close(); // 关闭连接
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.getCause().printStackTrace();
        ctx.channel().close();
    }
}
