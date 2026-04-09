package com.hify.common.web.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 *
 * @author hify
 */
@Slf4j
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("插入自动填充, metaObject={}", metaObject.getOriginalObject().getClass().getName());

        LocalDateTime now = LocalDateTime.now();

        // 创建时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        // 更新时间
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 逻辑删除标志
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);

        // TODO: 从 SecurityContext 获取当前用户 ID
        // this.strictInsertFill(metaObject, "createdBy", Long.class, getCurrentUserId());
        // this.strictInsertFill(metaObject, "updatedBy", Long.class, getCurrentUserId());
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("更新自动填充, metaObject={}", metaObject.getOriginalObject().getClass().getName());

        // 更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // TODO: 从 SecurityContext 获取当前用户 ID
        // this.strictUpdateFill(metaObject, "updatedBy", Long.class, getCurrentUserId());
    }

}
