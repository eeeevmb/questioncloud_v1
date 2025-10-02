package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.KnowledgePointEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgePointMapper extends MybatisMapper<KnowledgePointEntity> {
}
