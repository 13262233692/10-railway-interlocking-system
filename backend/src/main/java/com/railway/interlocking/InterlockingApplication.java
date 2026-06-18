package com.railway.interlocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 铁路联锁系统主启动类
 * Railway Interlocking System Main Application
 *
 * @author Railway Interlocking Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class InterlockingApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterlockingApplication.class, args);
        System.out.println("========================================");
        System.out.println("  铁路联锁系统启动成功!");
        System.out.println("  Railway Interlocking System Started!");
        System.out.println("  访问地址: http://localhost:8080/api");
        System.out.println("  WebSocket: ws://localhost:8080/api/ws/interlocking");
        System.out.println("========================================");
    }
}
