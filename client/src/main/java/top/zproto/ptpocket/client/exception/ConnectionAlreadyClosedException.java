package top.zproto.ptpocket.client.exception;

/**
 * 连接已经关闭异常
 */
public class ConnectionAlreadyClosedException extends RuntimeException{
    public ConnectionAlreadyClosedException(String message) {
        super(message);
    }
}
