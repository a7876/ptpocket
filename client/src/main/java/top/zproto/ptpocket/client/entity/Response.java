package top.zproto.ptpocket.client.entity;

import java.util.List;

public class Response {
    boolean bRes;
    byte[] data;
    List<byte[]> datas;
    int iNum;
    double dNum;

    public boolean isbRes() {
        return bRes;
    }

    public void setbRes(boolean bRes) {
        this.bRes = bRes;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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

    public List<byte[]> getDatas() {
        return datas;
    }

    public void setDatas(List<byte[]> datas) {
        this.datas = datas;
    }
}
