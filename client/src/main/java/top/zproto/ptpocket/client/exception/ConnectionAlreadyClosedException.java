package top.zproto.ptpocket.client.exception;

public class ConnectionAlreadyClosedException extends RuntimeException{
    public ConnectionAlreadyClosedException(String message) {
        super(message);
    }
}
