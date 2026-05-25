package com.foodgroup.chat.dto;

import com.foodgroup.chat.domain.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull ChatMessageType type,
        @NotBlank @Size(max = 1000) String content
) {}
