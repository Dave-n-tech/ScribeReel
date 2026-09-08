package org.scribereel.exceptions;


public class VideoProcessingException extends RuntimeException{
    public VideoProcessingException(String message){
        super(message);
    }

    public VideoProcessingException(String message, Throwable e){
        super(message, e);
    }
}
