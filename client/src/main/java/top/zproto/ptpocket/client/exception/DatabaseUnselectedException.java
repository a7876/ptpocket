package top.zproto.ptpocket.client.exception;

/**
 * 未选择对于数据库异常
 */
public class DatabaseUnselectedException extends RuntimeException{
    public DatabaseUnselectedException(String message) {
        super(message);
    }
}
