package com.hify.common.web.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.hify.common.web.handler.MybatisMetaObjectHandler;
import com.hify.common.web.handler.StringArrayTypeHandler;
import com.hify.common.web.handler.UUIDTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 *
 * @author hify
 */
@Configuration
@MapperScan("com.hify.**.mapper")
public class MybatisPlusConfig {

    /**
     * 插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

    /**
     * 自动填充配置
     */
    @Bean
    public GlobalConfig globalConfig(MybatisMetaObjectHandler metaObjectHandler) {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        return globalConfig;
    }

    /**
     * 注册全局 TypeHandler
     * <p>
     * 注册 PostgreSQL 特殊类型的 TypeHandler：
     * <ul>
     *   <li>UUIDTypeHandler         → PostgreSQL UUID</li>
     *   <li>StringArrayTypeHandler  → PostgreSQL TEXT[] / INT[] 等数组</li>
     * </ul>
     * <p>
     * 全局注册后，MyBatis-Plus 会自动为对应 Java 类型的字段选择对应 Handler，
     * 无需在每个字段上单独指定 {@code @TableField(typeHandler = ...)}
     */
    @Bean
    public TypeHandler<?>[] typeHandlers() {
        return new TypeHandler<?>[] {
            new UUIDTypeHandler(),
            new StringArrayTypeHandler()
        };
    }
}
