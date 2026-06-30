package cn.sztu.questioncloud.common.util;

/**
 * 组卷权重选择工具类
 */
public class PaperWeightAlgorithmUtil {

    private static final double DEFAULT_EXP_DECAY_RATE = 1.79;

    /**
     * 当题目难度偏离目标难度 0.15 时，权重约衰减到 60.6%
     */
    private static final double DEFAULT_DIFFICULTY_SIGMA = 0.15;

    private PaperWeightAlgorithmUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 计算难度权重（高斯衰减）
     */
    public static double calcDifficultyWeight(Double questionDifficulty, Double targetDifficulty, double variance) {
        if (questionDifficulty == null || targetDifficulty == null) {
            return 1.0;
        }
        if (variance <= 0) {
            throw new IllegalArgumentException("衰减常数必须大于 0");
        }

        double diff = questionDifficulty - targetDifficulty;
        return Math.exp(-(diff * diff) / (2 * variance * variance));
    }

    /**
     * 计算曝光权重（指数衰减）
     */
    public static double calcExposureWeight(Double exposureFactor, double exposureDecayRate) {
        if (exposureDecayRate <= 0) {
            throw new IllegalArgumentException("衰减常数必须大于 0");
        }

        double effectiveExposure = exposureFactor == null ? 0.0 : exposureFactor;
        return Math.exp(-exposureDecayRate * effectiveExposure);
    }

    /**
     * 计算组合权重
     */
    public static double calcCombinedWeight(Double exposureFactor,
                                            Double questionDifficulty,
                                            Double targetDifficulty,
                                            boolean ignoreExposure) {
        double difficultyWeight = calcDifficultyWeight(
                questionDifficulty, targetDifficulty, DEFAULT_DIFFICULTY_SIGMA);
        if (ignoreExposure) {
            return difficultyWeight;
        }

        double exposureWeight = calcExposureWeight(exposureFactor, DEFAULT_EXP_DECAY_RATE);
        return exposureWeight * difficultyWeight;
    }

    /**
     * 计算 Gumbel Key
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
}
