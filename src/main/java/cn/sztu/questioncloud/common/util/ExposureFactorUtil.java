package cn.sztu.questioncloud.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 题目曝光系数工具类。
 * <p>
 * 读路径只需要调用 {@link #calcEffectiveExposure(Double, LocalDateTime, LocalDateTime)} 计算当前有效曝光值；
 * 写路径（确认组卷/下发）需要使用 {@link #renewExposureOnEvent(Double, LocalDateTime, LocalDateTime)}，
 * 并由调用方负责将 {@code lastExposedAt} 更新为 {@code now}。
 * </p>
 *
 * @author CodeX
 */
public final class ExposureFactorUtil {
    private static final double DEFAULT_HALF_LIFE_DAYS = 7.0d;
    private static final double DEFAULT_ALPHA = 0.3d;
    private static final double SECONDS_PER_DAY = 86_400d;

    private ExposureFactorUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 计算当前有效曝光值（读路径专用）。
     *
     * @param exp            lastExposedAt 时刻的基准曝光强度，空值按 1.0 处理
     * @param lastExposedAt  上次曝光时间，空值视为刚曝光（Δt = 0）
     * @param now            当前时间
     * @param halfLifeDays   半衰期（天），必须 &gt; 0
     * @return 当前有效曝光值，范围 [0, 1]
     */
    public static double calcEffectiveExposure(Double exp,
                                               LocalDateTime lastExposedAt,
                                               LocalDateTime now,
                                               double halfLifeDays) {
        Objects.requireNonNull(now, "当前时间不能为空");
        if (halfLifeDays <= 0) {
            throw new IllegalArgumentException("半衰期必须大于 0");
        }
        double baseExp = normalizeExp(exp);
        if (lastExposedAt == null) {
            return baseExp;
        }
        Duration duration = Duration.between(lastExposedAt, now);
        if (duration.isZero() || duration.isNegative()) {
            return baseExp;
        }
        double deltaDays = duration.toMillis() / (SECONDS_PER_DAY * 1000d);
        double decay = Math.pow(0.5d, deltaDays / halfLifeDays);
        return clamp(baseExp * decay);
    }

    /**
     * 在曝光事件发生时计算新的基准曝光值，并由调用方回写 {@code exp'} 与 {@code lastExposedAt = now}。
     *
     * @param exp            lastExposedAt 时刻的基准曝光强度，空值按 1.0 处理
     * @param lastExposedAt  上次曝光时间，空值视为刚曝光（Δt = 0）
     * @param now            当前时间
     * @param halfLifeDays   半衰期（天），必须 &gt; 0
     * @param alpha          回升系数，要求 0 &lt; alpha &lt; 1
     * @return 回升后的基准曝光值，范围 [0, 1]
     */
    public static double renewExposureOnEvent(Double exp,
                                              LocalDateTime lastExposedAt,
                                              LocalDateTime now,
                                              double halfLifeDays,
                                              double alpha) {
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException("回升系数必须在 (0,1) 范围内");
        }
        double eff = calcEffectiveExposure(exp, lastExposedAt, Objects.requireNonNull(now, "当前时间不能为空"), halfLifeDays);
        double renewed = eff + (1 - eff) * alpha;
        return clamp(renewed);
    }

    /**
     * 使用默认半衰期计算当前有效曝光值。
     */
    public static double calcEffectiveExposure(Double exp,
                                               LocalDateTime lastExposedAt,
                                               LocalDateTime now) {
        return calcEffectiveExposure(exp, lastExposedAt, now, DEFAULT_HALF_LIFE_DAYS);
    }

    /**
     * 使用默认半衰期和回升系数计算曝光事件后的基准曝光值。
     */
    public static double renewExposureOnEvent(Double exp,
                                              LocalDateTime lastExposedAt,
                                              LocalDateTime now) {
        return renewExposureOnEvent(exp, lastExposedAt, now, DEFAULT_HALF_LIFE_DAYS, DEFAULT_ALPHA);
    }

    private static double normalizeExp(Double exp) {
        double value = exp == null ? 1.0d : exp;
        return clamp(value);
    }

    private static double clamp(double value) {
        if (value < 0d) {
            return 0d;
        }
        if (value > 1d) {
            return 1d;
        }
        return value;
    }
}
