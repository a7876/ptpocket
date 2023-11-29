package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import top.zproto.ptpocket.client.entity.Response;

import java.util.List;

public class ResponseAcceptor extends ByteToMessageDecoder {
    static final ResponseAcceptor INSTANCE = new ResponseAcceptor();
    private static final ResponseConverter converter = ResponseConverter.INSTANCE;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ctx.channel().attr(Client.KEY).get().offerResponse(converter.convert(in));
    }
}
