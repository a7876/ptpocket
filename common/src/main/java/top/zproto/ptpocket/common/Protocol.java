package top.zproto.ptpocket.common;

public interface Protocol {
    /**
     * 定义协议的组成
     * <p>
     * ----------------------------------------------------------
     * | magic number | version | command | body length | body |
     * ----------------------------------------------------------
     */
    int MAGIC_NUM = 0xff7878ff; // 固定魔数
    byte MAGIC_NUM_LENGTH = 4; // 魔法数长度
    byte VERSION = 1;
    byte VERSION_LENGTH = 1; // 版本字段的字节数
    byte COMMAND_LENGTH = 1; // 命令字段的字节数，请求阶段协议使用
    byte RESPONSE_LENGTH = 1; // 回复字段的字节数，响应阶段协议使用
    byte BODY_LENGTH = 4; // 数据体长度的字节长度
    int BODY_LENGTH_LIMIT = 20 * 1024 * 1024; // 最长数据体允许20m
}
