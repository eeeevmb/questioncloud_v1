package cn.sztu.questioncloud.infrastructure.adapter.utils;

import cn.sztu.questioncloud.common.enums.SortDirectionEnum;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import db.sql.api.Getter;

import java.util.List;

public class RepositoryUtils {

    /**
     * 获取查询链，并指定字段
     *
     * @param mapper  mapper
     * @param getters 字段
     * @return 查询链
     */
    public static <T> QueryChain<T> getQueryChainWithFields(MybatisMapper<T> mapper, List<Getter<T>> getters) {
        QueryChain<T> queryChain = QueryChain.of(mapper);
        if (getters != null && !getters.isEmpty()) {
            @SuppressWarnings("unchecked")
            Getter<T>[] getterArray = getters.toArray(new Getter[0]);
            return queryChain.select(getterArray);
        }
        return queryChain;
    }

    /**
     * 设置排序条件
     *
     * @param queryChain 查询链
     * @param column     字段
     * @param direction  排序方向
     */
    public static <T> void setSortDirectionCondition(QueryChain<T> queryChain, Getter<T> column, SortDirectionEnum direction) {
        if (SortDirectionEnum.ASC.equals(direction)) {
            queryChain.orderBy(column);
        } else {
            queryChain.orderByDesc(column);
        }
    }
}
