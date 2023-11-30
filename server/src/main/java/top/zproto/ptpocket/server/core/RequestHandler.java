package top.zproto.ptpocket.server.core;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import top.zproto.ptpocket.server.entity.Command;

import java.io.IOException;
import java.util.List;

public class RequestHandler extends ByteToMessageDecoder {
    private final CommandParser parser = CommandParser.instance;

    private static final AttributeKey<Client> CLIENT = AttributeKey.newInstance("clients");

    // 解析请求
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Command c = parser.parse(ctx.channel().attr(CLIENT).get(), in);
        Main.submitCommand(c);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception { // 连接到来
        Client client = new Client();
        ctx.channel().attr(CLIENT).set(client);
        client.channel = ctx.channel();
        ServerHolder.INSTANCE.clients.add(client);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { // 处理连接关闭
        ServerHolder.INSTANCE.clients.remove(ctx.channel().attr(CLIENT).get());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException)
            return;
        super.exceptionCaught(ctx, cause);
    }
}
