package top.zproto.ptpocket.client.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import top.zproto.ptpocket.client.entity.Response;
import top.zproto.ptpocket.client.exception.ConnectFailException;
import top.zproto.ptpocket.client.exception.ConnectionAlreadyClosedException;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * client 主类
 * 封装网络请求等信息
 * thread unsafe
 */
public class Client implements Closeable {
    private String ipAddr;
    private int port;
    private NioEventLoopGroup group;
    private static final int THREAD_LIMIT = 10;
    private ChannelFuture future;
    private Channel channel;
    /**
     * 阻塞队列用于阻塞客户等待服务器响应
     */
    private final LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>(); // 阻塞等待对方抵达

    public static final AttributeKey<Client> KEY = AttributeKey.newInstance("client");

    private boolean isClose = false;
    private Client shareFrom = null;
    private final AtomicInteger refCount = new AtomicInteger();

    private Client() {
    }

    public static Client getInstance(String ipAddr, int port) {
        return getInstance(ipAddr, port, 1);
    }

    public static Client getInstance(String ipAddr, int port, int thread) {
        if (thread < 1)
            throw new IllegalArgumentException("thread arg is illegal");
        Client client = new Client();
        client.ipAddr = ipAddr;
        client.port = port;
        client.group = new NioEventLoopGroup(Math.min(thread, THREAD_LIMIT));
        client.connect();
        client.refCount.incrementAndGet();
        return client;
    }


    /*
     * 共享底层netty线程池
     */
    public static Client getInstanceShared(String ipAddr, int port, Client client) {
        if (client == null)
            throw new NullPointerException("client can't be null");
        Client newClient = new Client();
        client.ipAddr = ipAddr;
        client.port = port;
        newClient.group = client.group;
        client.connect();
        newClient.shareFrom = client;
        client.refCount.incrementAndGet();
        return newClient;
    }

    private void connect() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProtocolSplitHandler());
                        ch.pipeline().addLast(new ResponseAcceptor());
                        ch.pipeline().addLast(new RequestSender());
                    }
                }).option(ChannelOption.TCP_NODELAY, true); // 关闭Nagle
        future = bootstrap.connect(new InetSocketAddress(ipAddr, port));
        future.addListener(f -> {
            if (f.isSuccess()) {
                channel = ((ChannelFuture) f).channel();
                channel.attr(KEY).set(this);
            } else {
                throw new ConnectFailException("client connect failed", f.cause());
            }
        });
    }

    public Channel getChannel() {
        if (isClose)
            throw new ConnectionAlreadyClosedException("connection already closed, maybe idle to long");
        if (channel != null)
            return channel;
        try {
            future.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return future.channel();
    }


    public Response getResponseSync() {
        while (true) {
            try {
                return queue.take();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void offerResponse(Response response) {
        queue.offer(response);
    }

    @Override
    public void close() throws IOException {
        isClose = true;
        channel.close();
        if (shareFrom != null){
            int i = shareFrom.refCount.decrementAndGet();
            if (i == 0)
                group.shutdownGracefully();
        }
        int i = this.refCount.decrementAndGet();
        if (i == 0)
            group.shutdownGracefully();
    }
}
