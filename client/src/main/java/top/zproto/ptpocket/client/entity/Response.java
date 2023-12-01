package top.zproto.ptpocket.client.entity;

import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * 响应数据包装对象
 */
public class Response {
    boolean bRes;
    byte[] data;
    List<byte[]> datas;
    int iNum;
    double dNum;
    String string;

    public boolean isbRes() {
        return bRes;
    }

    public Response setbRes(boolean bRes) {
        this.bRes = bRes;
        return this;
    }

    public String getString() {
        return string;
    }

    public Response setString(String string) {
        this.string = string;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public Response setData(ByteBuf buf) {
        if (buf == null) {
            this.data = null;
            return this;
        }
        int length = buf.readableBytes();
        this.data = new byte[length];
        buf.readBytes(this.data);
        return this;
    }

    public int getiNum() {
        return iNum;
    }

    public Response setiNum(int iNum) {
        this.iNum = iNum;
        return this;
    }

    public double getdNum() {
        return dNum;
    }

    public Response setdNum(double dNum) {
        this.dNum = dNum;
        return this;
    }

    public List<byte[]> getDatas() {
        return datas;
    }

    public Response setDatas(List<byte[]> datas) {
        this.datas = datas;
        return this;
    }
}
