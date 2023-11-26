package top.zproto.ptpocket.server.entity;

public class CommandPool implements ObjectPool<CommandPool>{
    // 对象池，获取命令对象
    @Override
    public CommandPool getObject() {
        return null;
    }

    @Override
    public void returnObject() {

    }

    @Override
    public void tryShrink() {

    }
}
