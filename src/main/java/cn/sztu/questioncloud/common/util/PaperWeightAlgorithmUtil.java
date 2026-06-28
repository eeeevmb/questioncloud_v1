package cn.sztu.questioncloud.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 组卷权重选择工具类
 *
 */
public class PaperWeightAlgorithmUtil {
    private static final double DEFAULT_EXP_DECAY_RATE = 1.79;

    //当题目难度偏离目标难度 0.15 时，其抽取权重会平滑衰减至 60.6%
    private static final double DEFAULT_DIFFICULTY_SIGMA = 0.15;

    private PaperWeightAlgorithmUtil()  {throw new UnsupportedOperationException("工具类不允许实例化");}

    /**
     * 计算难度正态分布权重
     * <p>
     * 已默认传入的 Difficulty合法: [0.0,1.0]
     * 公式：w = e^(-(x-μ)^2 / (2 * sigma^2))
     * </p>
     *
     * @param questionDifficulty 题目实际难度
     * @param targetDifficulty   目标难度
     * @param variance           权值衰减常数 ，类似于正态分布中的 σ，必须 > 0
     * @return 难度权重 (0, 1]
     */
    public static double calcDifficultyWeight(Double questionDifficulty, Double targetDifficulty, double variance) {
        // 如果题目没标难度，或者规则没设目标难度，则不进行难度加权惩罚（权重为 1）
        if (questionDifficulty == null || targetDifficulty == null) {
            return 1.0;
        }
        if (variance <= 0) {
            throw new IllegalArgumentException("衰减常数必须大于 0");
        }
        double diff = questionDifficulty - targetDifficulty;
        //variance 越大，衰减越平缓；偏离目标难度 variance 时，权重衰减至 60.6%
        return Math.exp(-(diff * diff) / (2 * variance * variance));
    }

    /**
     * 计算曝光指数衰减权重 (Exponential Decay)
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
     * 策略：曝光度 [0, 1] 区间内实现 6 倍的概率差。
     *      难度正态衰减，当难度偏离目标难度 0.15 时，权重衰减至 60.6%。
     * </p>
     */
    public static double calcExponentialWeight(Double exposureFactor, Double questionDifficulty, Double targetDifficulty) {
        double expWeight = calcExponentialWeight(exposureFactor, DEFAULT_EXP_DECAY_RATE);
        double DiffWeight = calcDifficultyWeight(questionDifficulty, targetDifficulty, DEFAULT_DIFFICULTY_SIGMA);

        return expWeight * DiffWeight;
    }

}
