package com.nefentus.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {
	@Value("${app.name.web3-endpoint-ethereum}")
	private String appWeb3Endpoint;

	@Bean
	public Web3j Web3() {
		return Web3j.build(new HttpService(appWeb3Endpoint));
	}
}
