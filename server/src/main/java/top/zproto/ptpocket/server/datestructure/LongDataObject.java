package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

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
    public void copyTo(byte[] bytes, int index) {
        for (int i = 0; i < 8; i++) {
            bytes[index++] = (byte) (num >>> (i * 8) & 0xff);
        }
    }

    public static DataObject getFromLong(byte[] bytes, int index) {
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res |= ((long) bytes[index++]) << i * 8;
        }
        return new LongDataObject(res);
    }

    @Override
    public long getLong() {
        return num;
    }
}
