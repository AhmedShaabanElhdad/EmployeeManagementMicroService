package core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomResponseException extends RuntimeException {
    public int code;
    public String message;

    public static CustomResponseException ResourceNotFound(String message) {
        return new CustomResponseException(404, message);
    }

    public static CustomResponseException BadRequest(String message) {
        return new CustomResponseException(400, message);
    }

    public static CustomResponseException TooManyRequest() {
        return new CustomResponseException(429, "Too many requests");
    }

    public static CustomResponseException BadCredential() {
        return new CustomResponseException(400, "Bad Credentials");
    }

    public static CustomResponseException InternalServerError(String message) {
        return new CustomResponseException(500, message);
    }
}
