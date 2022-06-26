package com.mars.ecsheet.common;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class UploadDataDto {
    private List<JSONObject> exceldatas;
}
