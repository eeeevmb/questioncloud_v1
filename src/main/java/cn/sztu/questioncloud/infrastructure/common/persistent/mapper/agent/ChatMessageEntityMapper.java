package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.agent;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageEntityMapper extends MybatisMapper<ChatMessageEntity> {
}
