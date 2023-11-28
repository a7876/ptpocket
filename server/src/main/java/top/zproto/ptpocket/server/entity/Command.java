package top.zproto.ptpocket.server.entity;

import top.zproto.ptpocket.server.core.Client;
import top.zproto.ptpocket.server.core.ServerCommandType;
import top.zproto.ptpocket.server.datestructure.DataObject;

public class Command {
    Client client;
    ServerCommandType commandType;
    DataObject[] dataObjects;

    public Command setClient(Client client) {
        this.client = client;
        return this;
    }

    public Command setCommandType(ServerCommandType commandType) {
        this.commandType = commandType;
        return this;
    }

    public void setDataObjects(DataObject[] dataObjects) {
        this.dataObjects = dataObjects;
    }

    public void clear() {
        client = null;
        commandType = null;
        dataObjects = null;
    }

    public void process() {
        commandType.processCommand(this);
    }

    public void returnObject() {
        CommandPool.instance.returnObject(this);
    }
}
