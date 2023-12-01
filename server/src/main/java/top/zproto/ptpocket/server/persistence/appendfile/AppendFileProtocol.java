package top.zproto.ptpocket.server.persistence.appendfile;

/**
 * AppendFile协议
 */
public interface AppendFileProtocol {
    // append file 构成

    /*
     *  开头固定两个协议magicNumber
     * ---------------------------------------------------------
     * | dbnum| command| size | body (every part with its size)|
     * ---------------------------------------------------------
     * append file的命令需要和commandType配合以便之后能直接利用其执行命令重写
     * 此命令的特点是每次执行重写都要切换一次db
     * 其中size是标记后面body分为几个部分，每个开头部分都要记录自己的长度
     */

    int DB_NUM_LENGTH = 1; // dbnum 是1byte
    int COMMAND_LENGTH = 1; // commandLength 是1byte
    int SIZE_LENGTH = 1; // size 是1byte
    /**
     * part size length 实际只用28位，高4位需要用来标记需要反序列化为哪种DataObject
     */
    int PART_SIZE_LENGTH = 4; // 每部分长度是一个int，但不完全是
    int TYPE_MASK = 0xf0000000; // 高四位预留
    int NORMAL_DATA_OBJECT = 0; // 高四位全为零
    int INT_DATA_OBJECT = 0x10000000; // 高四位最低位为1
    int DOUBLE_DATA_OBJECT = 0x20000000; // 高四位倒数第二位为1
    int LONG_DATA_OBJECT = 0x40000000; // 高四位倒数第三位为1
}
