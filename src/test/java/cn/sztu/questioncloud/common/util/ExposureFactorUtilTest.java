package cn.sztu.questioncloud.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExposureFactorUtilTest {
    private static final double HALF_LIFE = 7.0d;
    private static final double EPS = 1e-9;
    private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

    @Test
    void calcEffectiveExposure_whenDeltaZero_returnsBaseExposure() {
        double exp = 0.8d;
        double result = ExposureFactorUtil.calcEffectiveExposure(exp, now, now, HALF_LIFE);
        assertEquals(exp, result, EPS);
    }

    @Test
    void calcEffectiveExposure_whenDeltaEqualsHalfLife_halvesExposure() {
        double exp = 0.8d;
        LocalDateTime last = now.minusDays((long) HALF_LIFE);
        double result = ExposureFactorUtil.calcEffectiveExposure(exp, last, now, HALF_LIFE);
        assertEquals(exp / 2, result, 1e-6);
    }

    @Test
    void calcEffectiveExposure_sameDeltaProducesSameDecayRatio() {
        LocalDateTime last = now.minusDays(3);
        double exp1 = 0.8d;
        double exp2 = 0.3d;
        double eff1 = ExposureFactorUtil.calcEffectiveExposure(exp1, last, now, HALF_LIFE);
        double eff2 = ExposureFactorUtil.calcEffectiveExposure(exp2, last, now, HALF_LIFE);
        double ratio1 = eff1 / exp1;
        double ratio2 = eff2 / exp2;
        assertEquals(ratio1, ratio2, 1e-9);
    }

    @Test
    void renewExposureOnEvent_appliesFormula() {
        double exp = 0.5d;
        double alpha = 0.3d;
        double renewed = ExposureFactorUtil.renewExposureOnEvent(exp, now, now, HALF_LIFE, alpha);
        double expected = exp + (1 - exp) * alpha;
        assertEquals(expected, renewed, EPS);
    }

    @Test
    void calcEffectiveExposure_handlesNullExpAndLastExposed() {
        double resultWhenNullExp = ExposureFactorUtil.calcEffectiveExposure(null, now, now, HALF_LIFE);
        assertEquals(1.0d, resultWhenNullExp, EPS);

        double exp = 0.4d;
        double resultWhenNullLast = ExposureFactorUtil.calcEffectiveExposure(exp, null, now, HALF_LIFE);
        assertEquals(exp, resultWhenNullLast, EPS);
    }

    @Test
    void calcAndRenewValidateParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> ExposureFactorUtil.calcEffectiveExposure(0.5d, now, now, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ExposureFactorUtil.renewExposureOnEvent(0.5d, now, now, HALF_LIFE, 1));
    }
}
