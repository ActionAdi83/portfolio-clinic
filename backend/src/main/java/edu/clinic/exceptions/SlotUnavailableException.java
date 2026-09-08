package edu.clinic.exceptions;

/** Thrown when a requested slot is no longer free — either found busy on the
 * server-side re-check, or lost the race to the unique index at insert time. */
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
