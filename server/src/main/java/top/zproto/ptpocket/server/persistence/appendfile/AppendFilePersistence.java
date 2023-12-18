package top.zproto.ptpocket.server.persistence.appendfile;

import sun.nio.ch.DirectBuffer;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.server.core.*;
import top.zproto.ptpocket.server.datestructure.DataObject;
import top.zproto.ptpocket.server.datestructure.DoubleDataObject;
import top.zproto.ptpocket.server.datestructure.Hash;
import top.zproto.ptpocket.server.datestructure.SortedSet;
import top.zproto.ptpocket.server.entity.Command;
import top.zproto.ptpocket.server.entity.CommandPool;
import top.zproto.ptpocket.server.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Append File实现类
 * 此类包含了读取Append File装载数据库的功能和写入Append File的功能
 */
public class AppendFilePersistence implements Closeable, AppendFileProtocol {
    private final BlockingDeque<AppendCommand> commands = new LinkedBlockingDeque<>();
    private final Logger logger = Logger.DEFAULT;
    private static final String FILE_NAME = "ptpocket.af";

    private final String directory;

    public AppendFilePersistence(ServerConfiguration configuration) {
        directory = configuration.getAppendFileDirectory();
    }

    /**
     * 传递持久化Command
     */
    public void deliver(AppendCommand command) {
        commands.offerLast(command);
    }

    /**
     * 开始从磁盘转载数据
     */
    public void startReload(boolean startAppendFile) throws IOException {
        if (!Paths.get(directory).toFile().isDirectory()) {
            throw new IOException("append file directory is not a directory");
        }
        Path path = Paths.get(directory + FILE_NAME);
        if (path.toFile().exists()) {
            doReload(FileChannel.open(path, StandardOpenOption.READ));
            logger.info("successfully reload all data from disk");
        } else {
            logger.warn("no append file found, ptpocket start with empty data");
        }
        if (startAppendFile)
            startBackGroundAppendFileTask();
    }

    private static final int DEFAULT_READ_BUFFER_SIZE = Protocol.BODY_LENGTH_LIMIT + 4; // 确保至少能满足一个getBytes的最大量
    private ByteBuffer readBuf;
    private FileChannel readingChannel;

    /**
     * 执行持久化数据读取
     */
    private void doReload(FileChannel fileChannel) throws IOException {
        try {
            readingChannel = fileChannel;
            readBuf = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
            fileChannel.read(readBuf);
            readBuf.flip(); // 初次读取
            checkFileStart();
            while (hasNext()) { // 只要还有条目就执行
                AppendCommand.reload(this);
            }
        } catch (RuntimeException ex) {
            logger.warn(ex.toString());
            throw new IOException("error occurred in reading append file");
        } finally {
            readingChannel = null;
            readBuf = null; // 清理
            fileChannel.close();
        }
    }

    private void checkFileStart() throws IOException {
        int first = getInt();
        if (first != Protocol.MAGIC_NUM)
            throw new IOException("append file has illegal file start, check the file");
        first = getInt();
        if (first != Protocol.MAGIC_NUM)
            throw new IOException("append file has illegal file start, check the file");
    }

    byte getByte() throws IOException {
        if (readBuf.hasRemaining()) {
            return readBuf.get();
        } else {
            readBuf.clear();
            readingChannel.read(readBuf);
            readBuf.flip();
        }
        if (readBuf.hasRemaining()) {
            return readBuf.get();
        } else {
            throw new IOException("file end unexpected");
        }
    }

    int getInt() throws IOException {
        if (readBuf.remaining() >= 4) {
            return readBuf.getInt();
        } else {
            readBuf.compact();
            readingChannel.read(readBuf);
            readBuf.flip();
        }
        if (readBuf.remaining() >= 4) {
            return readBuf.getInt();
        } else {
            throw new IOException("file end unexpected");
        }
    }

    void dropBytes(int length) throws IOException {
        if (readBuf.remaining() >= length) {
            readBuf.position(readBuf.position() + length);
        } else {
            readBuf.compact();
            readingChannel.read(readBuf);
            readBuf.flip();
        }
        if (readBuf.remaining() >= length) {
            readBuf.position(readBuf.position() + length);
        } else {
            throw new IOException("file end unexpected");
        }
    }

    ByteBuffer getReadBuffer(int requiredLength) throws IOException {
        if (readBuf.remaining() >= requiredLength) {
            return readBuf;
        } else {
            readBuf.compact();
            readingChannel.read(readBuf);
            readBuf.flip();
        }
        if (readBuf.remaining() >= requiredLength) {
            return readBuf;
        } else {
            throw new IOException("file end unexpected");
        }
    }

    boolean hasNext() throws IOException {
        if (!readBuf.hasRemaining()) {
            readBuf.clear();
            int read = readingChannel.read(readBuf);
            readBuf.flip();
            return read > 0;
        }
        return true;
    }

    /**
     * 启动后台Append File
     */
    private synchronized void startBackGroundAppendFileTask() throws IOException {
        if (backgroundTaskThread != null)
            throw new IllegalStateException("backgroundTask is running");
        stop = false;
        Path path = Paths.get(directory + FILE_NAME);
        FileChannel file;
        if (path.toFile().exists()) { // 存在
            file = FileChannel.open(path, StandardOpenOption.APPEND);
        } else { // 不存在则新建
            file = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            writeFileStart(file); // 写入文件头
        }
        Runnable runnable = () -> {
            AppendCommand current = null; // 保存当前的命令
            boolean consumeSuccessfully = false; // 记录是否成功消费
            ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_WRITE_BUFFER_SIZE);
            try {
                while (!stop) {
                    try {
                        consumeSuccessfully = false;
                        current = commands.take();
                        if (current == AppendCommand.FORCE_FLUSH) {
                            file.force(false); // 落盘刷新
                        } else {
                            buffer = checkResizeForDirectBuffer(buffer, current.calcSpace());
                            buffer.clear(); // always clear before use
                            current.toByteBuffer(buffer);
                            buffer.flip(); // 切换至写模式
                            file.write(buffer);// 阻塞模式不考虑短写问题  blocked mode should not short write
                            current.command.returnObject(); // 内层command要归还
                            current.returnObject(); // 外层appendCommand要归还
                        }
                        consumeSuccessfully = true;
                    } catch (IOException ex) {
                        logger.panic("background append file task failed!");
                        stop = true; // 退出线程
                        panicOccurred = true; // 记录异常退出状态
                    } catch (InterruptedException ignored) { // 忽略中断
                    } catch (Throwable throwable) {
                        logger.warn("something unexpected occurred in background Task");
                        logger.warn(throwable.toString());
                    }
                    if (current != null && !consumeSuccessfully) { // 没有消费成功需要放回
                        commands.offerFirst(current);
                    }
                    if (panicOccurred) {
                        if (consumeSuccessfully) {
                            logger.warn("append file background task seems recovered");
                            panicOccurred = false; // 消除异常状态
                        }
                        inRetry = false; // 已经retry过了
                    }
                }
            } finally {
                beforeExit(file, buffer);
                try {
                    file.close();
                } catch (IOException ignored) {
                }
            }
        };
        Thread thread = new Thread(runnable);
        backgroundTaskThread = thread;
        thread.start();
        logger.info("background append file task started");
    }

    /**
     * 重试后台 append file task
     */
    private volatile boolean inRetry = false;

    public void retryBackGroundTask() throws IOException {
        inRetry = true;
        startBackGroundAppendFileTask();
    }

    private final static int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 1024; // 默认1M，不够再扩容

    /**
     * 检查容量
     * 不足则自动以两倍去扩容
     */

    private ByteBuffer checkResizeForDirectBuffer(ByteBuffer buffer, int neededSize) {
        int current = buffer.capacity();
        while (current < neededSize) current <<= 1; // 两倍去扩增
        if (current != buffer.capacity()) { // 需要扩容
            ((DirectBuffer) buffer).cleaner().clean();
            buffer = ByteBuffer.allocateDirect(current);
        }
        return buffer;
    }

    private void beforeExit(FileChannel fileChannel, ByteBuffer buffer) { // 收尾工作
        if (!panicOccurred) {
            try {
                fileChannel.force(true); // 落盘
            } catch (IOException ignored) {
            }
        }
        // 清理直接内存
        ((DirectBuffer) buffer).cleaner().clean();
    }

    private void writeFileStart(FileChannel fileChannel) throws IOException {
        ByteBuffer temp = ByteBuffer.allocate(8);
        temp.putInt(Protocol.MAGIC_NUM);
        temp.putInt(Protocol.MAGIC_NUM);
        temp.flip();
        fileChannel.write(temp);
        temp.clear();
    }

    private volatile boolean stop = false;
    private volatile boolean panicOccurred = false;

    public boolean failedForPanic() {
        return !inRetry && panicOccurred; // 如果在retry中就返回false
    }

    private volatile Thread backgroundTaskThread;

    @Override
    public synchronized void close() throws IOException {
        if (stop)
            return;
        stop = true;
        if (backgroundTaskThread != null) {
            backgroundTaskThread.interrupt(); // 中断阻塞
        }
        backgroundTaskThread = null;
    }


    /*
     *
     * rewrite implementation  Append file rewrite实现
     *
     * */


    public boolean rewriteAppendFile(ServerHolder server, boolean background) {
        if (stateOfRewrite != NO_REWRITE) // 有正在执行的重写
            return false;
        if (background) {
            logger.warn("unsupported yet");
        } else {
            try {
                blockingRewrite(server);
                return true;
            } catch (Exception ex) {
                logger.warn(ex.getMessage());
            }
        }
        return false;
    }

    private void doRewrite(ServerHolder serverHolder, LinkedBlockingQueue<AppendCommand> queue) {
        Database[] dbs = serverHolder.getDbs();
        Client fakeClient = new Client();
        for (Database db : dbs) {
            final Hash keySpace = db.getKeyspace();
            final Hash expire = db.getExpire();
            fakeClient.setUsedDb(db.getNumber()); // 只需要设置一次
            keySpace.iterate((key, value) -> {
                if (value instanceof DataObject) {
                    Command command = CommandPool.instance.getObject();
                    command.setClient(fakeClient);
                    command.setCommandType(ServerCommandType.SET);
                    command.setDataObjects(new DataObject[]{key, (DataObject) value});
                    AppendCommand appendCommand = AppendCommandPool.instance.getObject();
                    appendCommand.setCommand(command);
                    queue.offer(appendCommand);
                } else if (value instanceof Hash) {
                    rewriteInnerHash(fakeClient, (Hash) value, queue, key);
                } else if (value instanceof SortedSet) {
                    rewriteSortedSet(fakeClient, (SortedSet) value, queue, key);
                } else {
                    throw new RuntimeException("unknown type of value");
                }
            });
            expire.iterate((key, value) -> {
                Double expireTime = (Double) value; // 转化时间类型
                Command command = CommandPool.instance.getObject();
                command.setCommandType(ServerCommandType.EXPIRE_MILL);
                command.setClient(fakeClient);
                command.setDataObjects(new DataObject[]{key, new DoubleDataObject(expireTime)});
                AppendCommand appendCommand = AppendCommandPool.instance.getObject();
                appendCommand.setCommand(command);
                queue.offer(appendCommand);
            });
        }
    }

    private void rewriteInnerHash(final Client fakeClient, final Hash hash,
                                  final LinkedBlockingQueue<AppendCommand> queue, final DataObject key) {
        hash.iterate((k, v) -> {
            // 这里的v一定是DataObject
            Command command = CommandPool.instance.getObject();
            command.setClient(fakeClient);
            command.setCommandType(ServerCommandType.H_SET);
            command.setDataObjects(new DataObject[]{key, k, (DataObject) v});
            AppendCommand appendCommand = AppendCommandPool.instance.getObject();
            appendCommand.setCommand(command);
            queue.offer(appendCommand);
        });
    }

    private void rewriteSortedSet(final Client fakeClient, final SortedSet sortedSet,
                                  final LinkedBlockingQueue<AppendCommand> queue, final DataObject key) {
        sortedSet.iterate((k, v) -> {
            // 这里的v一定是Double
            Command command = CommandPool.instance.getObject();
            command.setClient(fakeClient);
            command.setCommandType(ServerCommandType.Z_ADD);
            command.setDataObjects(new DataObject[]{key, new DoubleDataObject((Double) v), k});
            AppendCommand appendCommand = AppendCommandPool.instance.getObject();
            appendCommand.setCommand(command);
            queue.offer(appendCommand);
        });
    }


    /**
     * 0 表示没有执行重写
     * 1 表示正在执行重写
     * 2 表示重写完成
     * 3 表示重写失败
     */
    private volatile int stateOfRewrite = 0;

    private static final int NO_REWRITE = 0;
    private static final int REWRITING = 1;
    private static final int REWRITE_FINISH = 2;
    private static final int REWRITE_FAILED = 3;

    private static final AppendCommand POSITION = new AppendCommand();

    /**
     * 执行阻塞重写，主线程会阻塞等待
     */
    private void blockingRewrite(ServerHolder server) {
        String tmpFileName;
        do {
            tmpFileName = "ptpocket-rwtmp-" + UUID.randomUUID(); // 获取随机文件ID
            tmpFileName = directory + tmpFileName;
        } while (Files.exists(Paths.get(tmpFileName)));
        boolean needToDelete = true; // 标记是否要删除文件
        try (FileChannel tmpFile = FileChannel.open(Paths.get(directory + tmpFileName),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            logger.info("starting rewrite!");
            long startTime = System.currentTimeMillis();
            LinkedBlockingQueue<AppendCommand> queue = new LinkedBlockingQueue<>();
            stateOfRewrite = REWRITING; // 标记开始
            Runnable r = () -> { // 专门的后台写线程
                boolean failed = false;
                ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_WRITE_BUFFER_SIZE);
                try {
                    writeFileStart(tmpFile); // 文件头
                    while (true) {
                        AppendCommand appendCommand = queue.take();
                        if (appendCommand == POSITION) // 结束
                            return;
                        buffer = checkResizeForDirectBuffer(buffer, appendCommand.calcSpace());
                        appendCommand.toByteBuffer(buffer);
                        buffer.flip();
                        tmpFile.write(buffer);
                        buffer.clear();
                    }
                } catch (Throwable ignore) {
                    failed = true;
                } finally {
                    stateOfRewrite = failed ? REWRITE_FAILED : REWRITE_FINISH;
                    ((DirectBuffer) buffer).cleaner().clean(); // 清理内存
                }
            };
            Thread thread = new Thread(r);
            thread.start();
            doRewrite(server, queue); // 执行扫描重写
            queue.offer(POSITION); // 标记结束
            while (stateOfRewrite < REWRITE_FINISH) Thread.yield(); // 阻塞等待结果
            if (stateOfRewrite == REWRITE_FINISH) {
                tmpFile.close();
                afterBlockingRewriteFinish(tmpFileName, startTime); // 重命名append file
                needToDelete = false;
            } else {
                throw new RuntimeException("rewrite failed");
            }
        } catch (Throwable throwable) {
            if (needToDelete)
                try {
                    Files.delete(Paths.get(tmpFileName)); // 因为各种失败的原因，删除此临时文件
                } catch (Throwable t) {
                    throw new RuntimeException("error occurred in blocking rewrite \n" + t.getMessage());
                }
            throw new RuntimeException("error occurred in blocking rewrite \n" + throwable.getMessage());
        } finally {
            stateOfRewrite = NO_REWRITE; // 修改标记
        }
    }

    private void afterBlockingRewriteFinish(String tmpFileName, long startTime) throws IOException {
        Thread taskThread = backgroundTaskThread;
        if (taskThread != null) {
            close(); // 关闭后台线程
            commands.clear(); // 清理队列
            while (taskThread.isAlive()) Thread.yield(); // 等待后台线程退出
        }
        Files.move(Paths.get(tmpFileName), Paths.get(directory + FILE_NAME),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        // 重新开始后台appendFile
        if (taskThread != null)
            startBackGroundAppendFileTask(); // 重启后台appendFile线程
        logger.info(String.format("rewrite finish time used %d ms", System.currentTimeMillis() - startTime));
    }

    public boolean isRewriting() {
        return stateOfRewrite != NO_REWRITE;
    }
}
