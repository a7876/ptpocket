package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import top.zproto.ptpocket.client.exception.UnknownDataPacketReceivedException;
import top.zproto.ptpocket.common.Protocol;

/**
 * 协议拆包器
 */
public class ProtocolSplitHandler extends LengthFieldBasedFrameDecoder {
    private static final int PREFIX_LENGTH = Protocol.MAGIC_NUM_LENGTH + Protocol.VERSION_LENGTH + Protocol.COMMAND_LENGTH;

    public ProtocolSplitHandler() {
        super(Protocol.BODY_LENGTH_LIMIT + PREFIX_LENGTH + Protocol.BODY_LENGTH
                , PREFIX_LENGTH, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.getInt(in.readerIndex()) != Protocol.MAGIC_NUM) {
            ctx.channel().close(); // 终止连接
            throw new UnknownDataPacketReceivedException("data packet violate protocol, " +
                    "do not start with correct magic number");
        }
        return super.decode(ctx, in);
    }
}
