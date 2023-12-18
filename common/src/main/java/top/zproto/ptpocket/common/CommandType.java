package top.zproto.ptpocket.common;

public interface CommandType {
    // 数据结构指令

    /**
     * body中前4位标记key的长度，body组成是keyLength + key + value
     */
    byte SET = 0;
    /**
     * body中直接只有一个key
     */
    byte GET = 1;
    /**
     * body中前四位标记key的长度，接着是key，然后四位标记key的长度，接着内层key，然后是value
     * keyLength + key + innerKeyLength + inner key + value
     */
    byte H_SET = 10;
    /**
     * body中前四位标记key的长度，接着是key，然后是内层key
     * keyLength + key + innerKey
     */
    byte H_GET = 11;
    /**
     * body中前四位标记key的长度，接着是key，然后是内层key
     * keyLength + key + innerKey
     */
    byte H_DEL = 12;
    /**
     * body中前四位标记key的长度，接着是key，再接着是8位double，然后是value
     * keyLength + key + double + value
     */
    byte Z_ADD = 20;
    /**
     * body中前四位标记key的长度，接着是key，然后是value
     * keyLength + key + value
     */
    byte Z_DEL = 21;
    /**
     * body中前四位标记key的长度，接着是key，然后是两个四位int
     * keyLength + key + int + int
     */
    byte Z_RANGE = 22;
    /**
     * body中前四位标记key的长度，接着是key，然后是两个四位int
     * keyLength + key + int + int
     */
    byte Z_REVERSE_RANGE = 23;
    /**
     * body中前四位标记key的长度，接着是key，然后是两个八位double
     * keyLength + key + double + double
     */
    byte Z_RANGE_SCORE = 24;
    /**
     * body中前四位标记key的长度，接着是key，然后是value
     * keyLength + key + value
     */
    byte Z_RANK = 25;
    /**
     * body中前四位标记key的长度，接着是key，然后是value
     * keyLength + key + value
     */
    byte Z_REVERSE_RANK = 26;
    /**
     * body中前四位标记key的长度，接着是key，然后是value
     * keyLength + key + value
     */
    byte Z_SCORE = 27;

    // 通用指令
    /**
     * body 只有一个key
     */
    byte DEL = (byte) 200; // 删除某个键
    /**
     * body 前四位标记key长度，接着是key，然后是四位int
     * keyLength + key + int
     */
    byte EXPIRE = (byte) 199; // 设置过期，单位秒
    /**
     * body 只有一个byte，是数据库编号
     */
    byte SELECT = (byte) 198; // 选择数据库
    /**
     * body 前四位标记key长度，接着是key，然后是四位int
     * keyLength + key + int
     */
    byte EXPIRE_MILL = (byte) 197; // 设置过期，单位毫秒
    /**
     * body 只有一个key
     */
    byte PERSIST = (byte) 196; // 取消过期

    // 特殊指令

    /**
     * 没有body
     */
    byte STOP = (byte) 255; // 停止redis

    byte INFO = 100; // 获取数据库的信息

    byte REWRITE = 101; // 阻塞从写AppendFile

    // 保留指令
    byte RESERVED_0 = 100;
    byte RESERVED_1 = 101;
    byte RESERVED_2 = 102;

}
