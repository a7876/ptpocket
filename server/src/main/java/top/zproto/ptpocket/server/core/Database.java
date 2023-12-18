package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.datestructure.Hash;

/**
 * 一个个数据库实体，记录每个库的必要信息
 */
public class Database {
    Hash keyspace;
    Hash expire;
    byte number;
    public Database(byte number) {
        keyspace = new Hash(true);
        expire = new Hash(true);
        this.number = number;
    }

    public Hash getKeyspace() {
        return keyspace;
    }

    public Hash getExpire() {
        return expire;
    }

    public byte getNumber() {
        return number;
    }
}
