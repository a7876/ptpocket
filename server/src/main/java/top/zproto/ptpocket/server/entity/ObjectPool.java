package top.zproto.ptpocket.server.entity;

public interface ObjectPool<E> {
    E getObject();

    void returnObject(E item);

    void tryShrink();
}
