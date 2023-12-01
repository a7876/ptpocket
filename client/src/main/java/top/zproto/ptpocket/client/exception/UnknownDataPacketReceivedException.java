package top.zproto.ptpocket.client.exception;

/**
 * 错误网络数据包异常
 */
public class UnknownDataPacketReceivedException extends RuntimeException{
    public UnknownDataPacketReceivedException(String message) {
        super(message);
    }
}
