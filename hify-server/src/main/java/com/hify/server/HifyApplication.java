package com.hify.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Hify 应用启动类
 *
 * @author hify
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.hify")
@MapperScan("com.hify.**.mapper")
public class HifyApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(HifyApplication.class, args);

        Environment env = context.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8080");
        String path = env.getProperty("server.servlet.context-path", "");

        log.info("\n" +
                "----------------------------------------------------------\n" +
                "\tHify 应用启动成功!\n" +
                "----------------------------------------------------------\n" +
                "\t本地访问:   http://localhost:{}{}\n" +
                "\t外部访问:   http://{}:{}{}\n" +
                "\tAPI文档:    http://{}:{}{}/doc.html\n" +
                "----------------------------------------------------------",
                port, path,
                ip, port, path,
                ip, port, path);
    }

}
