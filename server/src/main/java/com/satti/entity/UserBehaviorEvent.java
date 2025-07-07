package com.satti.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEvent {
    private String contact;
    private BehaviorType behavior;
    private Integer movieId;
    private Double rating;
    private Long timestamp;

    public enum BehaviorType {
        VIEW, RATE, WATCH_TOGETHER
    }
}

