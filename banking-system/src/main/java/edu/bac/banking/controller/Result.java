package edu.bac.banking.controller;

/** Uniform response for both boundary layers. Views never see exceptions, only a Result. */
public final class Result<T> {

    private final boolean ok;
    private final String message;
    private final T data;

    private Result(boolean ok, String message, T data) {
        this.ok = ok;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "OK", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(false, message, null);
    }

    public boolean isOk() { return ok; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
