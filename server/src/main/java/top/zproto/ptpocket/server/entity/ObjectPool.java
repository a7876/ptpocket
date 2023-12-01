package top.zproto.ptpocket.server.entity;

/**
 * 对象池接口
 */
public interface ObjectPool<E> {
    E getObject();

    void returnObject(E item);

    void tryShrink();
}
