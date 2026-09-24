package com.panelamagica.panelamagica.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponseDTO {

    private String message;
    private int status;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponseDTO(String message, int value, LocalDateTime now) {
        this.message = message;
        this.status = value;
        this.timestamp = now;
    }
}
