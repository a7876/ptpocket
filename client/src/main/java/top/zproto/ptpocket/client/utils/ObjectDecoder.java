package top.zproto.ptpocket.client.utils;

public interface ObjectDecoder<T> {
    T decode(byte[] bytes);
}
