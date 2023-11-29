package top.zproto.ptpocket.client.exception;

public class ConnectionResetException extends RuntimeException{
    public ConnectionResetException(String message) {
        super(message);
    }
}
