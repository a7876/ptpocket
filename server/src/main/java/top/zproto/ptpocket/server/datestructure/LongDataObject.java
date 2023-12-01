package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * 专门为long实现的DataObject子类
 */
public class LongDataObject extends DataObject {
    long num;

    public LongDataObject(long num) {
        this.num = num;
    }

    @Override
    public void populate(ByteBuf buf) {
        buf.writeInt(8); // body长度
        buf.writeLong(num);
    }

    @Override
    public int getUsed() {
        return 8; // always 8
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        buffer.putLong(num);
    }

    public static DataObject getFromLong(ByteBuffer buffer) {
        return new LongDataObject(buffer.getLong());
    }

    @Override
    public long getLong() {
        return num;
    }
}
