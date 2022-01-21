package com.example.tmf;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//=========================================================
// tmfController Error Handler クラス
// 呼び出し元にHTTP StatusとResponse Error Bodyを返す
//=========================================================
@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler{

    // TmfException(独自)の Error Handler。呼び出し元にHTTP StatusとResponse Bodyを返す。
    @ExceptionHandler(TmfException.class)
    public ResponseEntity<Object> handleTmfException(TmfException ex) {
        return new ResponseEntity<Object>(ex.getTmfErrorCode(), null, ex.getHttpStatus());
    }

    // 共通の Error Handler。呼び出し元にHTTP StatusとResponse Bodyを返す。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        return new ResponseEntity<Object>(ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
