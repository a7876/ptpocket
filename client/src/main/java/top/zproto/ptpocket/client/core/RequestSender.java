package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import top.zproto.ptpocket.client.entity.Request;

/**
 * 请求处理Handler
 */
public class RequestSender extends MessageToByteEncoder<Request> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Request msg, ByteBuf out) throws Exception {
        msg.convertTo(out);
    }
}
