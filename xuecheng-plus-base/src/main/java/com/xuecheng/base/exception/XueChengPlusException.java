package com.xuecheng.base.exception;

/**
 * 本项目自定义异常类型
 */
public class XueChengPlusException extends RuntimeException {
    private String errMessage;

    public XueChengPlusException() {
    }

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public static void cast(String message){
        throw new XueChengPlusException(message);
    }

    public static void cast(CommonError error){
        throw new XueChengPlusException(error.getErrMessage());
    }
}
