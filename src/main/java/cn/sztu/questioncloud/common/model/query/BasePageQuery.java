package cn.sztu.questioncloud.common.model.query;


import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.enums.SortDirectionEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 基础分页查询参数类
 * 简化并合并了分页和查询功能
 *
 * @author LinSanQi
 */
@Data
@Schema(description = "基础分页查询参数类")
public class BasePageQuery {

    /**
     * 页码
     */
    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", defaultValue = "20")
    private Integer pageSize = 20;

    /**
     * 关键词搜索
     */
    @Schema(description = "搜索关键词")
    private String keyword;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段")
    private String sortField;

    /**
     * 排序方向：ASC升序、DESC降序
     */
    @Schema(description = "排序方向", allowableValues = {"ASC", "DESC"}, defaultValue = "DESC")
    private SortDirectionEnum sortDirection = SortDirectionEnum.DESC;

    public void valid() {
        // 检查页码是否有效，无效则设置默认值
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }

        // 检查每页条数是否有效，无效则设置默认值
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }

        // 检查关键词长度是否有效（如果关键词不为空，则长度不能超过50）
        if (keyword != null && keyword.trim().length() > 50) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "关键词长度不能超过50");
        }

        // 检查排序方向是否有效，无效则设置默认值
        if (sortDirection != SortDirectionEnum.ASC && sortDirection != SortDirectionEnum.DESC) {
            sortDirection = SortDirectionEnum.DESC;
        }
    }

    public <R> Pager<R> buildPager() {
        if (this.pageNum == null || this.pageNum < 1) {
            this.pageNum = 1;
        }
        if (this.pageSize == null || this.pageSize < 1) {
            this.pageSize = 20;
        }
        return Pager.of(pageNum, pageSize);
    }
}