package cn.sztu.questioncloud.common.util;

import java.time.LocalDateTime;

// 用于计算班级成员状态的工具类
// 根据成员的角色和最后活跃时间来确定成员的状态
// 如果角色是教师，则状态为"N/A"
// 如果角色是学生，则根据最后活跃时间判断状态
// 如果最后活跃时间在一个月内，则状态为"ACTIVE"
// 否则状态为"INACTIVE"
public final class MemberStatusCalculator {

    private MemberStatusCalculator() {}

    public static String calculate(String role, LocalDateTime lastActiveTime) {
        if (role.equals("TEACHER")) {
            return "N/A";
        }
        if (lastActiveTime == null) {
            return "INACTIVE";
        }
        LocalDateTime now = LocalDateTime.now();

        if (lastActiveTime.isAfter(now.minusMonths(1))) {
            return "ACTIVE";
        } else {
            return "INACTIVE";
        }
    }
}
