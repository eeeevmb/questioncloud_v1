package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.ExamEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamMapper extends MybatisMapper<ExamEntity> {}