package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class IntDataObject extends DataObject {
    int num;

    public IntDataObject(int num) {
        this.num = num;
    }

    @Override
    public void populate(ByteBuf buf) {
        buf.writeInt(4); // body长度
        buf.writeInt(num);
    }

    @Override
    public int getInt() {
        return num;
    }

    @Override
    public int getUsed() {
        return 4; // always 4
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        buffer.putInt(num);
    }

    public static DataObject getFromInt(ByteBuffer buffer) {
        return new IntDataObject(buffer.getInt());
    }
}
