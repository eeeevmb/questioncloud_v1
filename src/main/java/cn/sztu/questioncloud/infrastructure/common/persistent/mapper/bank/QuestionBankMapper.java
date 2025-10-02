package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.bank;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.bank.QuestionBankEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionBankMapper extends MybatisMapper<QuestionBankEntity> {
}
