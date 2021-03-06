package org.jboss.fuse.security.camel.policy;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException() {}

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(Throwable cause) {
        super(cause);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
