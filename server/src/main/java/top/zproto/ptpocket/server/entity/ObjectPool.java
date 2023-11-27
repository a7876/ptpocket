package top.zproto.ptpocket.server.entity;

public interface ObjectPool<E, T extends ObjectPool<E, T>> {
    E getObject();

    void returnObject(E item);

    void tryShrink();
}
