package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.common.ResponseType;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.entity.Response;

/**
 * 响应处理枚举
 * 所有响应行为全部定义在此
 */
public enum ServerResponseType implements ResponseType, ResponseProcessor {
    DATA(ResponseType.DATA) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            response.getDataObjects()[0].populate(buf);
            response.returnObject();
        }
    }, NULL(ResponseType.NULL) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, OK(ResponseType.OK) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, TRUE(ResponseType.TRUE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, FALSE(ResponseType.FALSE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, INT(ResponseType.INT) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeInt(4); // 写body长度
            buf.writeInt(response.getiNum());
            response.returnObject();
        }
    },
    DOUBLE(ResponseType.DOUBLE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeInt(8); // 写body长度
            buf.writeDouble(response.getdNum());
            response.returnObject();
        }
    }, LIST(ResponseType.LIST) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            int length = 0;
            DataObject[] dataObjects = response.getDataObjects();
            for (DataObject dataObject : dataObjects)
                length += dataObject.getUsed(); // 累加每个DataObject的大小
            length += dataObjects.length * 4; // 还要加上记录每个的大小Int的长度
            buf.writeInt(length); // 写body长度
            for (DataObject dataObject : dataObjects)
                dataObject.populate(buf);
            response.returnObject();
        }
    }, STRING(ResponseType.STRING) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            byte[] bytes = response.getString().getBytes();
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
            response.returnObject();
        }
    }, UNKNOWN_COMMAND(ResponseType.UNKNOWN_COMMAND) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, ILLEGAL_COMMAND(ResponseType.ILLEGAL_COMMAND) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, CONNECT_RESET(ResponseType.CONNECT_RESET) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    }, DB_UNSELECTED(ResponseType.DB_UNSELECTED) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            noBody(buf);
            response.returnObject();
        }
    };

    final byte responseCode;

    ServerResponseType(byte responseCode) {
        this.responseCode = responseCode;
    }

    protected void commonPart(ByteBuf buf) { // 装配相同部分
        buf.writeInt(Protocol.MAGIC_NUM);
        buf.writeByte(Protocol.VERSION);
        buf.writeByte(this.responseCode);
    }

    protected void noBody(ByteBuf buf) {
        buf.writeInt(0); // 没有body长度
    }
}
