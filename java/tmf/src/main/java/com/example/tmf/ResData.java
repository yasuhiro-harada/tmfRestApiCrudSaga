package com.example.tmf;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

//=========================================================
// Response用データ クラス
//=========================================================
@Getter
@Setter
public class ResData implements Serializable{

    private static final long serialVersionUID = 1L;

    // ResponseBody(ID+Href+FetchData)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Setter
    public static class ResBody extends FetchData{
        private String id;
        private String href;
    }

    // ResponseBody List
    private List<ResBody> resBodies = new ArrayList<ResBody>();
    // ResponseHeader
    private HttpHeaders httpHeaders = new HttpHeaders();
    // Response HTTP Status
    private HttpStatus httpStatus;
}
