package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionVersionMapper extends MybatisMapper<QuestionVersionEntity> {
}
