package cn.sztu.questioncloud.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 组卷权重选择工具类
 *
 */
public class PaperWeightAlgorithmUtil {
    private static final double DEFAULT_EXP_DECAY_RATE = 1.79;

    private PaperWeightAlgorithmUtil()  {throw new UnsupportedOperationException("工具类不允许实例化");}

    /**
     * 计算指数衰减权重 (Exponential Decay)
     * <p>
     * 已默认传入的 exposureFactor nonNull且合法
     * 公式：w = e^(-k * exp)
     * </p>
     *
     * @param exposureFactor    曝光系数 [0, 1]
     * @param exposureDecayRate 权值衰减常数
     * @return                  权重
     */
    public static double calcExponentialWeight(Double exposureFactor,double exposureDecayRate) {
        if (exposureDecayRate <= 0) {
            throw new IllegalArgumentException("衰减常数必须大于 0");
        }

        // 计算权重 w = e^(-k * exp)
        return Math.exp(-exposureDecayRate * exposureFactor);
    }

    /**
     * 计算 Gumbel Key
     * <p>
     * 公式：g = -ln(u) / w
     * </p>
     *
     * @param weight            权重 (对应公式中的 k)
     * @param randomValue       随机数 (0, 1] (对应公式中的 u)
     * @return                  Gumbel 排序键 (Key)
     */
    public static double calcGumbelKey(double weight, double randomValue) {
        if (randomValue <= 0 || randomValue > 1) {
            throw new IllegalArgumentException("随机数必须在 (0, 1] 范围内");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("权重必须大于 0");
        }

        return -Math.log(randomValue) / weight;
    }

    /**
     * 使用默认衰减常数 计算指数衰减权重
     * <p>
     * 策略：平滑衰减，在曝光度 [0, 1] 区间内实现 6 倍的概率差。
     * </p>
     */
    public static double calcExponentialWeight(Double exposureFactor) {
        return calcExponentialWeight(exposureFactor, DEFAULT_EXP_DECAY_RATE);
    }

}
