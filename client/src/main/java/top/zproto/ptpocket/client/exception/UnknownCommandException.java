package top.zproto.ptpocket.client.exception;

/**
 * 无法解析的命令异常
 */
public class UnknownCommandException extends RuntimeException{
    public UnknownCommandException(String message) {
        super(message);
    }
}
