package top.zproto.ptpocket.client.entity;

public class DataWrapper {
    byte[] bytes;
    double dNum;
    int iNum;

    byte bNum;

    public DataWrapper(byte[] bytes) {
        this.bytes = bytes;
    }

    public DataWrapper(double dNum) {
        this.dNum = dNum;
    }

    public DataWrapper(int iNum) {
        this.iNum = iNum;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public double getdNum() {
        return dNum;
    }

    public void setdNum(double dNum) {
        this.dNum = dNum;
    }

    public byte getbNum() {
        return bNum;
    }

    public void setbNum(byte bNum) {
        this.bNum = bNum;
    }

    public int getiNum() {
        return iNum;
    }

    public void setiNum(int iNum) {
        this.iNum = iNum;
    }
}
