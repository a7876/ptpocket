package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.server.datestructure.*;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.CommandPool;
import top.zproto.ptpocket.server.entity.Response;
import top.zproto.ptpocket.server.entity.ResponsePool;
import top.zproto.ptpocket.server.persistence.appendfile.AppendCommand;
import top.zproto.ptpocket.server.persistence.appendfile.AppendCommandPool;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 一一对应的命令处理枚举类
 */
public enum ServerCommandType implements CommandType, CommandProcessor {
    UNKNOWN_COMMAND(RESERVED_0) {
        @Override
        public void processCommand(Command command) {
            Response response = response().setResponseType(ServerResponseType.UNKNOWN_COMMAND);
            command.getClient().channel.writeAndFlush(response);
        }
    }, DEL(CommandType.DEL) {
        @Override
        public void processCommand(Command command) { // 删除键
            Client client = command.getClient();
            Database db = checkDB(client);
            if (db == null)
                return;
            DataObject key = command.getDataObjects()[0];
            Object removed = db.keyspace.remove(key);
            if (removed != null)
                db.expire.remove(key);
            appendFile(command); // append file
            responseOK(client);
        }
    }, EXPIRE(CommandType.EXPIRE) {
        @Override
        public void processCommand(Command command) { // 设置过期，单位是秒
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key, client);
            if (value == null) {
                responseNull(client);
                return;
            }
            int time = dataObjects[1].getInt();
            if (time < 0) {
                responseOK(client);
                return;
            }
            long timeOut = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(time);
            database.expire.insert(key, timeOut);
            appendFileForExpire(command, timeOut);
            responseOK(client);
        }
    }, EXPIRE_MILL(CommandType.EXPIRE_MILL) { // 设置过期，单位是毫秒

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key, client);
            if (value == null) {
                responseNull(client);
                return;
            }
            int time = dataObjects[1].getInt();
            if (time < 0) {
                responseOK(client);
                return;
            }
            long timeOut = System.currentTimeMillis() + time;
            database.expire.insert(key, timeOut);
            appendFileForExpire(command, timeOut);
            responseOK(client);
        }
    }, SELECT(CommandType.SELECT) { // 选择数据库

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            int dbNum = command.getDataObjects()[0].getInt();
            if (dbNum < 0 || dbNum > server.dbs.length) {
                responseIllegal(client);
                return;
            }
            client.usedDb = (byte) dbNum;
            responseOK(client);
        }
    }, PERSIST(CommandType.PERSIST) { // 取消过期键

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject key = command.getDataObjects()[0];
            getValue(database, key, client); // 先检查一下是否过期了
            Object removed = database.expire.remove(key);
            appendFile(command);
            if (removed == null)
                responseNull(client);
            else
                responseOK(client);
        }
    }, STOP(CommandType.STOP) {
        @Override
        public void processCommand(Command command) { // 停止服务器
            responseConnectReset(command.getClient());
            server.shutdown = true;
        }
    }, GET(CommandType.GET) { // 获取值

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject key = command.getDataObjects()[0];
            Object value = getValue(database, key, client);
            if (value == null) {
                responseNull(client);
                return;
            }
            if (value instanceof DataObject) { // 检查是否是真确的类型
                responseData(client, (DataObject) value);
            } else {
                responseIllegal(client);
            }
        }
    }, SET(CommandType.SET) { // 设置值

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            DataObject value = dataObjects[1];
            database.keyspace.insert(key, value);
            database.expire.remove(key); // 防止原来的键设置了过期
            appendFile(command);
            responseOK(client);
        }
    }, H_SET(CommandType.H_SET) { // 设置hash表

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key, client);
            if (value != null && !(value instanceof Hash)) {
                responseIllegal(client);
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
            appendFile(command);
            responseOK(client);
        }
    }, H_GET(CommandType.H_GET) { // 获取内哈希的值

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Hash hash = getHash(client, command);
            if (hash == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[1];
            responseData(client, (DataObject) hash.get(key));
        }
    }, H_DEL(CommandType.H_DEL) { // 删除内哈希键

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Hash hash = getHash(client, command);
            if (hash == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[1];
            hash.remove(key);
            appendFile(command);
            responseOK(client);
        }
    }, Z_ADD(CommandType.Z_ADD) { // 有序集增加

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            Database database = checkDB(client);
            if (database == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject key = dataObjects[0];
            Object value = getValue(database, key, client);
            if (value != null && !(value instanceof SortedSet)) {
                responseIllegal(client);
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
            appendFile(command);
            responseOK(client);
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
            appendFile(command);
            responseOK(client);
        }
    }, Z_RANGE(CommandType.Z_RANGE) { // 有序集求范围

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            int leftIndex = dataObjects[1].getInt();
            int rightIndex = dataObjects[2].getInt();
            if (leftIndex < 0 || rightIndex < 0 || leftIndex > rightIndex) {
                responseIllegal(client);
                return;
            }
            responseList(client, sortedSet.getRange(leftIndex, rightIndex));
        }
    }, Z_REVERSE_RANGE(CommandType.Z_REVERSE_RANGE) {
        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            int leftIndex = dataObjects[1].getInt();
            int rightIndex = dataObjects[2].getInt();
            if (leftIndex < 0 || rightIndex < 0 || leftIndex > rightIndex) {
                responseIllegal(client);
                return;
            }
            responseList(client, sortedSet.getReverseRange(leftIndex, rightIndex));
        }
    }, Z_RANGE_SCORE(CommandType.Z_RANGE_SCORE) { // 返回特定分数内的值集合

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            double left = dataObjects[1].getDouble();
            double right = dataObjects[2].getDouble();
            if (left > right) {
                responseIllegal(client);
                return;
            }
            responseList(client, sortedSet.getRangeByScore(left, right));
        }
    }, Z_RANK(CommandType.Z_RANK) { // 获取排名

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject val = dataObjects[1];
            responseInt(client, sortedSet.rank(val));
        }
    }, Z_REVERSE_RANK(CommandType.Z_REVERSE_RANK) { // 获取逆向排名

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject val = dataObjects[1];
            responseInt(client, sortedSet.reverseRank(val));
        }
    }, Z_SCORE(CommandType.Z_SCORE) { // 获取分数

        @Override
        public void processCommand(Command command) {
            Client client = command.getClient();
            SortedSet sortedSet = getSortedSet(client, command);
            if (sortedSet == null)
                return;
            DataObject[] dataObjects = command.getDataObjects();
            DataObject val = dataObjects[1];
            responseDouble(client, sortedSet.getScore(val));
        }
    }, INFO(CommandType.INFO) {
        @Override
        public void processCommand(Command command) {
            Database[] dbs = server.dbs;
            long total = 0;
            for (Database db : dbs)
                total += db.keyspace.getSize();
            String str = String.format("peak processed command each second is %d \n" +
                            "command pool peak size is %d\n" +
                            "response pool peak size is %d\n" +
                            "keySpace total size is %d\n" +
                            "ptpocket is been running for %ds"
                    , server.commandProcessedEachSecondPeakValue
                    , CommandPool.instance.getPeakSize()
                    , ResponsePool.instance.getPeakSize()
                    , total
                    , (System.currentTimeMillis() - server.startTime) / 1000);
            responseString(command.getClient(), str);
        }
    };
    public final byte instruction;

    ServerCommandType(byte instruction) {
        this.instruction = instruction;
    }

    protected Response response() {
        return Response.getObject();
    }

    /**
     * 检查用户是否选择了数据库
     */
    protected Database checkDB(Client client) {
        int usedDb = client.usedDb;
        if (usedDb == -1) {
            client.channel.writeAndFlush(response().setClient(client)
                    .setResponseType(ServerResponseType.DB_UNSELECTED));
            return null;
        }
        return server.dbs[usedDb];
    }

    protected void responseOK(Client client) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.OK));
    }

    protected void responseString(Client client, String s) {
        client.channel.writeAndFlush(response().setClient(client)
                .setResponseType(ServerResponseType.STRING).setString(s));
    }

    protected void responseNull(Client client) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.NULL));
    }

    protected void responseIllegal(Client client) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.ILLEGAL_COMMAND));
    }

    protected void responseConnectReset(Client client) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.CONNECT_RESET));
    }

    protected void responseData(Client client, DataObject dataObject) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.DATA).setDataObjects(dataObject));
    }

    /**
     * 获取键空间的value，执行惰性删除
     */
    protected Object getValue(Database database, DataObject key, Client client) {
        Long l = (Long) database.expire.get(key);
        if (l != null && System.currentTimeMillis() - l >= 0) { // 已经过期
            database.keyspace.remove(key);
            database.expire.remove(key);
            appendFileExpireToDel(key, client);
            return null;
        }
        return database.keyspace.get(key);
    }

    protected void responseList(Client client, List<DataObject> list) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.LIST)
                .setDataObjects(list.toArray(new DataObject[0])));
    }

    protected void responseInt(Client client, int i) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.INT).setiNum(i));
    }

    protected void responseDouble(Client client, double d) {
        client.channel.writeAndFlush(response().setClient(client).setResponseType(ServerResponseType.DOUBLE).setdNum(d));
    }

    /**
     * common part for sortedSet
     * 公共部分
     */
    protected SortedSet getSortedSet(Client client, Command command) {
        Database database = checkDB(client);
        if (database == null)
            return null;
        DataObject[] dataObjects = command.getDataObjects();
        DataObject key = dataObjects[0];
        Object value = getValue(database, key, client);
        if (value != null && !(value instanceof SortedSet)) {
            responseIllegal(client);
            return null;
        }
        if (value == null) {
            responseNull(client);
            return null;
        }
        return (SortedSet) value;
    }

    /**
     * common part for hash
     * 公共部分
     */
    protected Hash getHash(Client client, Command command) {
        Database database = checkDB(client);
        if (database == null)
            return null;
        DataObject[] dataObjects = command.getDataObjects();
        DataObject key = dataObjects[0];
        Object value = getValue(database, key, client);
        if (value != null && !(value instanceof Hash)) {
            responseIllegal(client);
            return null;
        }
        if (value == null) {
            responseNull(client);
            return null;
        }
        return (Hash) value;
    }

    protected void appendFile(Command command) {
        if (server.afp != null) {
            Command nc = commandPool.copyFrom(command);// 每次都需要自己手动获取一个新的command
            AppendCommand ac = appendCommandPool.getObject();
            ac.setCommand(nc);
            server.afp.deliver(ac);
        }
    }

    protected void appendFileForExpire(Command command, long expireTime) {
        if (server.afp != null) {
            Command nc = commandPool.copyFrom(command);
            nc.setCommandType(EXPIRE_MILL);
            LongDataObject ldo = new LongDataObject(expireTime);
            DataObject[] dataObjects = nc.getDataObjects();
            dataObjects[1] = ldo;
            AppendCommand ac = appendCommandPool.getObject();
            ac.setCommand(nc);
            server.afp.deliver(ac);
        }
    }

    protected void appendFileExpireToDel(DataObject key, Client client) {
        if (server.afp != null) {
            Command command = commandPool.getObject();
            command.setDataObjects(new DataObject[]{key});
            command.setClient(client); // 设置client的目的是为了获取当前的数据库号码
            command.setCommandType(DEL);
            AppendCommand ac = appendCommandPool.getObject();
            ac.setCommand(command);
            server.afp.deliver(ac);
        }
    }

    /**
     * 全局serverHolder对象
     */
    protected ServerHolder server = ServerHolder.INSTANCE;
    protected final AppendCommandPool appendCommandPool = AppendCommandPool.instance;
    protected final CommandPool commandPool = CommandPool.instance;
}
