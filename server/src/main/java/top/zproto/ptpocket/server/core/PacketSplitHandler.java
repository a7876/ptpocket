package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import top.zproto.ptpocket.common.Protocol;

/**
 * 协议拆包器
 */
public class PacketSplitHandler extends LengthFieldBasedFrameDecoder {
    private static final int PREFIX_LENGTH = Protocol.MAGIC_NUM_LENGTH + Protocol.VERSION_LENGTH + Protocol.COMMAND_LENGTH;

    public PacketSplitHandler() {
        super(Protocol.BODY_LENGTH_LIMIT + PREFIX_LENGTH + Protocol.BODY_LENGTH
                , PREFIX_LENGTH, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.getInt(in.readerIndex()) != Protocol.MAGIC_NUM) {
            ctx.channel().close(); // 终止连接
            return null;
        }
        return super.decode(ctx, in);
    }
}
