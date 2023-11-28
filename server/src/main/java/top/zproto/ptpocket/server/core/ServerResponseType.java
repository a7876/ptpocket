package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.common.ResponseType;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.entity.Response;

public enum ServerResponseType implements ResponseType, ResponseProcessor {
    DATA(ResponseType.DATA) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.DATA);
            response.getDataObjects()[0].populate(buf);
            response.returnObject();
        }
    }, NULL(ResponseType.NULL) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.NULL);
            noBody(buf);
            response.returnObject();
        }
    }, OK(ResponseType.OK) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.OK);
            noBody(buf);
            response.returnObject();
        }
    }, TRUE(ResponseType.TRUE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.TRUE);
            noBody(buf);
            response.returnObject();
        }
    }, FALSE(ResponseType.FALSE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.FALSE);
            noBody(buf);
            response.returnObject();
        }
    }, INT(ResponseType.INT) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.INT);
            buf.writeInt(4); // 写body长度
            buf.writeInt(response.getiNum());
            response.returnObject();
        }
    },
    DOUBLE(ResponseType.DOUBLE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.DOUBLE);
            buf.writeInt(4); // 写body长度
            buf.writeDouble(response.getdNum());
            response.returnObject();
        }
    }, LIST(ResponseType.LIST) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.LIST);
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
    }, UNKNOWN_COMMAND(ResponseType.UNKNOWN_COMMAND) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.UNKNOWN_COMMAND);
            noBody(buf);
            response.returnObject();
        }
    }, ILLEGAL_COMMAND(ResponseType.ILLEGAL_COMMAND) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.ILLEGAL_COMMAND);
            noBody(buf);
            response.returnObject();
        }
    }, CONNECT_RESET(ResponseType.CONNECT_RESET) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.CONNECT_RESET);
            noBody(buf);
            response.returnObject();
        }
    }, DB_UNSELECTED(ResponseType.DB_UNSELECTED) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {
            commonPart(buf);
            buf.writeByte(ResponseType.DB_UNSELECTED);
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
    }

    protected void noBody(ByteBuf buf) {
        buf.writeInt(0); // 没有body长度
    }
}
