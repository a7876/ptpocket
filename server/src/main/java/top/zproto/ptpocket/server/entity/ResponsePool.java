package top.zproto.ptpocket.server.entity;

public class ResponsePool implements ObjectPool<ResponsePool>{
    // 对象池，获取响应对象
    @Override
    public ResponsePool getObject() {
        return null;
    }

    @Override
    public void returnObject() {

    }

    @Override
    public void tryShrink() {

    }
}
