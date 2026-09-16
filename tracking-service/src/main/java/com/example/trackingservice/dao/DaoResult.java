package com.example.trackingservice.dao;

public final class DaoResult<T> {
    private final T data;
    private final boolean found;
    private final String message;

    private DaoResult(T data, boolean found, String message) {
        this.data = data;
        this.found = found;
        this.message = message;
    }

    public static <T> DaoResult<T> found(T data) {
        return new DaoResult<>(data, true, "Record found");
    }

    public static <T> DaoResult<T> notFound(String message) {
        return new DaoResult<>(null, false, message);
    }

    public static <T> DaoResult<T> success(T data, String message) {
        return new DaoResult<>(data, true, message);
    }

    public T getData() { return data; }
    public boolean isFound() { return found; }
    public String getMessage() { return message; }
}
