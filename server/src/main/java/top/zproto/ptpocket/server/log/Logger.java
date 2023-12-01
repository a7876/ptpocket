package top.zproto.ptpocket.server.log;

/**
 * 日志接口
 */
public interface Logger {
    Logger DEFAULT = new StandardIOLogger();

    void warn(String msg);

    void info(String msg);

    void panic(String msg);

    void print(String msg);
}
