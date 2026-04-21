package com.shop.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;

    private R() {}

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.code = 500;
        r.message = message;
        return r;
    }

    public static <T> R<T> error(String message) {
        return fail(message);
    }

    public static <T> R<T> error(Integer code, String message) {
        return fail(code, message);
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> unauthorized(String message) {
        return fail(401, message);
    }

    public static <T> R<T> forbidden(String message) {
        return fail(403, message);
    }

    public boolean isOk() {
        return this.code != null && this.code == 200;
    }
}
