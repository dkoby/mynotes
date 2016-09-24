
package org.mynotes.exceptions;

/**
 * Thrown when no user exists.
 */
public class NoSuchUserException extends Exception {
    public NoSuchUserException() {super();}
    public NoSuchUserException(String msg) {super(msg);}
}

