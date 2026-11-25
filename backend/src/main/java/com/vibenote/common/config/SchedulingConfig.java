package com.vibenote.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @Scheduled} 작업을 켠다. 지금은 끝난 챌린지 다시 세기(#146, challenge.ChallengeRecountJob) 하나다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
