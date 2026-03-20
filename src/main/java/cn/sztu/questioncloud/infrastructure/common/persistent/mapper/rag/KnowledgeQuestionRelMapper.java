package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.rag;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.rag.KnowledgeQuestionRelEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeQuestionRelMapper extends MybatisMapper<KnowledgeQuestionRelEntity> { }