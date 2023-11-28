package top.zproto.ptpocket.server.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.common.ResponseType;
import top.zproto.ptpocket.server.entity.Response;

public enum ServerResponseType implements ResponseType, ResponseProcessor {
    DATA(ResponseType.DATA) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, NULL(ResponseType.NULL) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, OK(ResponseType.OK) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, TRUE(ResponseType.TRUE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, FALSE(ResponseType.FALSE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, INT(ResponseType.INT) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    },
    DOUBLE(ResponseType.DOUBLE) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, LIST(ResponseType.LIST) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, UNKNOWN_COMMAND(ResponseType.UNKNOWN_COMMAND) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, ILLEGAL_COMMAND(ResponseType.ILLEGAL_COMMAND) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, CONNECT_RESET(ResponseType.CONNECT_RESET) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    }, DB_UNSELECTED(ResponseType.DB_UNSELECTED) {
        @Override
        public void processResponse(Response response, ByteBuf buf) {

        }
    };

    private final byte responseCode;

    ServerResponseType(byte responseCode) {
        this.responseCode = responseCode;
    }
}
