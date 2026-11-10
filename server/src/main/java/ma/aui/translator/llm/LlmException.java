package ma.aui.translator.llm;

/** Raised when the upstream LLM cannot be reached or returns an unusable answer. */
public class LlmException extends RuntimeException {
    private final int upstreamStatus;

    public LlmException(String message) {
        this(message, -1, null);
    }

    public LlmException(String message, Throwable cause) {
        this(message, -1, cause);
    }

    public LlmException(String message, int upstreamStatus, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    /** HTTP status returned by the LLM provider, or -1 if not applicable. */
    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
