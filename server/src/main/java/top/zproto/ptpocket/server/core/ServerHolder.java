package top.zproto.ptpocket.server.core;

import java.util.HashSet;
import java.util.Set;

public class ServerHolder {
    // 服务器信息对象

    Database[] dbs; // 数据库
    Set<Client> clients; // 连接的客户

    boolean shutdown = false;

    long startTime;

    public static final ServerHolder INSTANCE = new ServerHolder();

    private ServerHolder() {
    }

    public void init(ServerConfiguration config) {
        dbs = new Database[config.dbNums];
        for (int i = 0; i < config.dbNums; i++) {
            dbs[i] = new Database(i);
        }
        startTime = System.currentTimeMillis();
        clients = new HashSet<>();
    }

}
