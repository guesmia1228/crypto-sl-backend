package com.nefentus.api.entities;

public enum EBlockchain {
	ETHEREUM("ETHEREUM"),
	BSC("BSC");

	public final String label;

	private EBlockchain(String label) {
		this.label = label;
	}
}
