
package org.mynotes.exceptions;

/**
 * Thrown on attempt to create user with not unique name.
 */
public class DuplicateUserException extends Exception {
    public DuplicateUserException() {super();}
    public DuplicateUserException(String msg) {super(msg);}
}

