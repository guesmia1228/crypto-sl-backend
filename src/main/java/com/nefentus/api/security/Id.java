package com.nefentus.api.security;

import org.apache.commons.lang3.RandomStringUtils;

public class Id {

	public static String getAlphaNumeric() {
		return Id.getAlphaNumeric(21);
	}

	public static String getAlphaNumeric(int length) {
		return RandomStringUtils.random(length, true, true);
	}
}
