package cn.sztu.questioncloud.infrastructure.common.file.repository.mapper;

import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMetadataMapper extends MybatisMapper<InfraFileMetadata> {
}
