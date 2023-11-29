package top.zproto.ptpocket.client.exception;

public class ConnectFailException extends RuntimeException{
    public ConnectFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
