package com.nefentus.api.entities;

public enum EBlockchain {
	ETHEREUM("ETH"),
	BSC("BSC");

	public final String label;

	private EBlockchain(String label) {
		this.label = label;
	}
}
