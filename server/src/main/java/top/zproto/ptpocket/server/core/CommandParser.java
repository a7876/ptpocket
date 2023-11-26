package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.server.entity.Command;

public class CommandParser implements Protocol, CommandType {
    public static final CommandParser instance = new CommandParser();

    public Command parse(Channel sc, ByteBuf buf){
        return null;
    }
}
