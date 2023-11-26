package top.zproto.ptpocket.server.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class TimeOutHandler extends IdleStateHandler {
    public TimeOutHandler(ServerConfiguration config) {
        super(config.timeOutLimit, 0, 0, TimeUnit.MINUTES);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        ctx.channel().close(); // 关闭超时连接
    }
}
