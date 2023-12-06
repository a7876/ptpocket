package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

/**
 * 服务器启动配置类
 */
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
    boolean strongPersistenceSecurityRequired = true; // 不允许在不开启持久化的情况下运行
    String appendFileDirectory = "." + File.separator;
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

    /**
     * 读配置文件，有限读取启动参数第一个作为配置文件
     * 默认配置文件是在classpath下的ptpocket.properties
     */

    private static final String DEFAULT_PROPERTIES = "ptpocket.properties";

    private void readConfig() {
        Properties properties = new Properties();
        if (startArgs.length != 0) { // 默认第一个是启动配置文件
            String settingFilePath = startArgs[0];
            File file = new File(settingFilePath);
            if (!file.exists()) {
                Logger.DEFAULT.warn(String.format("path %s is invalided, try using default properties", settingFilePath));
            }
            try {
                properties.load(Files.newInputStream(file.toPath()));
                setConfig(properties);
            } catch (IOException e) {
                Logger.DEFAULT.warn(String.format("reading setting file %s error occurred, try using default properties", settingFilePath));
            }
        }
        try {
            properties.load(ClassLoader.getSystemResourceAsStream(DEFAULT_PROPERTIES));
            Logger.DEFAULT.info("reading default properties");
            setConfig(properties);
        } catch (IOException e) {
            Logger.DEFAULT.warn("reading default properties file failed, using default setting");
        } catch (NullPointerException ex) {
            Logger.DEFAULT.warn("can't find default properties, using default setting");
        }
    }

    private void setConfig(Properties properties) {
        try {
            addr = properties.getProperty("addr");
            port = Integer.parseInt(properties.getProperty("port"));
            IOThreads = Integer.parseInt(properties.getProperty("IOThreads"));
            if (IOThreads < 1 || IOThreads > 10)
                IOThreads = 2;
            timeOutLimit = Integer.parseInt(properties.getProperty("timeOutLimit"));
            if (timeOutLimit <= 0)
                timeOutLimit = Integer.MAX_VALUE;
            dbNums = Integer.parseInt(properties.getProperty("dbNums"));
            if (dbNums < 1 || dbNums > 1024)
                dbNums = 8;
            useAppendFile = Boolean.parseBoolean(properties.getProperty("useAppendFile"));
            strongPersistenceSecurityRequired = Boolean.parseBoolean(properties.getProperty("strongPersistenceSecurityRequired"));
            appendFileDirectory = properties.getProperty("appendFileDirectory");
        } catch (Throwable e) {
            Logger.DEFAULT.panic("some error occurred in reading setting properties, make sure it is correct, server exit!");
            System.exit(1);
        }
    }

    public String getAppendFileDirectory() {
        return appendFileDirectory;
    }
}
