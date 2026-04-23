package com.demand.common;

import lombok.Data;
import java.util.List;

/**
 * 分页结果集
 * @param <T>
 */
@Data
public class PageResult<T> {
    //数据集
    private List<T> list;
    //总条数
    private Long total;
    //页码
    private Integer pageNum;
    //每页条数
    private Integer pageSize;
    //总页数
    private Integer pages;

    //封装分页结果集
    public PageResult(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        this.pages = (int) Math.ceil((double) total / this.pageSize);
    }
}