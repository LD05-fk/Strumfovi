package hr.fer.artconnect.file.service;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;

public class UnsupportedFileTypeException extends ApiException {
    public UnsupportedFileTypeException() {
        super(ErrorCode.UNSUPPORTED_FILE_TYPE, "Vrsta sadržaja datoteke nije podržana.");
    }
}
