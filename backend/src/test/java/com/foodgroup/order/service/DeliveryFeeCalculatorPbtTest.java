package com.foodgroup.order.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.*;

class DeliveryFeeCalculatorPbtTest {

    private final DeliveryFeeCalculator calculator = new DeliveryFeeCalculator();

    @Property
    void PBT02_1인당배달비_곱하기_인원수는_총배달비_이상(
            @ForAll @IntRange(min = 0, max = 50_000) int totalFee,
            @ForAll @IntRange(min = 1, max = 20) int count) {
        int perPerson = calculator.calculatePerPersonFee(totalFee, count);
        assertThat((long) perPerson * count).isGreaterThanOrEqualTo(totalFee);
    }

    @Property
    void PBT02_잉여금은_항상_0_이상(
            @ForAll @IntRange(min = 0, max = 50_000) int totalFee,
            @ForAll @IntRange(min = 1, max = 20) int count) {
        assertThat(calculator.calculateHostSurplus(totalFee, count)).isGreaterThanOrEqualTo(0);
    }

    @Property
    void PBT03_잉여금은_인원수_미만(
            @ForAll @IntRange(min = 0, max = 50_000) int totalFee,
            @ForAll @IntRange(min = 1, max = 20) int count) {
        assertThat(calculator.calculateHostSurplus(totalFee, count)).isLessThan(count);
    }

    @Property
    void 정확히_나누어지면_잉여금_0(
            @ForAll @IntRange(min = 1, max = 20) int count) {
        int totalFee = count * 1000;
        assertThat(calculator.calculateHostSurplus(totalFee, count)).isEqualTo(0);
    }

    @Property
    void 잉여금과_총배달비_합산은_perPerson_곱_인원수_와_일치(
            @ForAll @IntRange(min = 0, max = 50_000) int totalFee,
            @ForAll @IntRange(min = 1, max = 20) int count) {
        int perPerson = calculator.calculatePerPersonFee(totalFee, count);
        int surplus = calculator.calculateHostSurplus(totalFee, count);
        assertThat(perPerson * count - surplus).isEqualTo(totalFee);
    }
}
