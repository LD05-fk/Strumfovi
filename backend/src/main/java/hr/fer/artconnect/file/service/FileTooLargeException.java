package hr.fer.artconnect.file.service;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;

public class FileTooLargeException extends ApiException {
    public FileTooLargeException() {
        super(ErrorCode.FILE_TOO_LARGE, "Datoteka prelazi dopuštenu veličinu.");
    }
}
