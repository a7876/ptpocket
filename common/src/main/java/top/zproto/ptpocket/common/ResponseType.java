package top.zproto.ptpocket.common;

public interface ResponseType {
    byte STRING = 1; // 数据库回复的是一段字符串
    byte DATA = 2; // 响应的是一段用户传来的数据
    byte NULL = 3; // 数据库响应NULL
    byte OK = 4; // 响应操作成功
    byte TRUE = 5; // 响应true
    byte FALSE = 6; // 响应false
    byte CONNECT_RESET = 7; // 服务端断开连接
}
