package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

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
    public void copyTo(byte[] bytes, int index) {
        for (int i = 0; i < 4; i++) {
            bytes[index++] = (byte) (num >>> (i * 8) & 0xff);
        }
    }

    public static DataObject getFromInt(byte[] bytes, int index) {
        int res = 0;
        for (int i = 0; i < 4; i++) {
            res |= bytes[index++] << i * 8;
        }
        return new IntDataObject(res);
    }
}
