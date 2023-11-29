package top.zproto.ptpocket.server.log;

public class StandardIOLogger implements Logger {
    private static final String WARN_PREFIX = "warn: ";
    private static final String INFO_PREFIX = "info: ";
    private static final String PANIC_PREFIX = "panic: ";

    @Override
    public void warn(String msg) {
        System.out.println(WARN_PREFIX + msg);
    }

    @Override
    public void info(String msg) {
        System.out.println(INFO_PREFIX + msg);
    }

    @Override
    public void panic(String msg) {
        System.err.println(PANIC_PREFIX + msg);
    }

    @Override
    public void print(String msg) {
        System.out.println(msg);
    }
}
