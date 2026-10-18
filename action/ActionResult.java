package com.evcharging.action;

public class ActionResult<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final Throwable exception;

    private ActionResult(boolean success, T data, String message, Throwable exception) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.exception = exception;
    }

    public static <T> ActionResult<T> success(T data) {
        return new ActionResult<>(true, data, "Success", null);
    }

    public static <T> ActionResult<T> success(T data, String message) {
        return new ActionResult<>(true, data, message, null);
    }

    public static <T> ActionResult<T> failure(String message) {
        return new ActionResult<>(false, null, message, null);
    }

    public static <T> ActionResult<T> failure(String message, Throwable exception) {
        return new ActionResult<>(false, null, message, exception);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return exception;
    }
}
