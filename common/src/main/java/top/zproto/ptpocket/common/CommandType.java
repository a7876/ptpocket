package top.zproto.ptpocket.common;

public interface CommandType {
    // 数据结构指令
    byte SET = 0;
    byte GET = 1;
    byte H_SET = 10;
    byte H_GET = 11;
    byte Z_ADD = 20;

    // 通用指令
    byte DEL = (byte) 200; // 删除某个键
    byte EXPIRE = (byte) 199; // 设置过期，单位秒
    byte SELECT = (byte) 198; // 选择数据库
    byte EXPIRE_MILL = (byte) 197; // 设置过期，单位毫秒
    byte PERSIST = (byte) 196; // 取消过期

    // 特殊指令

    byte STOP = (byte) 255; // 停止redis

    // 保留指令
    byte RESERVED_0 = 100;
    byte RESERVED_1 = 101;
    byte RESERVED_2 = 102;

}
