package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.datestructure.Hash;

public class Database {
    Hash keyspace;
    Hash expire;
    int number;
    public Database(int number) {
        keyspace = new Hash(true);
        expire = new Hash(true);
        this.number = number;
    }
}
