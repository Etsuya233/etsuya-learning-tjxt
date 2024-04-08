package com.tianji.learning.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SignInMessage {
	private Long userId;
	private Integer points;
}
