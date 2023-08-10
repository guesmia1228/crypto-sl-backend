package com.nefentus.api.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddProductRequest {
	private String name;
	private String description;
	private BigDecimal price;
	private String imagePath;
	private Integer stock;
}
