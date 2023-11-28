package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

public class IntDataObject extends DataObject {
    int num;

    public IntDataObject(int num) {
        this.num = num;
    }

    @Override
    public void populate(ByteBuf buf) {
        buf.writeInt(num);
    }

    @Override
    public int getInt() {
        return num;
    }
}
