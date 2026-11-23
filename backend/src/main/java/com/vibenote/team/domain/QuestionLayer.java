package com.vibenote.team.domain;

/** 문항과 그 모듈(레이어). 개념·연결·파일이 없으면 {@code layer}가 null이다 (F-20b AC6·AC7). */
public record QuestionLayer(long questionId, String layer) {}
