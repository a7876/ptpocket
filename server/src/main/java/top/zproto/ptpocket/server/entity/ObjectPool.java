package top.zproto.ptpocket.server.entity;

public interface ObjectPool<T extends ObjectPool<T>> {
    T getObject();

    void returnObject();

    void tryShrink();
}
