package com.hify.common.web.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应包装
 *
 * @param <T> 数据类型
 * @author hify
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> list;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult() {
        this.list = Collections.emptyList();
        this.pageNum = 1L;
        this.pageSize = 20L;
        this.total = 0L;
        this.pages = 0L;
    }

    public PageResult(List<T> list, Long pageNum, Long pageSize, Long total) {
        this.list = list == null ? Collections.emptyList() : list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    /**
     * 构建分页结果
     */
    public static <T> PageResult<T> of(List<T> list, Long pageNum, Long pageSize, Long total) {
        return new PageResult<>(list, pageNum, pageSize, total);
    }

    /**
     * 构建空分页结果
     */
    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return new PageResult<>(Collections.emptyList(), pageNum, pageSize, 0L);
    }

    /**
     * 判断是否为空页
     */
    public boolean isEmpty() {
        return list == null || list.isEmpty();
    }

    /**
     * 判断是否有下一页
     */
    public boolean hasNext() {
        return pageNum < pages;
    }

    /**
     * 判断是否有上一页
     */
    public boolean hasPrevious() {
        return pageNum > 1;
    }

}
