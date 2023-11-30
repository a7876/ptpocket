package top.zproto.ptpocket.server.core;

import io.netty.channel.Channel;

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
