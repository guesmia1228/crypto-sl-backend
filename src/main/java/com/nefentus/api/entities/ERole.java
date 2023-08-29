package com.nefentus.api.entities;

public enum ERole {
	ROLE_VENDOR("ROLE_VENDOR"),
	ROLE_AFFILIATE("ROLE_AFFILIATE"),
	ROLE_BROKER("ROLE_BROKER"),
	ROLE_SENIOR_BROKER("ROLE_SENIOR_BROKER"),
	ROLE_LEADER("ROLE_LEADER"),
	ROLE_ADMIN("ROLE_ADMIN");

	public final String label;

	private ERole(String label) {
		this.label = label;
	}
}
