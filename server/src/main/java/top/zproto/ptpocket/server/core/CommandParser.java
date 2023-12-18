package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.DoubleDataObject;
import top.zproto.ptpocket.server.datestructure.IntDataObject;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.CommandPool;

import static top.zproto.ptpocket.server.core.ServerCommandType.*;

/**
 * 命令解析器
 * 用于构建正确的Command并传递给主线程处理
 */
public class CommandParser implements Protocol {
    public static final CommandParser instance = new CommandParser();
    private final CommandPool pool = CommandPool.instance;

    public Command parse(Client client, ByteBuf buf) {
        skipMagicNumber(buf);
        skipVersion(buf);
        byte command = getCommand(buf);
        skipBodyLength(buf); // 跳过body长度进入body
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
            case CommandType.Z_REVERSE_RANGE:
                return zReverseRange(client, buf);
            case CommandType.Z_RANK:
                return zRank(client, buf);
            case CommandType.Z_SCORE:
                return zScore(client, buf);
            case CommandType.Z_RANGE_SCORE:
                return zRangeScore(client, buf);
            case CommandType.Z_REVERSE_RANK:
                return zReverseRank(client, buf);
            case CommandType.DEL:
                return del(client, buf);
            case CommandType.EXPIRE:
                return expire(client, buf);
            case CommandType.EXPIRE_MILL:
                return expireMill(client, buf);
            case CommandType.SELECT:
                return select(client, buf);
            case CommandType.PERSIST:
                return persist(client, buf);
            case CommandType.STOP:
                return stop(client, buf);
            case CommandType.INFO:
                return info(client, buf);
            case CommandType.REWRITE:
                return rewrite(client, buf);
            default:
                return unkonwnCommand(client);
        }
    }

    private void skipMagicNumber(ByteBuf buf) { // 跳过魔数
        buf.readInt();
    }

    private void skipVersion(ByteBuf buf) {
        buf.readByte();
    }

    private void skipBodyLength(ByteBuf buf) {
        buf.readInt();
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
        return threePartWithLastTwoInt(client, buf, Z_RANGE);
    }

    private Command zReverseRange(Client client, ByteBuf buf) {
        // keyLength + key + int + int
        return threePartWithLastTwoInt(client, buf, Z_REVERSE_RANGE);
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

    private Command del(Client client, ByteBuf buf) {
        // body 只有一个key
        return onlyKey(client, buf, DEL);
    }

    private Command expire(Client client, ByteBuf buf) {
        // keyLength + key + int
        return threePartParseLastInt(client, buf, EXPIRE);
    }

    private Command select(Client client, ByteBuf buf) {
        // body 只有一个byte，是数据库编号
        if (buf.readableBytes() != 1)
            return unkonwnCommand(client);
        IntDataObject i0 = new IntDataObject(buf.readByte());
        return setDataObject(getCommand(client), i0).setCommandType(SELECT);
    }

    private Command expireMill(Client client, ByteBuf buf) {
        // keyLength + key + int
        return threePartParseLastInt(client, buf, EXPIRE_MILL);
    }

    private Command persist(Client client, ByteBuf buf) {
        // body 只有一个key
        return onlyKey(client, buf, PERSIST);
    }

    private Command stop(Client client, ByteBuf buf) {
        // 没有body
        if (buf.readableBytes() != 0)
            return unkonwnCommand(client);
        return simpleCommand(client, STOP);
    }

    private Command info(Client client, ByteBuf buf) {
        // 没有body
        if (buf.readableBytes() != 0)
            return unkonwnCommand(client);
        return simpleCommand(client, INFO);
    }

    private Command rewrite(Client client, ByteBuf buf) {
        // 没有body
        if (buf.readableBytes() != 0)
            return unkonwnCommand(client);
        return simpleCommand(client, REWRITE);
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

    private Command threePartParseLastInt(Client client, ByteBuf buf, ServerCommandType serverCommandType) {
        // 专门针对 keyLength + key + innerKey/value 结构
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() < keyLength + 4)
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        IntDataObject i0 = new IntDataObject(buf.readInt());
        return setDataObject(getCommand(client), key, i0).setCommandType(serverCommandType);
    }

    private Command onlyKey(Client client, ByteBuf buf, ServerCommandType serverCommandType) {
        // 专门针对 只有key 结构
        if (buf.readableBytes() == 0)
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf);
        return setDataObject(getCommand(client), key).setCommandType(serverCommandType);
    }

    private Command threePartWithLastTwoInt(Client client, ByteBuf buf, ServerCommandType serverCommandType) {
        if (buf.readableBytes() < 4)
            return unkonwnCommand(client);
        int keyLength = buf.readInt();
        if (keyLength <= 0 || buf.readableBytes() != keyLength + 8)
            return unkonwnCommand(client);
        DataObject key = new DataObject(buf, keyLength);
        IntDataObject i0 = new IntDataObject(buf.readInt());
        IntDataObject i1 = new IntDataObject(buf.readInt());
        return setDataObject(getCommand(client), key, i0, i1).setCommandType(serverCommandType);
    }
}
