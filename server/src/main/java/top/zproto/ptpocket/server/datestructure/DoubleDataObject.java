package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

public class DoubleDataObject extends DataObject {
    double num;

    public DoubleDataObject(double num) {
        this.num = num;
    }

    @Override
    public void populate(ByteBuf buf) {
        buf.writeInt(4); // body长度
        buf.writeDouble(num);
    }

    @Override
    public double getDouble() {
        return num;
    }

    @Override
    public int getUsed() {
        return 8; // always 8
    }

    @Override
    public void copyTo(byte[] bytes, int index) {
        long l = Double.doubleToLongBits(num);
        for (int i = 0; i < 8; i++) {
            bytes[index++] = (byte) (l >>> (i * 8) & 0xff);
        }
    }

    public static DataObject getFromDouble(byte[] bytes, int index) {
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res |= ((long) bytes[index++]) << i * 8;
        }
        return new DoubleDataObject(Double.longBitsToDouble(res));
    }
}
