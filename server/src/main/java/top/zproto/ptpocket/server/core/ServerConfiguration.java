package top.zproto.ptpocket.server.core;

import java.io.File;

public class ServerConfiguration {
    static final String LOCAL_HOST = "localhost";
    String addr = LOCAL_HOST;
    int port = 7878;
    int IOThreads = 2;
    int timeOutLimit = 5; // 单位是分钟
    final int bossGroupThread = 1; // 主reactor线程数，固定是1
    int dbNums = 8; // 数据库数量
    int frequencyOfServerCron = 10; // 数据库定时任务频率，默认10hz
    String[] startArgs;
    boolean useAppendFile = true;
    boolean strongPersistenceSecurityRequired = false; // appendFile失败且重试之后再次失败直接退出
    long persistenceRetryInterval = 1000; // persistence失败之后的重试间隔,毫秒为单位
    String appendFileDirectory = "." + File.pathSeparator;
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

    public String getAppendFileDirectory() {
        return appendFileDirectory;
    }
}
