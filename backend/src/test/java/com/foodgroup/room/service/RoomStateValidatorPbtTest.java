package com.foodgroup.room.service;

import com.foodgroup.room.domain.RoomStatus;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class RoomStateValidatorPbtTest {

    private final RoomStateValidator validator = new RoomStateValidator();

    @Property
    void PBT07_COMPLETED_상태에서_어떤_전이도_불가(@ForAll RoomStatus to) {
        assertThat(validator.isValidTransition(RoomStatus.COMPLETED, to)).isFalse();
    }

    @Property
    void PBT07_CANCELLED_상태에서_어떤_전이도_불가(@ForAll RoomStatus to) {
        assertThat(validator.isValidTransition(RoomStatus.CANCELLED, to)).isFalse();
    }

    @Property
    void PBT07_OPEN은_CLOSED_또는_CANCELLED만_허용(@ForAll RoomStatus to) {
        boolean result = validator.isValidTransition(RoomStatus.OPEN, to);
        if (to == RoomStatus.CLOSED || to == RoomStatus.CANCELLED) {
            assertThat(result).isTrue();
        } else {
            assertThat(result).isFalse();
        }
    }

    @Property
    void PBT07_CLOSED는_OPEN_CONFIRMED_또는_CANCELLED만_허용(@ForAll RoomStatus to) {
        boolean result = validator.isValidTransition(RoomStatus.CLOSED, to);
        if (to == RoomStatus.OPEN || to == RoomStatus.CONFIRMED || to == RoomStatus.CANCELLED) {
            assertThat(result).isTrue();
        } else {
            assertThat(result).isFalse();
        }
    }

    @Property
    void PBT07_CONFIRMED는_DELIVERING_또는_CANCELLED만_허용(@ForAll RoomStatus to) {
        boolean result = validator.isValidTransition(RoomStatus.CONFIRMED, to);
        if (to == RoomStatus.DELIVERING || to == RoomStatus.CANCELLED) {
            assertThat(result).isTrue();
        } else {
            assertThat(result).isFalse();
        }
    }

    @Property
    void PBT07_DELIVERING는_COMPLETED만_허용(@ForAll RoomStatus to) {
        boolean result = validator.isValidTransition(RoomStatus.DELIVERING, to);
        if (to == RoomStatus.COMPLETED) {
            assertThat(result).isTrue();
        } else {
            assertThat(result).isFalse();
        }
    }

    @Property
    void PBT07_유효한_전이는_역방향_불가(@ForAll RoomStatus from, @ForAll RoomStatus to) {
        if (validator.isValidTransition(from, to) && from != to
                && !(from == RoomStatus.OPEN && to == RoomStatus.CLOSED)
                && !(from == RoomStatus.CLOSED && to == RoomStatus.OPEN)) {
            assertThat(validator.isValidTransition(to, from)).isFalse();
        }
    }
}
