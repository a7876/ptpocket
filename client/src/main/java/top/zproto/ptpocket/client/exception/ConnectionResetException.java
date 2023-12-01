package top.zproto.ptpocket.client.exception;

/**
 * 服务器要求重设连接异常
 */
public class ConnectionResetException extends RuntimeException{
    public ConnectionResetException(String message) {
        super(message);
    }
}
