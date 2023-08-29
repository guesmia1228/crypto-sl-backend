package com.nefentus.api.entities;

import java.math.BigDecimal;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommissionRate {
	// An affiliate to a vendor
	private static BigDecimal COMM_AFF = new BigDecimal("0.15");
	// A broker, senior broker, or leader to a vendor directly
	private static BigDecimal COMM_PARTNER_DIRECT = new BigDecimal("0.175");
	// A broker, senior broker, or leader to a lower senior broker or broker
	private static BigDecimal COMM_PARTNER = new BigDecimal("0.025");

	private static Role getHighestRole(Set<Role> roleSet) {
		Role highestRole = null;
		for (Role role : roleSet) {
			if (highestRole == null || role.getId() > highestRole.getId()) {
				highestRole = role;
			}
		}
		return highestRole;
	}

	public static BigDecimal get(Role child, Role parent) {
		ERole childRole = child.getName();
		ERole parentRole = parent.getName();

		if (childRole == ERole.ROLE_VENDOR && parentRole == ERole.ROLE_AFFILIATE) {
			return COMM_AFF;
		} else if (childRole == ERole.ROLE_VENDOR && (parentRole == ERole.ROLE_BROKER
				|| parentRole == ERole.ROLE_SENIOR_BROKER || parentRole == ERole.ROLE_LEADER)) {
			return COMM_PARTNER_DIRECT;
		} else if ((childRole == ERole.ROLE_AFFILIATE && parentRole == ERole.ROLE_BROKER) ||
				(childRole == ERole.ROLE_AFFILIATE && parentRole == ERole.ROLE_SENIOR_BROKER) ||
				(childRole == ERole.ROLE_AFFILIATE && parentRole == ERole.ROLE_LEADER) ||
				(childRole == ERole.ROLE_BROKER && parentRole == ERole.ROLE_SENIOR_BROKER) ||
				(childRole == ERole.ROLE_BROKER && parentRole == ERole.ROLE_LEADER) ||
				(childRole == ERole.ROLE_SENIOR_BROKER && parentRole == ERole.ROLE_LEADER)) {
			return COMM_PARTNER;
		}

		log.error("CommissionRate.get() - Invalid role combination: child={}, parent={}", childRole, parentRole);
		return null;
	}

	/*
	 * Returns the commission rate between two sets of roles considering the highest
	 * role for each.
	 */
	public static BigDecimal getAccordingToHighestRole(Set<Role> child, Set<Role> parent) {
		Role childRole = CommissionRate.getHighestRole(child);
		Role parentRole = CommissionRate.getHighestRole(parent);

		return CommissionRate.get(childRole, parentRole);
	}
}
