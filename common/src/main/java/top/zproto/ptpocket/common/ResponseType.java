package top.zproto.ptpocket.common;

public interface ResponseType {
    /**
     * 只有一段body
     */
    byte DATA = 1; // 响应的是一段用户传来的数据
    byte NULL = 2; // 数据库响应NULL
    byte OK = 3; // 响应操作成功
    byte TRUE = 4; // 响应true
    byte FALSE = 5; // 响应false
    byte INT = 6; // 响应一个int
    byte DOUBLE = 7; // 响应一个double
    /**
     * body之后就是跟着一个个DataObject，都是先是DataObject的大小然后是DataObject
     */
    byte LIST = 8; // 响应一个列表
    byte UNKNOWN_COMMAND = 100; // 未知指令
    byte ILLEGAL_COMMAND = 101; // 不合法的指令
    byte DB_UNSELECTED = 102;
    byte CONNECT_RESET = (byte) 255; // 服务端断开连接
}
