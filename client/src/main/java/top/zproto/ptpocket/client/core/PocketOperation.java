package top.zproto.ptpocket.client.core;

import io.netty.channel.Channel;
import top.zproto.ptpocket.client.entity.DataWrapper;
import top.zproto.ptpocket.client.entity.Request;
import top.zproto.ptpocket.client.entity.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 对应各种指令方法
 */
public class PocketOperation implements Closeable {
    private Client client;

    private PocketOperation() {
    }

    public static PocketOperation getInstance(Client client) {
        if (client == null)
            throw new NullPointerException("client can't be null");
        PocketOperation pocketOperation = new PocketOperation();
        pocketOperation.client = client;
        return pocketOperation;
    }


    /**
     * 各类指令
     */
    public boolean set(byte[] key, byte[] value) {
        notEmpty(key, value);
        return commonPart(ClientRequestType.SET, fromByteArray(key), fromByteArray(value)).isbRes();
    }

    public byte[] get(byte[] key) {
        notEmpty(key);
        return commonPart(ClientRequestType.GET, fromByteArray(key)).getData();
    }

    public boolean hSet(byte[] key, byte[] innerKey, byte[] value) {
        notEmpty(key, innerKey, value);
        return commonPart(ClientRequestType.H_SET, fromByteArray(key),
                fromByteArray(innerKey), fromByteArray(value)).isbRes();
    }

    public byte[] hGet(byte[] key, byte[] innerKey) {
        notEmpty(key, innerKey);
        return commonPart(ClientRequestType.H_GET, fromByteArray(key), fromByteArray(innerKey)).getData();
    }

    public boolean hDel(byte[] key, byte[] innerKey) {
        notEmpty(key, innerKey);
        return commonPart(ClientRequestType.H_DEL, fromByteArray(key), fromByteArray(innerKey)).isbRes();
    }

    public boolean zAdd(byte[] key, double score, byte[] value) {
        notEmpty(key, value);
        return commonPart(ClientRequestType.Z_ADD, fromByteArray(key), fromDouble(score), fromByteArray(value)).isbRes();
    }

    public boolean zDel(byte[] key, byte[] value) {
        notEmpty(key, value);
        return commonPart(ClientRequestType.Z_DEL, fromByteArray(key), fromByteArray(value)).isbRes();
    }

    public List<byte[]> zRange(byte[] key, int start, int end) {
        notEmpty(key);
        return commonPart(ClientRequestType.Z_RANGE, fromByteArray(key), fromInt(start), fromInt(end)).getDatas();
    }

    public List<byte[]> zReverseRange(byte[] key, int start, int end) {
        notEmpty(key);
        return commonPart(ClientRequestType.Z_REVERSE_RANGE, fromByteArray(key), fromInt(start), fromInt(end)).getDatas();
    }

    public List<byte[]> zRangeScore(byte[] key, double start, double end) {
        notEmpty(key);
        return commonPart(ClientRequestType.Z_RANGE_SCORE, fromByteArray(key), fromDouble(start), fromDouble(end)).getDatas();
    }

    public int zRank(byte[] key, byte[] value) {
        notEmpty(key, value);
        return commonPart(ClientRequestType.Z_RANK, fromByteArray(key), fromByteArray(value)).getiNum();
    }

    public int zReverseRank(byte[] key, byte[] value) {
        notEmpty(key, value);
        return commonPart(ClientRequestType.Z_REVERSE_RANK, fromByteArray(key), fromByteArray(value)).getiNum();
    }

    public double zScore(byte[] key, byte[] value) {
        notEmpty(key, value);
        return commonPart(ClientRequestType.Z_SCORE, fromByteArray(key), fromByteArray(value)).getdNum();
    }

    public boolean del(byte[] key) {
        notEmpty(key);
        return commonPart(ClientRequestType.DEL, fromByteArray(key)).isbRes();
    }

    public boolean expire(int expireTime) {
        return commonPart(ClientRequestType.EXPIRE, fromInt(expireTime)).isbRes();
    }

    public boolean expireMill(int expireTime) {
        return commonPart(ClientRequestType.EXPIRE_MILL, fromInt(expireTime)).isbRes();
    }

    public boolean persist(byte[] key) {
        notEmpty(key);
        return commonPart(ClientRequestType.PERSIST, fromByteArray(key)).isbRes();
    }

    public boolean select(byte dbNum) {
        return commonPart(ClientRequestType.SELECT, fromByte(dbNum)).isbRes();
    }

    public boolean stop() {
        return commonPart(ClientRequestType.STOP).isbRes();
    }

    public String info() {
        return commonPart(ClientRequestType.INFO).getString();
    }

    private Channel getChannel() {
        return client.getChannel();
    }

    /**
     * 公共的处理部分
     */
    private Response commonPart(ClientRequestType type, DataWrapper... bytes) {
        Request request = new Request();
        request.setDatas(bytes);
        request.setType(type);
        getChannel().writeAndFlush(request);
        return client.getResponseSync();
    }

    private DataWrapper fromByteArray(byte[] bytes) {
        return new DataWrapper(bytes);
    }

    private DataWrapper fromInt(int num) {
        return new DataWrapper(num);
    }

    private DataWrapper fromDouble(double num) {
        return new DataWrapper(num);
    }

    private DataWrapper fromByte(byte b) {
        return new DataWrapper(b);
    }

    private void notEmpty(byte[]... bytes) {
        for (byte[] bs : bytes) {
            if (bs == null || bs.length == 0)
                throw new IllegalArgumentException("[byte can't be null or empty");
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
