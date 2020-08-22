package com.example.demo;


import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author dongw
 * @description: TODO
 * @date 2020/8/22 002219:23
 */
@ServerEndpoint("/webSocket/{taskCount}")
@Component
public class WebSocketServer {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static AtomicInteger onlineNum = new AtomicInteger();

    //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));

    private boolean start;

    //发送消息
    public void sendMessage(Session session, String message) throws IOException {
        if (session != null) {
            synchronized (session) {
                System.out.println("发送数据：" + message);
                session.getBasicRemote().sendText(message);
            }
        }
    }


    //建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "taskCount") String taskCount) {
        sessionPools.put(session.getId(), session);
        addOnlineCount();
        System.out.println("开始执行模拟任务===>");
        int task = Integer.parseInt(taskCount);
        try {
            IntStream.range(0, task).forEach((i) -> {
                poolExecutor.execute(() -> {
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            });
            sendMessage(session, "任务数" + taskCount + "创建成功！");
            process();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //关闭连接时调用
    @OnClose
    public void onClose(@PathParam(value = "sid") String taskCount,Session session) throws IOException {
        sessionPools.remove(session.getId());
        subOnlineCount();
        System.out.println(session.getId() + "=======>断开webSocket连接！");
        session.close();
    }

    //收到客户端信息
    @OnMessage
    public void onMessage(String message,Session session) throws IOException, InterruptedException {
        System.out.println("====================="+message+"============================");
        if (message.equals("start")) {
            start = true;
        } else {
            start = false;
        }
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private void process(){
        executorService.submit(()->{
            for (Session session : sessionPools.values()) {
                while (true) {
                    if(start){
                        int queueSize = poolExecutor.getQueue().size();
                        System.out.println("当前排队线程数：" + queueSize);
                        int activeCount = poolExecutor.getActiveCount();
                        System.out.println("当前活动线程数：" + activeCount);
                        long completedTaskCount = poolExecutor.getCompletedTaskCount();
                        System.out.println("执行完成线程数：" + completedTaskCount);
                        System.out.println("总线程数：" + poolExecutor.getTaskCount());

                        // 写回到客户端的格式为matrix=排队数|运行线程数|执行完数|总任务数|时间
                        LocalTime now = LocalTime.now().withNano(0);
                        String result = "matrix=" + queueSize +"|" +activeCount +"|" +completedTaskCount+"|"+poolExecutor.getTaskCount()+"|"+now;
                        try {
                            sendMessage(session, result);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    //错误时调用
    @OnError
    public void onError(Session session, Throwable throwable) throws IOException {
        System.out.println("发生错误");
        throwable.printStackTrace();
        session.close();
    }

    public static void addOnlineCount() {
        onlineNum.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineNum.decrementAndGet();
    }
}
