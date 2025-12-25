package com.fluffycat.sentinelapp.target.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluffycat.sentinelapp.common.api.ErrorCode;
import com.fluffycat.sentinelapp.common.exception.BusinessException;
import com.fluffycat.sentinelapp.common.pagination.PageRequest;
import com.fluffycat.sentinelapp.common.pagination.PageRequests;
import com.fluffycat.sentinelapp.common.pagination.PageResponse;
import com.fluffycat.sentinelapp.target.config.TargetProperties;
import com.fluffycat.sentinelapp.target.converter.TargetConverter;
import com.fluffycat.sentinelapp.domain.dto.target.request.CreateTargetRequest;
import com.fluffycat.sentinelapp.domain.dto.target.request.UpdateTargetRequest;
import com.fluffycat.sentinelapp.domain.dto.target.response.TargetResponse;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.target.repo.TargetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {

    private final Environment environment;
    private final TargetProperties targetProperties;
    private final PageRequests pageRequests;
    private final TargetMapper targetMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public TargetResponse createTarget(CreateTargetRequest request) {
        String env = resolveEnv(request.getEnv());
        TargetEntity entity = TargetConverter.toEntity(request, env);
        targetMapper.insert(entity);
        return TargetConverter.toResponse(entity);
    }

    public PageResponse<TargetResponse> getAllTargets() {
        List<TargetResponse> list = targetMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(TargetConverter::toResponse)
                .toList();
        return PageResponse.unpaged(list);
    }

    @Transactional
    public TargetResponse updateTarget(Long id, UpdateTargetRequest req) {
        TargetEntity existing = targetMapper.selectById(id);
        if (existing == null) throw new BusinessException(ErrorCode.NOT_FOUND, "target not found: " + id);

        TargetConverter.merge(req, existing);
        targetMapper.updateById(existing);
        TargetEntity saved = targetMapper.selectById(id);
        return TargetConverter.toResponse(saved != null ? saved : existing);
    }

    public PageResponse<TargetResponse> getTargets(Boolean enabled, String tag, String owner, String env, int page, int size) {
        PageRequest pr = pageRequests.of(page, size);

        LambdaQueryWrapper<TargetEntity> qw = Wrappers.lambdaQuery();
        qw.eq(enabled != null, TargetEntity::getEnabled, enabled)
                .eq(owner != null, TargetEntity::getOwner, owner)
                .eq(env != null, TargetEntity::getEnv, env)
                .apply(StringUtils.hasText(tag), "FIND_IN_SET({0},tags))", StringUtils.trimAllWhitespace(tag))
                .orderByDesc(TargetEntity::getId);

        Page<TargetEntity> p = targetMapper.selectPage(Page.of(pr.getPage(), pr.getSize()), qw);
        return PageResponse.from(p, TargetConverter::toResponse);
    }

    public TargetResponse getTarget(Long id) {
        return TargetConverter.toResponse(targetMapper.selectById(id));
    }

    public String resolveEnv(String reqEnv) {
        // 1) 请求优先
        if (reqEnv != null && !reqEnv.trim().isEmpty()) {
            return normalizeAndValidate(reqEnv);
        }
        // 2) 配置默认
        String configured = targetProperties.defaultEnv();
        if (configured != null && !configured.isBlank()) {
            return normalizeAndValidate(configured);
        }
        // 3) 根据 active profiles 推断
        for (String p : environment.getActiveProfiles()) {
            if ("docker".equalsIgnoreCase(p)) return "docker";
        }
        return "local";
    }

    private String normalizeAndValidate(String env) {
        String e = env.trim().toLowerCase();
        if (!e.equals("local") && !e.equals("docker")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "env must be local or docker");
        }
        return e;
    }


}