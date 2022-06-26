package com.mars.ecsheet.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
@Slf4j
public class SheetUtil {

    /**
     * 获取sheet的默认option
     *
     * @return
     */
    public static JSONObject getDefautOption() {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("container", "ecsheet");
        jsonObject.put("title", "ecsheet demo");
        jsonObject.put("lang", "zh");
        jsonObject.put("allowUpdate", true);
        jsonObject.put("loadUrl", "");
        jsonObject.put("loadSheetUrl", "");
        jsonObject.put("updateUrl", "");

        return jsonObject;
    }

    /**
     * 获取默认的sheetData
     *
     * @return
     */
    public static List<JSONObject> getDefaultSheetData() {
        List<JSONObject> list = new ArrayList<>();

        for (int i = 1; i < 4; i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("row", 84);
            jsonObject.put("column", 60);
            jsonObject.put("name", "sheet" + i);
//            Integer index = i - 1;
            jsonObject.put("index", UUID.randomUUID());
            jsonObject.put("order", i - 1);
            if (i == 1) {
                jsonObject.put("status", 1);
            } else {
                jsonObject.put("status", 0);
            }
            jsonObject.put("celldata", new ArrayList<>());
            list.add(jsonObject);
        }
        log.info("{}",list);
        return list;
    }
//
//    public static JSONObject getOneSheetData() {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("row", 84);
//        jsonObject.put("column", 60);
//        jsonObject.put("name", "sheet" + i);
////            Integer index = i - 1;
//        jsonObject.put("index", UUID.randomUUID());
//        jsonObject.put("order", i - 1);
//        if (i == 1) {
//            jsonObject.put("status", 1);
//        } else {
//            jsonObject.put("status", 0);
//        }
//        jsonObject.put("celldata", new ArrayList<JSONObject>() {
//        });
//        return jsonObject;
//    }

    /**
     * 获取默认的全部sheetData
     *
     * @return
     */
    public static JSONObject getDefaultAllSheetData() {
        JSONObject result = new JSONObject();

        for (int i = 1; i < 4; i++) {
            JSONObject data = new JSONObject();
            data.put("r", 0);
            data.put("c", 0);
            data.put("v", new JSONObject());
            result.put("sheet" + i, data);
        }
        return result;
    }

    /**
     * 组装异步加载sheet所需的数据
     *
     * @param data
     * @return
     */
    public static JSONObject buildSheetData(List<JSONObject> data) {
        JSONObject result = new JSONObject();
        data.forEach((d) -> {
            log.debug("load sheet index:{}",d.get("index").toString());
            result.put(d.get("index").toString(), d.get("celldata"));
        });

        return result;
    }


    /**
     * 将一个sheet页面数据根据部门拆分成不同的sheet页面
     *
     * @param sheetData 原始数据
     * @return 返回拆分后的多个sheet页面数据
     */
    public static List<JSONObject> splitSheet(List<JSONObject> sheetData) {
        log.info("sheet data size: {}", sheetData.size());

        JSONObject jsonObject = sheetData.get(0);
        JSONArray celldataArray = jsonObject.getJSONArray("celldata");

        List<JSONObject> sheetHeader = new ArrayList<>();
        int headerRowEndIndex = 0;//标题结束行号
        int depStartStartIndex = 0;//员工数据开始行号

        // 寻找标题结束行号和员工数开始行号,并记录标题部分
        for (int i = 0; i < celldataArray.size(); i++) {
            if ("员工编码".equals(celldataArray.getJSONObject(i).getJSONObject("v").getString("v"))) {
                headerRowEndIndex = celldataArray.getJSONObject(i).getInteger("r");
                log.debug("index:{},headerRowIndex:{}", i, headerRowEndIndex);
            }
            sheetHeader.add(celldataArray.getJSONObject(i));
            if (headerRowEndIndex != 0 && headerRowEndIndex + 1 == celldataArray.getJSONObject(i).getInteger("r")) {
                depStartStartIndex = i;
                break;
            }
        }
        log.debug("sheetHeader:{}", JSON.toJSONString(sheetHeader));

        //将数据按行分组
        Map<Integer, List<JSONObject>> rowData = new HashMap<>();
        for (int i = depStartStartIndex + 1; i < celldataArray.size(); i++) {
            Integer r = celldataArray.getJSONObject(i).getInteger("r");
            if (rowData.containsKey(r)) {
                rowData.get(r).add(celldataArray.getJSONObject(i));
            } else {
                List<JSONObject> rowDataValue = new ArrayList<>();
                rowDataValue.add(celldataArray.getJSONObject(i));
                rowData.put(r, rowDataValue);
            }
        }
        //将数据按部门分组
        Map<String, List<JSONObject>> deptData = new HashMap<>();
        rowData.forEach((r, value) -> {
            if (value.size() >= 3) {
                String deptName = value.get(2).getJSONObject("v").getString("v");
                if (deptData.containsKey(deptName)) {
                    deptData.get(deptName).addAll(value);
                } else {
                    deptData.put(deptName, value);
                }
            }
        });
        //将数据按照部门分组分成多个sheet页
        List<JSONObject> result = new ArrayList<>();
        Set<String> sheetNameSet = deptData.keySet();
        int sheetIndex = 1;
        for (String sheetName : sheetNameSet) {
            List<JSONObject> cellData = new ArrayList<>(sheetHeader);
            AtomicInteger rowIndex = new AtomicInteger(headerRowEndIndex);

            //转换成行数据
            Map<Integer, List<JSONObject>> rowData1 = new HashMap<>();
            for (JSONObject deptData1 : deptData.get(sheetName)) {
                Integer r = deptData1.getInteger("r");
                if (rowData1.containsKey(r)) {
                    rowData1.get(r).add(deptData1);
                } else {
                    List<JSONObject> rowDataValue = new ArrayList<>();
                    rowDataValue.add(deptData1);
                    rowData1.put(r, rowDataValue);
                }
            }
            //修改行号
            for (Map.Entry<Integer, List<JSONObject>> entry : rowData1.entrySet()) {
                int ri = rowIndex.incrementAndGet();
                entry.getValue().forEach(obj -> {
                    obj.put("r", ri);
                });
                cellData.addAll(entry.getValue());

            }
            JSONObject data = new JSONObject();
            data.put("row", rowIndex.getAndIncrement() + 10);
            data.put("column", 60);
            data.put("name", sheetName);
            Integer index = sheetIndex - 1;
            data.put("index", UUID.randomUUID());
            data.put("order", index);
            data.put("status", index == 0 ? 1 : 0);

            data.put("celldata", cellData);
            result.add(data);
            sheetIndex += 1;
        }
        return result;
    }


    public static List<JSONObject> mergeSheet(List<JSONObject> sheetData) {
        log.info("sheetData size: {}", sheetData.size());
        JSONObject jsonObject = sheetData.get(0);
        JSONArray celldataArray = jsonObject.getJSONArray("celldata");

        List<JSONObject> sheetHeader = new ArrayList<>();
        int headerRowEndIndex = 0;//标题结束行号
        int depStartStartIndex = 0;//员工数据开始行号

        // 寻找标题结束行号和员工数开始行号,并记录标题部分
        for (int i = 0; i < celldataArray.size(); i++) {
            if ("员工编码".equals(celldataArray.getJSONObject(i).getJSONObject("v").getString("v"))) {
                headerRowEndIndex = celldataArray.getJSONObject(i).getInteger("r");
                log.debug("index:{},headerRowIndex:{}", i, headerRowEndIndex);
            }
            sheetHeader.add(celldataArray.getJSONObject(i));
            if (headerRowEndIndex != 0 && headerRowEndIndex + 1 == celldataArray.getJSONObject(i).getInteger("r")) {
                depStartStartIndex = i;
                break;
            }
        }
        log.debug("sheetHeader:{}", JSON.toJSONString(sheetHeader));

        AtomicInteger deptRowNumber = new AtomicInteger(headerRowEndIndex);
        List<JSONObject> cellDataAll = new ArrayList<>(sheetHeader);
        for (int i = 0; i < sheetData.size(); i++) {
            JSONObject sheetDataObject = sheetData.get(i);
            if (!sheetDataObject.containsKey("celldata")) continue;
            JSONArray celldata = sheetDataObject.getJSONArray("celldata");

            //转换成行数据
            Map<Integer, List<JSONObject>> rowData = new HashMap<>();
            for (int x = depStartStartIndex; x < celldata.size(); x++) {
                JSONObject cData = celldata.getJSONObject(x);
                Integer r = cData.getInteger("r");
                if (rowData.containsKey(r)) {
                    rowData.get(r).add(cData);
                } else {
                    List<JSONObject> rowDataValue = new ArrayList<>();
                    rowDataValue.add(cData);
                    rowData.put(r, rowDataValue);
                }
            }
            //修改行号
            for (Map.Entry<Integer, List<JSONObject>> entry : rowData.entrySet()) {
                int ri = deptRowNumber.incrementAndGet();
                entry.getValue().forEach(obj -> {
                    obj.put("r", ri);
                });
                cellDataAll.addAll(entry.getValue());
            }
        }

        JSONObject data = new JSONObject();
        data.put("row", deptRowNumber.getAndIncrement() + 10);
        data.put("column", 60);
        data.put("name", "sheet");

        data.put("index", 0);
        data.put("order", 0);
        data.put("status", 1);

        data.put("celldata", cellDataAll);

        return Collections.singletonList(data);
    }
}
