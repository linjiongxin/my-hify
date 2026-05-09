package com.hify.test.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hify 测试 MCP Server 启动类
 *
 * 提供订单查询、物流查询、退货申请等模拟工具，用于本地联调测试。
 *
 * @author hify
 */
@Slf4j
@SpringBootApplication
public class TestMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestMcpServerApplication.class, args);
        log.info("\n" +
                "----------------------------------------------------------\n" +
                "\tHify 测试 MCP Server 启动成功!\n" +
                "\t访问地址: http://localhost:3000/sse\n" +
                "----------------------------------------------------------");
    }
}
