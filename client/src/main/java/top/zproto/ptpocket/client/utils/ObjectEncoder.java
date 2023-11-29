package top.zproto.ptpocket.client.utils;

public interface ObjectEncoder<T> {
    byte[] encode(T o);
}
