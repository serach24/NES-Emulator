package me.charlesj.util.assembler;

/**
 * 2020/2/15.
 */
public class AssemblerException extends Exception {
    public AssemblerException(String message) {
        super(message);
    }
    public AssemblerException(String message, Throwable cause) {
        super(message, cause);
    }
    public AssemblerException(Throwable cause) {
        super(cause);
    }
    protected AssemblerException(String message, Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
