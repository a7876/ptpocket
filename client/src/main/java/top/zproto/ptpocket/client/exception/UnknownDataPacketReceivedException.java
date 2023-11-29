package top.zproto.ptpocket.client.exception;

public class UnknownDataPacketReceivedException extends RuntimeException{
    public UnknownDataPacketReceivedException(String message) {
        super(message);
    }
}
