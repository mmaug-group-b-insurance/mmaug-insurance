package com.mmaug.claimsagent.common.exception;

public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException() {
        super("This document is already uploaded to this claim. Open the existing document instead.");
    }
}
