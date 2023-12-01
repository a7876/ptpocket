package top.zproto.ptpocket.client.exception;

/**
 * 错误的命令异常（可能是参数不匹配）
 */
public class IllegalCommandException extends RuntimeException{
    public IllegalCommandException(String message) {
        super(message);
    }
}
