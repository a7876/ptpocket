package top.zproto.ptpocket.client.core;

import io.netty.buffer.ByteBuf;
import top.zproto.ptpocket.client.entity.Response;
import top.zproto.ptpocket.client.exception.ConnectionResetException;
import top.zproto.ptpocket.client.exception.DatabaseUnselectedException;
import top.zproto.ptpocket.client.exception.IllegalCommandException;
import top.zproto.ptpocket.client.exception.UnknownCommandException;
import top.zproto.ptpocket.common.ResponseType;

import java.util.ArrayList;

public class ResponseConverter implements ResponseType {
    public static final ResponseConverter INSTANCE = new ResponseConverter();

    public Response convert(ByteBuf buf) {
        skipMagicNumber(buf);
        skipVersion(buf);
        byte command = getCommand(buf);
        skipBodyLength(buf);
        switch (command) {
            case DATA:
                return data(buf);
            case NULL:
                return nul();
            case OK:
                return ok();
            case TRUE:
                return tru();
            case FALSE:
                return fal();
            case INT:
                return inT(buf);
            case DOUBLE:
                return dou(buf);
            case LIST:
                return list(buf);
            case UNKNOWN_COMMAND:
                return unknown();
            case ILLEGAL_COMMAND:
                return illegal();
            case DB_UNSELECTED:
                return unselect();
            case CONNECT_RESET:
                return connectReset();
        }
        throw new RuntimeException("unknown response type");
    }

    public void skipMagicNumber(ByteBuf buf) {
        buf.readInt();
    }

    public void skipVersion(ByteBuf buf) {
        buf.readByte();
    }

    public void skipBodyLength(ByteBuf buf) {
        buf.readInt();
    }

    public byte getCommand(ByteBuf buf) {
        return buf.readByte();
    }

    public Response data(ByteBuf buf) {
        return new Response().setData(buf);
    }

    public Response nul() {
        return new Response().setData(null).setbRes(false);
    }

    public Response ok() {
        return new Response().setbRes(true);
    }

    public Response tru() {
        return new Response().setbRes(true);
    }

    public Response fal() {
        return new Response().setbRes(false);
    }

    public Response inT(ByteBuf buf) {
        return new Response().setiNum(buf.readInt());
    }

    public Response dou(ByteBuf buf) {
        return new Response().setdNum(buf.readDouble());
    }

    public Response list(ByteBuf buf) {
        Response response = new Response();
        ArrayList<byte[]> res = new ArrayList<>();
        while (buf.readableBytes() != 0) {
            int length = buf.readInt();
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            res.add(bytes);
        }
        response.setDatas(res);
        return response;
    }

    public Response unknown() {
        throw new UnknownCommandException("db can't process this command");
    }

    public Response illegal() {
        throw new IllegalCommandException("db reject process this command maybe type for the key mismatched " +
                "or illegal parameter");
    }

    public Response unselect() {
        throw new DatabaseUnselectedException("no database selected");
    }

    public Response connectReset() {
        throw new ConnectionResetException("db has reset the connection");
    }
}
