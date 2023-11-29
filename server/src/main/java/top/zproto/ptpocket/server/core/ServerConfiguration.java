package top.zproto.ptpocket.server.core;

public class ServerConfiguration {
    static final String LOCAL_HOST = "localhost";
    String addr = LOCAL_HOST;
    int port = 7878;
    int IOThreads = 2;
    int timeOutLimit = 5; // 单位是分钟
    int bossGroupThread = 1; // 主reactor线程数，固定是1
    int dbNums = 8; // 数据库数量
    int frequencyOfServerCron = 10; // 数据库定时任务频率，默认10hz
    String[] startArgs;
    private static volatile ServerConfiguration instance = null;

    static ServerConfiguration getConfig(String[] startArgs) {
        if (instance == null) {
            synchronized (ServerConfiguration.class) {
                if (instance == null) {
                    ServerConfiguration instance = new ServerConfiguration();
                    instance.startArgs = startArgs;
                    instance.readConfig();
                    ServerConfiguration.instance = instance;
                }
                return instance;
            }
        } else {
            return instance;
        }
    }

    void readConfig() {

    }
}
