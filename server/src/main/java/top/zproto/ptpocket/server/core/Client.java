package top.zproto.ptpocket.server.core;

import io.netty.channel.Channel;

/**
 * 代表一个数据库的客体，可以是网络连接客户，也会有伪客户端用于持久化
 */
public class Client {
    Channel channel;
    byte usedDb = -1;

    public int getUsedDb() {
        return usedDb;
    }

    public void setUsedDb(byte usedDb) {
        this.usedDb = usedDb;
    }

    boolean isFake = false;

    private volatile static Client fakeInstance;

    public static Client getFakeClient() {
        if (fakeInstance == null) {
            synchronized (Client.class) {
                if (fakeInstance == null) {
                    Client client = new Client();
                    client.isFake = true;
                    fakeInstance = client;
                    return client;
                }
                return fakeInstance;
            }
        }
        return fakeInstance;
    }
}
