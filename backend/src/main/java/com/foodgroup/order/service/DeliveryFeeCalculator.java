package com.foodgroup.order.service;

import org.springframework.stereotype.Component;

@Component
public class DeliveryFeeCalculator {

    public int calculatePerPersonFee(int totalFee, int participantCount) {
        return (int) Math.ceil((double) totalFee / participantCount);
    }

    public int calculateHostSurplus(int totalFee, int participantCount) {
        return calculatePerPersonFee(totalFee, participantCount) * participantCount - totalFee;
    }
}
