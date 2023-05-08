package com.nefentus.api.entities;

public enum ERole {
    ROLE_USER("ROLE_USER"),
    ROLE_AFFILIATE("ROLE_AFFILIATE"),
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_VENDOR("ROLE_VENDOR"),
    ROLE_AFFILIATE_VENDOR("ROLE_AFFILIATE_VENDOR"),
    ROLE_DIAMOND_PARTNER("ROLE_SENIOR_IB"),
    ROLE_GOLD_PARTNER("ROLE_IB"),
    ROLE_IB_LEADER("ROLE_IB_LEADER");

    public final String label;

    private ERole(String label) {
        this.label = label;
    }
}
