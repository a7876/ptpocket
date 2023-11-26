package top.zproto.ptpocket.server.log;

public interface Logger {
    Logger DEFAULT = new StandardIOLogger();
    void warn(String msg);
    void info(String msg);
    void panic(String msg);
}
