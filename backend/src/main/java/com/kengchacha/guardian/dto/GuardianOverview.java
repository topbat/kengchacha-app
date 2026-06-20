package com.kengchacha.guardian.dto;

import java.util.List;

/** 守护中心一次性概览：关系 + 预警 + 未读数。 */
public record GuardianOverview(int relationCount, long unreadCount,
                               List<RelationView> relations, List<AlertView> alerts) {
}
