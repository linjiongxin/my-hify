package com.hify.rag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * RAG Demo 测试应用
 * <p>
 * 演示 pgvector 的完整使用流程：
 * 1. 创建表结构（通过 init SQL）
 * 2. 插入向量数据
 * 3. 相似度检索
 */
@SpringBootApplication
@ComponentScan(
    basePackages = {"com.hify.rag", "com.hify.common.web"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.hify\\.rag\\.controller\\..*"
    )
)
@MapperScan("com.hify.rag.mapper")
public class RagDemoApplication {
}
