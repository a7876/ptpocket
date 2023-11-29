package top.zproto.ptpocket.client.exception;

public class UnknownCommandException extends RuntimeException{
    public UnknownCommandException(String message) {
        super(message);
    }
}
