package top.zproto.ptpocket.server.entity;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.server.core.Client;
import top.zproto.ptpocket.server.core.ServerResponseType;
import top.zproto.ptpocket.server.datestructure.DataObject;

// 响应类
public class Response {
    private static final ResponsePool pool = ResponsePool.instance;

    Client client;
    DataObject[] dataObjects;
    String string;
    ServerResponseType responseType;
    int iNum;
    double dNum;

    public void clear() {
        client = null;
        dataObjects = null;
        string = null;
        responseType = null;
        iNum = 0;
        dNum = 0;
    }

    public Response setClient(Client client) {
        this.client = client;
        return this;
    }

    public Response setDataObjects(DataObject... dataObjects) {
        this.dataObjects = dataObjects;
        return this;
    }

    public Response setString(String string) {
        this.string = string;
        return this;
    }

    public Response setResponseType(ServerResponseType responseType) {
        this.responseType = responseType;
        return this;
    }

    public int getiNum() {
        return iNum;
    }

    public void setiNum(int iNum) {
        this.iNum = iNum;
    }

    public double getdNum() {
        return dNum;
    }

    public void setdNum(double dNum) {
        this.dNum = dNum;
    }

    public Client getClient() {
        return client;
    }

    public DataObject[] getDataObjects() {
        return dataObjects;
    }

    public String getString() {
        return string;
    }

    public void returnObject() {
        pool.returnObject(this);
    }

    public void respond(ByteBuf buf) {
        responseType.processResponse(this, buf);
    }

    public static Response getObject() {
        return pool.getObject();
    }
}
