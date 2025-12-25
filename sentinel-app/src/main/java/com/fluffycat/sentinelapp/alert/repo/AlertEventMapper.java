package com.fluffycat.sentinelapp.alert.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fluffycat.sentinelapp.domain.entity.alert.AlertEventEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertEventMapper extends BaseMapper<AlertEventEntity> {
}
