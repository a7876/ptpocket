package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.Hash;
import top.zproto.ptpocket.server.datestructure.SortedSet;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.Response;

import java.util.concurrent.TimeUnit;


/**
 * 专门为AppendFileReload准备的命令执行类
 */
public enum ReloadCommandType implements CommandType, CommandProcessor {
    DEL(CommandType.DEL) {
        @Override
        public void processCommand(Command command) { // 删除键
            Client client = command.getClient();
            Database db = getDb(client);
            DataObject key = command.getDataObjects()[0];
            Object removed = db.keyspace.remove(key);
            if (removed != null)
                db.expire.remove(key);
        }
    }, EXPIRE_MILL(CommandType.EXPIRE_MILL) {
        /*
        * 这里需要注意的是持久化中过期时间只会使用此指令，且参数意义不一样，参数是一个时间戳的绝对值
        * */
        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = getDb(client);
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key);
            if (value == null) {
                return;
            }
            long time = dataObjects[1].getLong();
            if (time < 0) {
                return;
            }
            database.expire.insert(key, time);
        }
    }, PERSIST(CommandType.PERSIST) { // 取消过期键

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = getDb(client);
            DataObject key = command.getDataObjects()[0];
            database.expire.remove(key);
        }
    }, SET(CommandType.SET) { // 设置值
        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = getDb(client);
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            DataObject value = dataObjects[1];
            database.keyspace.insert(key, value);
            database.expire.remove(key);
        }
    }, H_SET(CommandType.H_SET) { // 设置hash表
        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = getDb(client);
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key);
            if (value != null && !(value instanceof Hash)) {
                return;
            }
            DataObject innerKey = dataObjects[1];
            DataObject val = dataObjects[2];
            if (value != null) {
                ((Hash) value).insert(innerKey, val);
            } else {
                Hash hash = new Hash(false);
                database.keyspace.insert(key, hash);
                hash.insert(innerKey, val);
            }
        }
    }, H_DEL(CommandType.H_DEL) { // 删除内哈希键

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Hash hash = getHash(client, command);
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[1];
            hash.remove(key);
        }
    }, Z_ADD(CommandType.Z_ADD) { // 有序集增加

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = getDb(client);
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key);
            if (value != null && !(value instanceof SortedSet)) {
                return;
            }
            double score = dataObjects[1].getDouble();
            DataObject dataObject = dataObjects[2];
            if (value == null) {
                SortedSet sortedSet = new SortedSet();
                database.keyspace.insert(key, sortedSet);
                sortedSet.insert(score, dataObject);
            } else {
                ((SortedSet) value).insert(score, dataObject);
            }
        }
    }, Z_DEL(CommandType.Z_DEL) { // 有序集删除

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject val = dataObjects[1];
            sortedSet.remove(val);
        }
    };
    public final byte instruction;

    ReloadCommandType(byte instruction) {
        this.instruction = instruction;
    }

    /**
     * 获取对应的数据库
     */
    protected Database getDb(Client client) {
        int usedDb = client.usedDb;
        return server.dbs[usedDb];
    }

    /**
     * 载入时并不检查是够过期
     */
    protected Object getValue(Database database, DataObject dataObject) {
        return database.keyspace.get(dataObject);
    }

    /**
     * common part for sortedSet
     * 公共部分
     */
    protected SortedSet getSortedSet(Client client, Command command) {
        Database database = getDb(client);
        DataObject[] dataObjects = command.getDataObjects();
        DataObject key = dataObjects[0];
        Object value = getValue(database, key);
        if (value != null && !(value instanceof SortedSet)) {
            return null;
        }
        if (value == null) {
            return null;
        }
        return (SortedSet) value;
    }

    /**
     * common part for hash
     * 公共部分
     */
    protected Hash getHash(Client client, Command command) {
        Database database = getDb(client);
        DataObject[] dataObjects = command.getDataObjects();
        DataObject key = dataObjects[0];
        Object value = getValue(database, key);
        if (value != null && !(value instanceof Hash)) {
            return null;
        }
        if (value == null) {
            return null;
        }
        return (Hash) value;
    }

    /**
     * 全局serverHolder对象
     */
    protected ServerHolder server = ServerHolder.INSTANCE;
}
