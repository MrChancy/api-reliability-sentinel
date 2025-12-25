package com.fluffycat.sentinelapp.domain.dto.target.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTargetRequest {
	@Schema(description = "目标名称", example = "demo-flaky")
	private String name;
	private String owner;
	private Headers headers;
	@Schema(description = "请求方法", example = "GET")
	private String method;
	@Schema(description = "错误率阈值（百分比）", example = "50.0")
	private BigDecimal errorRateThreshold;
	private Integer silenceMinutes;
	@Schema(description = "探测周期（秒）", example = "60")
	private Integer intervalSec;
	private String silenceReason;
	@Schema(description = "是否启用", example = "true")
	private Boolean enabled;
	private String tags;
	@Schema(description = "路径（可包含查询参数）", example = "/demo/flaky?failRate=50")
	private String path;
	private Integer retries;
	@Schema(description = "窗口（秒）", example = "300")
	private Integer windowSec;
	@Schema(description = "基础地址（docker内用 http://demo-api:8081）", example = "http://demo-api:8081")
	private String baseUrl;
	@Schema(description = "超时（毫秒）", example = "800")
	private Integer timeoutMs;
	@Schema(description = "环境：local/docker", example = "docker")
	private String env;
}