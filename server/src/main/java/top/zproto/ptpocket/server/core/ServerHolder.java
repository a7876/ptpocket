package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.entity.Client;
import top.zproto.ptpocket.server.entity.Database;

import java.util.LinkedList;
import java.util.Set;

public class ServerHolder {
    // 服务器信息对象

    Database[] dbs; // 数据库
    Set<Client> clients; // 连接的客户

    boolean shutdown = false;

    long startTime = 0;

    public static final ServerHolder INSTANCE = new ServerHolder();

    private ServerHolder() {
    }

    public ServerHolder explicitNewInstance() {
        return new ServerHolder();
    }
}
