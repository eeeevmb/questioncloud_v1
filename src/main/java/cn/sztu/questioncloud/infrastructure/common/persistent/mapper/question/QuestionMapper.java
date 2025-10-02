package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends MybatisMapper<QuestionEntity> {
}
