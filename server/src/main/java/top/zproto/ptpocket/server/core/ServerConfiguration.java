package top.zproto.ptpocket.server.core;

public class ServerConfiguration {
    static final String LOCAL_HOST = "localhost";
    String addr = LOCAL_HOST;
    int port = 7878;
    int IOThreads = 2;
    int databases = 8;

    int timeOutLimit = 5; // 单位是分钟

    int bossGroupThread = 1; // 主reactor线程数，固定是1

    private static volatile ServerConfiguration instance = null;

    static ServerConfiguration getConfig() {
        if (instance == null) {
            synchronized (ServerConfiguration.class) {
                if (instance == null) {
                    instance = new ServerConfiguration();
                }
                return instance;
            }
        } else {
            return instance;
        }
    }
}
