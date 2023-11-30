package top.zproto.ptpocket.server.persistence.appendfile;

import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.server.core.Client;
import top.zproto.ptpocket.server.core.ReloadCommandType;
import top.zproto.ptpocket.server.core.ServerCommandType;
import top.zproto.ptpocket.server.core.ServerHolder;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.DoubleDataObject;
import top.zproto.ptpocket.server.datestructure.IntDataObject;
import top.zproto.ptpocket.server.datestructure.LongDataObject;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.CommandPool;

public class AppendCommand implements AppendFileProtocol {
    ServerCommandType type;
    Command command;
    byte currentDb;

    public AppendCommand(ServerCommandType type, Command command, byte currentDb) {
        this.type = type;
        this.command = command;
        this.currentDb = currentDb;
    }

    /**
     * 变为可以写入appendFile的数组
     */
    public int toBytes(byte[] bytes) { // should ensure bytes is enough to load the data
        Client client = command.getClient();
        int index = 0;
        bytes[index++] = (byte) client.getUsedDb();
        bytes[index++] = type.instruction;
        int parts = getParts();
        bytes[index++] = (byte) parts;
        DataObject[] dataObjects = command.getDataObjects();
        for (int i = 0; i < parts; i++) {
            DataObject dataObject = dataObjects[i];
            index = writeInt(getEachPartSize(dataObject), bytes, index); // 写此部分的长度
            dataObject.copyTo(bytes, index); // 写数据
            index += dataObject.getUsed(); // 加上数据量
        }
        return index;
    }

    /**
     * 从数据重装入db
     */
    public static void reload(byte[] bytes) {
        int index = 0;
        byte usedDB = bytes[index++];
        if (usedDB < 0 || usedDB >= ServerHolder.INSTANCE.getDbNumbs()) // 目前没有这个库
            return;
        ReloadCommandType type = getReloadCommandtype(bytes[index++]);
        int parts = bytes[index++];
        Command command = CommandPool.instance.getObject();
        Client fakeClient = Client.getFakeClient();
        fakeClient.setUsedDb(usedDB); // 设置数据库
        command.setClient(fakeClient);
        DataObject[] dataObjects = new DataObject[parts];
        command.setDataObjects(dataObjects);
        for (int i = 0; i < parts; i++) { // 转载dataObject
            int length = getInt(bytes, index);
            index += 4;
            int dataObjectType = length & TYPE_MASK; // 得到具体的类型
            length &= (~TYPE_MASK); // 还原得到真正的长度
            if (dataObjectType == INT_DATA_OBJECT) { // 判断是何种类型的dataObject
                dataObjects[i] = IntDataObject.getFromInt(bytes, index);
            } else if (dataObjectType == DOUBLE_DATA_OBJECT) {
                dataObjects[i] = DoubleDataObject.getFromDouble(bytes, index);
            } else if (dataObjectType == NORMAL_DATA_OBJECT) {
                dataObjects[i] = new DataObject(bytes, index, length);
            } else if (dataObjectType == LONG_DATA_OBJECT) {
                dataObjects[i] = LongDataObject.getFromLong(bytes, index);
            } else {
                throw new IllegalArgumentException("wrong type in reading data from append file");
            }
            index += length; // 加上长度
        }
        type.processCommand(command);
        command.returnObject(); // 统一在此返回
    }

    private int calcSpace() {
        int neededSpace = 0;
        neededSpace += 1; // dbnum
        neededSpace += 1; // command
        neededSpace += 1; // size
        int parts = getParts();
        neededSpace += parts * 4; // 需要parts个int
        DataObject[] dataObjects = command.getDataObjects();
        for (int i = 0; i < parts; i++) {
            parts += dataObjects[i].getUsed();
        }
        return neededSpace;
    }

    /**
     * 根据具体的类型返回加上了标记的int
     */
    private int getEachPartSize(DataObject dataObject) {
        int used = dataObject.getUsed();
        if (dataObject instanceof DoubleDataObject) {
            return used | DOUBLE_DATA_OBJECT;
        } else if (dataObject instanceof IntDataObject) {
            return used | INT_DATA_OBJECT;
        } else {
            return used | NORMAL_DATA_OBJECT;
        }
    }

    private int getParts() {
        int parts = 0;
        switch (type.instruction) {
            case CommandType.DEL:
            case CommandType.PERSIST:
                parts = 1;
                break;
            case CommandType.SET:
            case CommandType.H_DEL:
            case CommandType.Z_DEL:
            case CommandType.EXPIRE_MILL:
                parts = 2;
                break;
            case CommandType.H_SET:
            case CommandType.Z_ADD:
                parts = 3;
                break;
            default: // should be unreachable
                throw new IllegalArgumentException();
        }
        return parts;
    }

    private static ReloadCommandType getReloadCommandtype(byte command) {
        switch (command) {
            case CommandType.SET:
                return ReloadCommandType.SET;
            case CommandType.H_SET:
                return ReloadCommandType.H_SET;
            case CommandType.H_DEL:
                return ReloadCommandType.H_DEL;
            case CommandType.Z_ADD:
                return ReloadCommandType.Z_ADD;
            case CommandType.Z_DEL:
                return ReloadCommandType.Z_DEL;
            case CommandType.DEL:
                return ReloadCommandType.DEL;
            case CommandType.EXPIRE_MILL:
                return ReloadCommandType.EXPIRE_MILL;
            case CommandType.PERSIST:
                return ReloadCommandType.PERSIST;
            default:
                throw new IllegalArgumentException("reload append file with wrong command type");
        }
    }

    public int writeInt(int num, byte[] bytes, int index) { // testUse
        for (int i = 0; i < 4; i++) {
            bytes[index++] = (byte) (num >>> (i * 8) & 0xff);
        }
        return index;
    }

    public static int getInt(byte[] bytes, int index) { // testUse
        int res = 0;
        for (int i = index; i < index + 4; i++) {
            res |= bytes[i] << i * 8;
        }
        return res;
    }
}
