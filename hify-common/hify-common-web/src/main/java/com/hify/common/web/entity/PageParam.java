package com.hify.common.web.entity;

import com.hify.common.core.constants.CommonConstants;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数基类
 *
 * @author hify
 */
@Data
public class PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从 1 开始）
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;

    /**
     * 获取有效页码
     */
    public Long getPageNum() {
        if (pageNum == null || pageNum < CommonConstants.ONE) {
            return (long) CommonConstants.DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    /**
     * 获取有效页大小
     */
    public Long getPageSize() {
        if (pageSize == null || pageSize < CommonConstants.ONE) {
            return (long) CommonConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > CommonConstants.MAX_PAGE_SIZE) {
            return (long) CommonConstants.MAX_PAGE_SIZE;
        }
        return pageSize;
    }

    /**
     * 计算 offset
     */
    public Long getOffset() {
        return (getPageNum() - 1) * getPageSize();
    }

    /**
     * 转换为 MyBatis-Plus Page 对象
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Object> toPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(getPageNum(), getPageSize());
    }

    /**
     * 转换为带类型的 MyBatis-Plus Page 对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage(Class<T> clazz) {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(getPageNum(), getPageSize());
    }

}
