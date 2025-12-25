package com.fluffycat.sentinelapp.probe.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fluffycat.sentinelapp.domain.entity.probe.ProbeEventEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProbeEventMapper extends BaseMapper<ProbeEventEntity> {
}
