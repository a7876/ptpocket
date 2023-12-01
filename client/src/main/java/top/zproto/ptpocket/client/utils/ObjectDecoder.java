package top.zproto.ptpocket.client.utils;

/**
 * 数据解码接口
 */
public interface ObjectDecoder<T> {
    T decode(byte[] bytes);
}
