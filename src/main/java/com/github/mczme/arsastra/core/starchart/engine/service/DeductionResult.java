package com.github.mczme.arsastra.core.starchart.engine.service;

import java.util.List;

/**
 * 推演系统的结果汇总。
 * 包含完整的路径片段、最终稳定度以及是否炼金成功的判定。
 *
 * @param segments       分段路径列表，供客户端渲染动画
 * @param finalStability 最终稳定度 (0.0 - 1.0)
 * @param isSuccess      是否成功完成炼金（未因稳定度归零而崩溃）
 */
public record DeductionResult(
        List<SegmentData> segments,
        float finalStability,
        boolean isSuccess
) {
}
