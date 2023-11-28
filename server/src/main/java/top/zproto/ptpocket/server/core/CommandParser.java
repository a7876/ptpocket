package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.DoubleDataObject;
import top.zproto.ptpocket.server.datestructure.IntDataObject;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.CommandPool;

import java.security.Key;

import static top.zproto.ptpocket.server.core.ServerCommandType.*;

public class CommandParser implements Protocol {
    public static final CommandParser instance = new CommandParser();
    private final CommandPool pool = CommandPool.instance;
    private static final byte SUPPORTED_VERSION = 1;

    public Command parse(Client client, ByteBuf buf) {
        skipMagicNumber(buf);
        if (getVersion(buf) != SUPPORTED_VERSION)
            return simpleCommand(client, VERSION_UNSUPPORTED);
        byte command = getCommand(buf);
        switch (command) {
            case CommandType.SET:
                return set(client, buf);
            case CommandType.GET:
                return get(client, buf);
            case CommandType.H_SET:
                return hSet(client, buf);
            case CommandType.H_GET:
                return hGet(client, buf);
            case CommandType.H_DEL:
                return hDel(client, buf);
            case CommandType.Z_ADD:
                return zAdd(client, buf);
            case CommandType.Z_DEL:
                return zDel(client, buf);
            case CommandType.Z_RANGE:
                return zRange(client, buf);
            case CommandType.Z_RANK:
                return zRank(client, buf);
            case CommandType.Z_SCORE:
                return zScore(client, buf);
            case CommandType.Z_RANGE_SCORE:
                return zRangeScore(client, buf);
            case CommandType.Z_REVERSE_RANK:
                return zReverseRank(client, buf);
            default:
                return unkonwnCommand(client);
        }
    }

    private void skipMagicNumber(ByteBuf buf) { // 跳过魔数
        buf.readInt();
    }

    private byte getVersion(ByteBuf buf) {
        return buf.readByte();
    }

    private byte getCommand(ByteBuf buf) {
        return buf.readByte();
    }

    private Command simpleCommand(Client client, ServerCommandType commandType) {
        return getCommand(client).setCommandType(commandType);
    }

    private Command unkonwnCommand(Client client) {
        return getCommand(client).setCommandType(UNKNOWN_COMMAND);
    }

    private Command get(Client client, ByteBuf buf) {
        // body中直接只有一个key
        if (buf.readableBytes() == 0) // 没有key
            return unkonwnCommand(client);
        return setDataObject(getCommand(client), new DataObject(buf)).setCommandType(GET);
    }

    private Command set(Client client, ByteBuf buf) {
        // keyLength + key + value
        return threePartParse(client, buf, SET);
    }

    private Command hSet(Client client, ByteBuf buf) {
        // keyLength + key + innerKeyLength + inner key + value
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() < keyLength + 4 + 1 + 1) // 确保后面都能最短满足
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() <= keyLength)
            return unkonwnCommand(client);
        DataObject innerKey = new DataObject(buf, keyLength);
        DataObject value = new DataObject(buf);
        return setDataObject(getCommand(client), key, innerKey, value).setCommandType(H_SET);
    }

    private Command hGet(Client client, ByteBuf buf) {
        // keyLength + key + innerKey
        return threePartParse(client, buf, H_GET);
    }

    private Command hDel(Client client, ByteBuf buf) {
        // keyLength + key + innerKey
        return threePartParse(client, buf, H_DEL);
    }

    private Command zAdd(Client client, ByteBuf buf) {
        // keyLength + key + double + value
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() < keyLength + 8 + 1) // 确保最短长度
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        DoubleDataObject d = new DoubleDataObject(buf.readDouble());
        DataObject value = new DataObject(buf);
        return setDataObject(getCommand(client), key, d, value).setCommandType(Z_ADD);
    }

    private Command zDel(Client client, ByteBuf buf) {
        // keyLength + key + value
        return threePartParse(client, buf, Z_DEL);
    }

    private Command zRange(Client client, ByteBuf buf) {
        // keyLength + key + int + int
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() != keyLength + 8)
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        IntDataObject i0 = new IntDataObject(buf.readInt());
        IntDataObject i1 = new IntDataObject(buf.readInt());
        return setDataObject(getCommand(client), key, i0, i1).setCommandType(Z_RANGE);
    }

    private Command zRank(Client client, ByteBuf buf) {
        // keyLength + key + value
        return threePartParse(client, buf, Z_RANK);
    }

    private Command zReverseRank(Client client, ByteBuf buf) {
        // keyLength + key + value
        return threePartParse(client, buf, Z_REVERSE_RANK);
    }

    private Command zRangeScore(Client client, ByteBuf buf) {
        // keyLength + key + double + double
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() != keyLength + 16)
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        DoubleDataObject d0 = new DoubleDataObject(buf.readDouble());
        DoubleDataObject d1 = new DoubleDataObject(buf.readDouble());
        return setDataObject(getCommand(client), key, d0, d1).setCommandType(Z_RANGE_SCORE);
    }

    private Command zScore(Client client, ByteBuf buf) {
        // keyLength + key + value
        return threePartParse(client, buf, Z_SCORE);
    }

    /**
     * 从池中获取Command
     */
    private Command getCommand(Client client) {
        return pool.getObject().setClient(client);
    }

    // 装载dataObject
    private Command setDataObject(Command command, DataObject... dataObjects) {
        command.setDataObjects(dataObjects);
        return command;
    }

    private Command threePartParse(Client client, ByteBuf buf, ServerCommandType serverCommandType) {
        // 专门针对 keyLength + key + innerKey/value 结构
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() <= keyLength)
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        DataObject innerKeyOrValue = new DataObject(buf);
        return setDataObject(getCommand(client), key, innerKeyOrValue).setCommandType(serverCommandType);
    }
}
