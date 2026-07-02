package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeScopeMapper extends MybatisMapper<KnowledgeScopeEntity> { }
