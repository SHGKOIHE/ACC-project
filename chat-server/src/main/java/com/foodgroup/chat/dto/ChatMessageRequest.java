package com.foodgroup.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageRequest(
        @NotNull ChatMessageType type,
        @NotBlank String content
) {
}
