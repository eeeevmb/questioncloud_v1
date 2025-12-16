package cn.sztu.questioncloud.common.model.vo;

import cn.sztu.questioncloud.common.model.query.BasePageQuery;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果类
 *
 * @author LinSanQi
 */
@Data
public class PageResult<T> {
    /**
     * 当前页数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int pageNum;

    /**
     * 每页记录数
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int pages;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 创建空的分页结果
     *
     * @param <T> 记录类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return of(Collections.emptyList(), 0, 1, 10);
    }

    /**
     * 创建分页结果
     *
     * @param records   记录列表
     * @param total     总记录数
     * @param pageQuery 分页查询参数
     * @param <T>       记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, BasePageQuery pageQuery) {
        return of(records, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    /**
     * 创建分页结果
     *
     * @param records  记录列表
     * @param total    总记录数
     * @param pageNum  页码
     * @param pageSize 每页记录数
     * @param <T>      记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.pageNum = pageNum;
        result.pageSize = pageSize;

        // 计算总页数
        result.pages = (int) ((total + pageSize - 1) / pageSize);

        // 计算是否有上一页和下一页
        result.hasPrevious = pageNum > 1;
        result.hasNext = pageNum < result.pages;

        return result;
    }

    /**
     * 将当前分页结果转换为另一种类型的分页结果
     *
     * @param mapper 转换函数
     * @param <R>    目标类型
     * @return 转换后的分页结果
     */
    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        List<R> mappedRecords = this.records.stream()
                .map(mapper)
                .collect(java.util.stream.Collectors.toList());

        return PageResult.of(mappedRecords, this.total, this.pageNum, this.pageSize);
    }
}