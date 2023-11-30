package top.zproto.ptpocket.client.utils;

import top.zproto.ptpocket.client.core.Client;
import top.zproto.ptpocket.client.core.PocketOperation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PocketTemplate<T> {
    private final ObjectEncoder<T> objectEncoder;
    private final ObjectDecoder<T> objectDecoder;
    private final PocketOperation operation;

    public PocketTemplate(Client client, ObjectEncoder<T> objectEncoder, ObjectDecoder<T> objectDecoder) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(objectEncoder);
        Objects.requireNonNull(objectDecoder);
        this.objectEncoder = objectEncoder;
        this.objectDecoder = objectDecoder;
        operation = PocketOperation.getInstance(client);
    }

    public PocketTemplate(Client client, ObjectEncoder<T> objectEncoder, ObjectDecoder<T> objectDecoder, byte defaultDB) {
        this(client, objectEncoder, objectDecoder);
        if (!this.select(defaultDB))
            throw new RuntimeException("default db selected failed");
    }

    public boolean set(T key, T value) {
        return operation.set(objectEncoder.encode(key), objectEncoder.encode(value));
    }

    public T get(T key) {
        return objectDecoder.decode(operation.get(objectEncoder.encode(key)));
    }

    public boolean hSet(T key, T innerKey, T value) {
        return operation.hSet(objectEncoder.encode(key),
                objectEncoder.encode(innerKey), objectEncoder.encode(value));
    }

    public T hGet(T key, T innerKey) {
        return objectDecoder.decode(operation.hGet(objectEncoder.encode(key), objectEncoder.encode(innerKey)));
    }

    public boolean hDel(T key, T innerKey) {
        return operation.hDel(objectEncoder.encode(key), objectEncoder.encode(innerKey));
    }

    public boolean zAdd(T key, double score, T value) {
        return operation.zAdd(objectEncoder.encode(key), score, objectEncoder.encode(value));
    }

    public boolean zDel(T key, T value) {
        return operation.zDel(objectEncoder.encode(key), objectEncoder.encode(value));
    }

    public List<T> zRange(T key, int start, int end) {
        return operation.zRange(objectEncoder.encode(key), start, end).stream()
                .map(objectDecoder::decode).collect(Collectors.toList());
    }

    public List<T> zReverseRange(T key, int start, int end) {
        return operation.zReverseRange(objectEncoder.encode(key), start, end).stream()
                .map(objectDecoder::decode).collect(Collectors.toList());
    }

    public List<T> zRangeScore(T key, double start, double end) {
        return operation.zRangeScore(objectEncoder.encode(key), start, end).stream()
                .map(objectDecoder::decode).collect(Collectors.toList());
    }

    public int zRank(T key, T value) {
        return operation.zRank(objectEncoder.encode(key), objectEncoder.encode(value));
    }

    public int zReverseRank(T key, T value) {
        return operation.zReverseRank(objectEncoder.encode(key), objectEncoder.encode(value));
    }

    public double zScore(T key, T value) {
        return operation.zScore(objectEncoder.encode(key), objectEncoder.encode(value));
    }

    public String info(){
        return operation.info();
    }
    public boolean select(byte dbNum){
        return operation.select(dbNum);
    }

    public boolean del(T key) {
        return operation.del(objectEncoder.encode(key));
    }

    public boolean expire(int expireTime) {
        return operation.expire(expireTime);
    }

    public boolean expireMill(int expireTime) {
        return operation.expireMill(expireTime);
    }

    public boolean persist(T key) {
        return operation.persist(objectEncoder.encode(key));
    }

    public boolean stop() {
        return operation.stop();
    }

}
