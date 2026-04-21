package com.shop.common.result;

/**
 * 分页响应体
 */
public class PageResult<T> {
    private Long total;
    private java.util.List<T> records;
    private Long current;
    private Long size;

    public PageResult() {}

    public PageResult(java.util.List<T> records, Long total, Long pages) {
        this.records = records;
        this.total = total;
        this.size = pages;
    }

    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.total = page.getTotal();
        result.records = page.getRecords();
        result.current = page.getCurrent();
        result.size = page.getSize();
        return result;
    }

    // Getters and Setters
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public java.util.List<T> getRecords() { return records; }
    public void setRecords(java.util.List<T> records) { this.records = records; }
    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
