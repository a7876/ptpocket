package top.zproto.ptpocket.client.core;

import io.netty.channel.Channel;

import java.util.List;

/**
 * 各类方法
 */
public class PocketOperation {
    private Client client;

    public boolean set(byte[] key, byte[] value) {
    }

    public byte[] get(byte[] key) {

    }

    public boolean hSet(byte[] key, byte[] innerKey, byte[] value) {

    }

    public byte[] hGet(byte[] key, byte[] innerKey) {
    }

    public boolean hDel(byte[] key, byte[] innerKey) {

    }

    public boolean zAdd(byte[] key, double score, byte[] value) {

    }

    public boolean zDel(byte[] key, byte[] value) {

    }

    public List<byte[]> zRange(byte[] key, int start, int end) {

    }

    public List<byte[]> zReverseRange(byte[] key, int start, int end) {

    }

    public List<byte[]> zRangeScore(byte[] key, double start, double end) {

    }

    public int zRank(byte[] key, byte[] value) {

    }

    public int zReverseRank(byte[] key, byte[] value) {

    }

    public double zScore(byte[] key, byte[] value) {
    }

    public boolean del(byte[] key) {

    }

    public boolean expire(int expireTime) {

    }

    public boolean expireMill(int expireTime) {

    }

    public boolean persist(byte[] key) {

    }

    public boolean stop() {

    }

    private Channel getChannel(){
        return client.getChannel();
    }
}
