package com.chiyumechunga.backend.exception;

public class QrCodeGenerationException extends RuntimeException {
    public QrCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}