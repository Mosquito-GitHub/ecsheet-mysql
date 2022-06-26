package com.mars.ecsheet.common;

import lombok.Data;

@Data
public class CommonResVo {
    private String code;
    private String message;
    private Object deta;

    public CommonResVo() {
    }

    public CommonResVo(String code, String message, Object deta) {
        this.code = code;
        this.message = message;
        this.deta = deta;
    }

    public static CommonResVo success(){
        return new CommonResVo("SUCCESS","",null);
    }
    public static CommonResVo success(Object data){
        return new CommonResVo("SUCCESS","",data);
    }
    public static CommonResVo fail(String errCode,String errMessage){
        return new CommonResVo(errCode,errMessage,null);
    }
}
