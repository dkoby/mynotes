
package org.mynotes.exceptions;

/**
 * Thrown on user confirm string mismatch.
 */
public class InvalidUserConfirmString extends Exception {
    public InvalidUserConfirmString() {super();}
    public InvalidUserConfirmString(String msg) {super(msg);}
}

