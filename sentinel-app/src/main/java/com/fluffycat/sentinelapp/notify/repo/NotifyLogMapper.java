package com.fluffycat.sentinelapp.notify.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fluffycat.sentinelapp.domain.entity.notify.NotifyLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotifyLogMapper extends BaseMapper<NotifyLogEntity> {}