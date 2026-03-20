package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.agent;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentEntityMapper extends MybatisMapper<AgentEntity> {
}
