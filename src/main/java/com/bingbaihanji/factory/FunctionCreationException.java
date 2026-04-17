package com.bingbaihanji.factory;

import com.bingbaihanji.constant.FunctionType;

public class FunctionCreationException extends RuntimeException {

    private final FunctionType type;

    public FunctionCreationException(FunctionType type, String message) {
        super(message);
        this.type = type;
    }

    public FunctionCreationException(FunctionType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public FunctionType getType() {
        return type;
    }
}
