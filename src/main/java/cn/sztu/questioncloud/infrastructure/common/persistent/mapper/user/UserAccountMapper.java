package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.user;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountMapper extends MybatisMapper<UserAccountEntity> {
}
