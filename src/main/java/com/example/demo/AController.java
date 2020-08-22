package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author dongw
 * @description: TODO
 * @date 2020/8/20 002020:48
 */
@RestController
public class AController {

    @Value("${demo.name}")
    private String str;

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10,20,10, TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));


    @GetMapping("/hello")
    public String hello(){

        IntStream.range(0,15).forEach((i)->{
            poolExecutor.execute(()->{
                try {
                    Thread.sleep(1000*10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

        return "hello ";
    }

    @GetMapping("/result")
    public String result(){
        int poolSize = poolExecutor.getPoolSize();
        long taskCount = poolExecutor.getTaskCount();
        int activeCount = poolExecutor.getActiveCount();
        long completedTaskCount = poolExecutor.getCompletedTaskCount();
        System.out.println(poolSize);
        System.out.println(activeCount);
        System.out.println(taskCount);
        System.out.println(completedTaskCount);
        return new StringBuffer("poolsize=").append(poolSize).append('\n')
                .append("activeCount=").append(activeCount).append('\n')
                .append("taskCount=").append(taskCount).append('\n')
                .append("completedTaskCount=").append(completedTaskCount).toString();
    }
}
