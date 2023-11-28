package top.zproto.ptpocket.server.core;

import top.zproto.ptpocket.server.entity.Command;

/**
 * 指令执行接口
 */
public interface CommandProcessor {
    void processCommand(Command command);
}
