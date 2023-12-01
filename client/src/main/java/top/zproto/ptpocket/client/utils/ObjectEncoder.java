package top.zproto.ptpocket.client.utils;

/**
 * 数据编码接口
 */
public interface ObjectEncoder<T> {
    byte[] encode(T o);
}
