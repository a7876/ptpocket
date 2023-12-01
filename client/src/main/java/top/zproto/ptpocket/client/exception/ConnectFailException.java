package top.zproto.ptpocket.client.exception;

/**
 * 连接失败异常
 */
public class ConnectFailException extends RuntimeException{
    public ConnectFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
