package top.zproto.ptpocket.server.datestructure;

import io.netty.buffer.ByteBuf;

public class DoubleDataObject extends DataObject {
    double num;

    public DoubleDataObject(double num) {
        this.num = num;
    }

    @Override
    public void populate(ByteBuf buf) {
        buf.writeDouble(num);
    }

    @Override
    public double getDouble() {
        return num;
    }
}
