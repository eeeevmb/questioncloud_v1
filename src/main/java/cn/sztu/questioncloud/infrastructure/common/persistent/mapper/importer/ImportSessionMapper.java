package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.importer;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportSession;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportSessionMapper extends MybatisMapper<ImportSession> {
}
