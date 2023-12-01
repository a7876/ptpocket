package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

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
    public void copyTo(ByteBuffer buffer) {
        buffer.putDouble(num);
    }

    public static DataObject getFromDouble(ByteBuffer buffer) {
        return new DoubleDataObject(buffer.getDouble());
    }
}
