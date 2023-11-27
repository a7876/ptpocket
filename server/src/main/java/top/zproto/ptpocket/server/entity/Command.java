package top.zproto.ptpocket.server.entity;

import top.zproto.ptpocket.common.CommandType;
import top.zproto.ptpocket.server.core.Client;

public class Command implements CommandType {
    Client client;
    CommandType commandType;
}
