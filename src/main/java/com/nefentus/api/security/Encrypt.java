package com.nefentus.api.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.InvalidKeyException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class Encrypt {
	/*
	 * Algorithms used for encryption/decryption
	 * See https://baeldung.com/java-aes-encryption-decryption
	 * https://crypto.stackexchange.com/questions/75586/aes-256-cbc-storing-local-
	 * data-how-to-save-iv-vector
	 */
	// Use AES and GCM encryption
	private static final String algorithm = "AES/GCM/NoPadding";

	public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(n);
		SecretKey key = keyGenerator.generateKey();
		return key;
	}

	public static SecretKey getKeyFromPassword(String password, String salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
		SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
				.getEncoded(), "AES");
		return secret;
	}

	public static GCMParameterSpec generateNonce() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return new GCMParameterSpec(96, iv);
	}

	public static String encrypt(String input, SecretKey key, GCMParameterSpec nonce)
			throws NoSuchPaddingException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, InvalidKeyException,
			BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = Cipher.getInstance(Encrypt.algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key, nonce);
		byte[] cipherText = cipher.doFinal(input.getBytes());
		return Base64.getEncoder()
				.encodeToString(cipherText);
	}

	public static String decrypt(String cipherText, SecretKey key, GCMParameterSpec nonce)
			throws NoSuchPaddingException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, InvalidKeyException,
			BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = Cipher.getInstance(Encrypt.algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, nonce);
		byte[] plainText = cipher.doFinal(Base64.getDecoder()
				.decode(cipherText));
		return new String(plainText);
	}
}
