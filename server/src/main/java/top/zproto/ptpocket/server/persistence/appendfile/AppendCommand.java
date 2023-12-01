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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Append File 中转数据类
 * 用于转化命令和将数据重装入库
 */
public class AppendCommand implements AppendFileProtocol {
    ServerCommandType type;
    Command command;
    byte currentDb;

    /**
     * 特别对象用于指示后台线程刷新
     */
    public static final AppendCommand FORCE_FLUSH = new AppendCommand();

    /**
     * 变为可以写入appendFile的数组
     */
    public void toByteBuffer(ByteBuffer bytes) { // should ensure bytes is enough to load the data
        Client client = command.getClient();
        bytes.put((byte) client.getUsedDb()); // 写数据库编号
        bytes.put(type.instruction); // 写命令类型
        int parts = getParts();
        bytes.put((byte) parts); // 写后面部分的数量
        DataObject[] dataObjects = command.getDataObjects();
        for (int i = 0; i < parts; i++) {
            DataObject dataObject = dataObjects[i];
            bytes.putInt(getEachPartSize(dataObject));
            dataObject.copyTo(bytes); // 写数据
        }
    }

    /**
     * 将一条条数据装载进入数据库
     */
    public static void reload(AppendFilePersistence afp) throws IOException {
        byte usedDB = afp.getByte();
        if (usedDB < 0 || usedDB >= ServerHolder.INSTANCE.getDbNumbs()) { // 目前没有这个库
            skipCommand(afp);
            return;
        }
        ReloadCommandType type = getReloadCommandtype(afp.getByte());
        int parts = afp.getByte();
        if (parts < 0)
            throw new IOException("illegal negative value occurred in append file");
        Command command = CommandPool.instance.getObject();
        Client fakeClient = Client.getFakeClient();
        fakeClient.setUsedDb(usedDB); // 设置数据库
        command.setClient(fakeClient);
        DataObject[] dataObjects = new DataObject[parts];
        command.setDataObjects(dataObjects);
        for (int i = 0; i < parts; i++) { // 转载dataObject
            int length = afp.getInt();
            int dataObjectType = length & TYPE_MASK; // 得到具体的类型
            length &= (~TYPE_MASK); // 还原得到真正的长度
            if (dataObjectType == INT_DATA_OBJECT) { // 判断是何种类型的dataObject
                dataObjects[i] = IntDataObject.getFromInt(afp.getReadBuffer());
            } else if (dataObjectType == DOUBLE_DATA_OBJECT) {
                dataObjects[i] = DoubleDataObject.getFromDouble(afp.getReadBuffer());
            } else if (dataObjectType == NORMAL_DATA_OBJECT) {
                dataObjects[i] = new DataObject(afp.getReadBuffer(), length);
            } else if (dataObjectType == LONG_DATA_OBJECT) {
                dataObjects[i] = LongDataObject.getFromLong(afp.getReadBuffer());
            } else {
                throw new IllegalArgumentException("wrong type in reading data from append file");
            }
        }
        type.processCommand(command);
        command.returnObject(); // 统一在此返回
    }

    /**
     * 数据库不存在的时候执行
     */
    private static void skipCommand(AppendFilePersistence afp) throws IOException {
        afp.getByte(); // skip command
        int parts = afp.getByte();
        if (parts < 0)
            throw new IOException("illegal negative value occurred in append file");
        for (int i = 0; i < parts; i++) {
            int size = afp.getInt();
            size &= (~TYPE_MASK);
            afp.dropBytes(size);
        }
    }

    public int calcSpace() {
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
        } else if (dataObject instanceof LongDataObject) {
            return used | LONG_DATA_OBJECT;
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

    public void setType(ServerCommandType type) {
        this.type = type;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setCurrentDb(byte currentDb) {
        this.currentDb = currentDb;
    }

    void clear() {
        type = null;
        command = null;
        currentDb = -1;
    }
}
