package top.zproto.ptpocket.server.persistence.appendfile;

import sun.nio.ch.DirectBuffer;
import top.zproto.ptpocket.common.Protocol;
import top.zproto.ptpocket.server.core.ServerConfiguration;
import top.zproto.ptpocket.server.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

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
            int write = readingChannel.read(readBuf);
            readBuf.flip();
            return write > 0;
        }
        return true;
    }

    /**
     * 启动后台Append File
     */
    private synchronized void startBackGroundAppendFileTask() throws IOException {
        if (backgroundTaskThread != null)
            throw new IllegalStateException("backgroundTask is running");
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
            try {
                beforeAppendFile(); // 进行前置准备工作
                while (!stop) {
                    try {
                        consumeSuccessfully = false;
                        current = commands.take();
                        consumeSuccessfully = consume(file, current);
                        if (current != AppendCommand.FORCE_FLUSH) {
                            current.command.returnObject(); // 内层command要归还
                            current.returnObject(); // 外层appendCommand要归还
                        }
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
                        inRetry = false; // 已经retry过了
                        if (consumeSuccessfully) {
                            logger.warn("append file background task seems recovered");
                            panicOccurred = false; // 消除异常状态
                        }
                    }
                }
            } finally {
                beforeExit(file);
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

    private ByteBuffer writeBuf;
    private final static int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 1024; // 默认1M，不够再扩容

    private void beforeAppendFile() {
        writeBuf = ByteBuffer.allocateDirect(DEFAULT_WRITE_BUFFER_SIZE); // directed
    }

    private boolean consume(FileChannel fileChannel, AppendCommand command) throws IOException {
        if (command == AppendCommand.FORCE_FLUSH) {
            fileChannel.force(false); // 落盘刷新
            return true;
        }
        writeBuf.clear(); // always clear before use
        checkSize(command.calcSpace());
        command.toByteBuffer(writeBuf);
        writeBuf.flip(); // 切换至写模式
        fileChannel.write(writeBuf); // blocking mode should not short write
        return true; // always return true unless exception occurred
    }

    /**
     * 检查容量
     */
    private void checkSize(int neededSize) {
        int current = writeBuf.capacity();
        while (current < neededSize) current <<= 2; // 两倍去扩增
        if (current != writeBuf.capacity()) { // 需要扩容
            ((DirectBuffer) writeBuf).cleaner().clean(); // 清理
            writeBuf = ByteBuffer.allocateDirect(current); // 重新申请
        }
    }

    private void beforeExit(FileChannel fileChannel) { // 收尾工作
        if (!panicOccurred) {
            try {
                fileChannel.force(true); // 落盘
            } catch (IOException ignored) {
            }
        }
        // 清理直接内存
        ((DirectBuffer) writeBuf).cleaner().clean();
        writeBuf = null;
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

    private Thread backgroundTaskThread;

    @Override
    public void close() throws IOException {
        stop = true;
        if (backgroundTaskThread != null)
            backgroundTaskThread.interrupt(); // 中断阻塞
        backgroundTaskThread = null;
    }
}
