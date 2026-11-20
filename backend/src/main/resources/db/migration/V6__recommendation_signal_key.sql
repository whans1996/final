-- F-16: 추천 이유를 만든 근거(신호 키). 지금 신호와 다르면 이유를 다시 만든다.
ALTER TABLE recommendation_reasons
    ADD COLUMN signal_key VARCHAR(20) NOT NULL DEFAULT '' COMMENT '예: w2c1k0l1' AFTER reason;
