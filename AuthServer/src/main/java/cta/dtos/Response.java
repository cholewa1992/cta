package cta.dtos;

import java.io.Serializable;

/**
 * Created by wismann on 26/04/2017.
 */
public class Response implements Serializable {

    protected boolean success;
    protected String errorMessage;
    protected Object content;

    public Response(boolean success, String errorMessage, Object content) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.content = content;
    }

    public Response() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
