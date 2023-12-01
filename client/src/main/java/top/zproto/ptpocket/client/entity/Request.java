package top.zproto.ptpocket.client.entity;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.client.core.ClientRequestType;


/**
 * 请求数据包装对象
 */
public class Request {
    private DataWrapper[] datas;
    private ClientRequestType type;

    public void convertTo(ByteBuf buf) {
        type.convert(this, buf);
    }

    public DataWrapper[] getDatas() {
        return datas;
    }

    public void setDatas(DataWrapper... datas) {
        this.datas = datas;
    }


    public void setType(ClientRequestType type) {
        this.type = type;
    }
}
