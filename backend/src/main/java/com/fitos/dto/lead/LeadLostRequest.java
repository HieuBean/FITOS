package com.fitos.dto.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Task #29: đánh dấu lead thất bại — bắt buộc có lý do để phục vụ phân tích. */
public record LeadLostRequest(
        @NotBlank(message = "Lý do mất lead là bắt buộc")
        @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
        String reason
) {
}
