package com.mars.ecsheet.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.*;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author SKFC
 * @title: ExcelUtils
 * @projectName central-platform
 * @description: TODO
 * @date 2020/11/2410:06
 */
@Slf4j
public class ExcelUtils {
    public static CellStyle createCellStyle(XSSFSheet sheet, XSSFWorkbook wb, JSONObject jsonObjectValue){
        XSSFCellStyle cellStyle = wb.createCellStyle();
        Map<Integer, String> fontMap = new HashMap<>();
        fontMap.put(-1, "Arial");
        fontMap.put(0, "Times New Roman");
        fontMap.put(1, "Arial");
        fontMap.put(2, "Tahoma");
        fontMap.put(3, "Verdana");
        fontMap.put(4, "微软雅黑");
        fontMap.put(5, "宋体");
        fontMap.put(6, "黑体");
        fontMap.put(7, "楷体");
        fontMap.put(8, "仿宋");
        fontMap.put(9, "新宋体");
        fontMap.put(10, "华文新魏");
        fontMap.put(11, "华文行楷");
        fontMap.put(12, "华文隶书");
        //合并单元格
        if (jsonObjectValue.get("mc") != null && ((JSONObject)jsonObjectValue.get("mc")).get("rs") != null && ((JSONObject)jsonObjectValue.get("mc")).get("cs") != null){
            int r = Integer.parseInt(((JSONObject)jsonObjectValue.get("mc")).get("r").toString());//主单元格的行号,开始行号
            int rs = Integer.parseInt(((JSONObject)jsonObjectValue.get("mc")).get("rs").toString());//合并单元格占的行数,合并多少行
            int c = Integer.parseInt(((JSONObject)jsonObjectValue.get("mc")).get("c").toString());//主单元格的列号,开始列号
            int cs = Integer.parseInt(((JSONObject)jsonObjectValue.get("mc")).get("cs").toString());//合并单元格占的列数,合并多少列
            CellRangeAddress region = new CellRangeAddress(r, r + rs - 1, c, c + cs - 1);
            sheet.addMergedRegion(region);
        }
        XSSFFont font = wb.createFont();
        //字体
        if(jsonObjectValue.get("ff") != null){
            if (jsonObjectValue.get("ff").toString().matches("^(-?\\d+)(\\.\\d+)?$")){
                font.setFontName(fontMap.get(jsonObjectValue.getInteger("ff")));
            }else {
                font.setFontName(jsonObjectValue.get("ff").toString());
            }
        }
        //字体颜色
        if (jsonObjectValue.get("fc") != null){
            String fc = jsonObjectValue.get("fc").toString();
            XSSFColor color = toColorFromString(fc);
            font.setColor(color);
        }
        //粗体
        if (jsonObjectValue.get("bl") != null){
            font.setBold("1".equals(jsonObjectValue.get("bl").toString()));
        }
        //斜体
        if (jsonObjectValue.get("it") != null){
            font.setItalic("1".equals(jsonObjectValue.get("it").toString()));
        }
        //删除线
        if (jsonObjectValue.get("cl") != null){
            font.setStrikeout("1".equals(jsonObjectValue.get("cl").toString()));
        }
        //下滑线
        if (jsonObjectValue.get("un") != null){
            font.setUnderline("1".equals(jsonObjectValue.get("un").toString()) ? FontUnderline.SINGLE : FontUnderline.NONE);
        }
        //字体大小
        if (jsonObjectValue.get("fs") != null){
            font.setFontHeightInPoints(new Short(jsonObjectValue.get("fs").toString()));
        }
        cellStyle.setFont(font);
        //水平对齐
        if (jsonObjectValue.get("ht") != null){
            switch (jsonObjectValue.getInteger("ht")) {
                case 0:
                    cellStyle.setAlignment(HorizontalAlignment.CENTER);
                    break;
                case 1:
                    cellStyle.setAlignment(HorizontalAlignment.LEFT);
                    break;
                case 2:
                    cellStyle.setAlignment(HorizontalAlignment.RIGHT);
                    break;
            }
        }
        //垂直对齐
        if (jsonObjectValue.get("vt") != null){
            switch (jsonObjectValue.getInteger("vt")) {
                case 0:
                    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    break;
                case 1:
                    cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
                    break;
                case 2:
                    cellStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);
                    break;
            }
        }
        //背景颜色
        if (jsonObjectValue.get("bg") != null){
            String bg = jsonObjectValue.get("bg").toString();
            cellStyle.setFillForegroundColor(toColorFromString(bg));
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        cellStyle.setWrapText(true);
        return cellStyle;
    }

    /**
     * 字符串转换成Color对象
     * @param colorStr 16进制颜色字符串
     * @return Color对象
     * */
    public static XSSFColor toColorFromString(String colorStr) {
        if (colorStr.contains("#")){
            colorStr = colorStr.substring(1);
            Color color = new Color(Integer.parseInt(colorStr, 16));
            return new XSSFColor(color, new DefaultIndexedColorMap());
        }else {
            int strStartIndex = colorStr.indexOf("(");
            int strEndIndex = colorStr.indexOf(")");
            String[] strings = colorStr.substring(strStartIndex+1,strEndIndex).split(",");
            String R = Integer.toHexString(Integer.parseInt(strings[0].replaceAll(" ","")));
            R = R.length() < 2 ? ('0' + R) : R;
            String B = Integer.toHexString(Integer.parseInt(strings[1].replaceAll(" ","")));
            B = B.length() < 2 ? ('0' + B) : B;
            String G = Integer.toHexString(Integer.parseInt(strings[2].replaceAll(" ","")));
            G = G.length() < 2 ? ('0' + G) : G;
            String  cStr=  R + B + G;
            Color color1 = new Color(Integer.parseInt(cStr, 16));
            return new XSSFColor(color1, new DefaultIndexedColorMap());
        }
    }



    /**
     * 功能: LuckySheet导出方法
     * 开发：zzq
     * @param excelData    数据
     * @param response         用来获取输出流
     * @param request       针对火狐浏览器导出时文件名乱码的问题,也可以不传入此值
     * @throws IOException
     */
    public static void exportLuckySheetXlsx(String excelData,String fileName,HttpServletRequest request, HttpServletResponse response)  {
        //解析对象，可以参照官方文档:https://mengshukeji.github.io/LuckysheetDocs/zh/guide/#%E6%95%B4%E4%BD%93%E7%BB%93%E6%9E%84
        JSONArray jsonArray = (JSONArray) JSONObject.parse(excelData);
        //如果只有一个sheet那就是get(0),有多个那就对应取下标
        List<JSONObject> jsonObjects = jsonArray.toJavaList(JSONObject.class);
        XSSFWorkbook wb = new XSSFWorkbook();
        for (int i=0 ;i<jsonObjects.size();i++){
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
//            JSONObject jsonObject = sheetObject.getJSONObject("data");
            JSONArray jsonObjectList = jsonObject.getJSONArray("celldata");
            JSONObject images = jsonObject.getJSONObject("images");
            JSONObject dataVerification = jsonObject.getJSONObject("dataVerification");
            //默认高
            short defaultRowHeight = jsonObject.getShort("defaultRowHeight") == null ?20:jsonObject.getShort("defaultRowHeight");
            //默认宽
            short defaultColWidth = jsonObject.getShort("defaultColWidth") == null ?74:jsonObject.getShort("defaultColWidth");
            JSONObject config = jsonObject.getJSONObject("config");
            //行列冻结
            JSONObject frozen = jsonObject.getJSONObject("frozen");
            JSONObject columnlenObject = null;//表格列宽
            JSONObject rowlenObject = null;//表格行高
            JSONArray borderInfoObjectList = null;//边框样式
            if (config != null){
                columnlenObject = jsonObject.getJSONObject("config").getJSONObject("columnlen");//表格列宽
                rowlenObject = jsonObject.getJSONObject("config").getJSONObject("rowlen");//表格行高
                borderInfoObjectList = jsonObject.getJSONObject("config").getJSONArray("borderInfo");//边框样式
            }
            //读取了模板内所有sheet内容
            XSSFSheet sheet = wb.createSheet(jsonObject.get("name").toString());
            //如果这行没有了，整个公式都不会有自动计算的效果的
            sheet.setForceFormulaRecalculation(true);
            //固定行列
            setFreezePane(sheet,frozen);
            //设置行高列宽
            setCellWH(sheet,columnlenObject,rowlenObject);
            //图片插入
            setImages(wb,sheet,images,columnlenObject,rowlenObject,defaultRowHeight,defaultColWidth);
            //设置单元格值及格式
            setCellValue(wb,sheet,jsonObjectList,columnlenObject,rowlenObject,defaultRowHeight,defaultColWidth);
            //设置数据验证
            settDataValidation(dataVerification,sheet);
            if (borderInfoObjectList != null){
                //设置边框
                setBorder(borderInfoObjectList,sheet);
            }
        }
        try {
            String disposition = "attachment;filename=";
            if (request != null && request.getHeader("USER-AGENT") != null && (request.getHeader("USER-AGENT").contains("Firefox"))) {
                disposition += new String((fileName+".xlsx").getBytes(), "ISO8859-1");
            } else {
                disposition += URLEncoder.encode(fileName+".xlsx", "UTF-8");
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            response.setHeader("Content-Disposition", disposition);
            //修改模板内容导出新模板
            OutputStream out = null;
            out = response.getOutputStream();
            wb.write(out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取图片位置
     * dx1：起始单元格的x偏移量，如例子中的255表示直线起始位置距A1单元格左侧的距离；
     * dy1：起始单元格的y偏移量，如例子中的125表示直线起始位置距A1单元格上侧的距离；
     * dx2：终止单元格的x偏移量，如例子中的1023表示直线起始位置距C3单元格左侧的距离；
     * dy2：终止单元格的y偏移量，如例子中的150表示直线起始位置距C3单元格上侧的距离；
     * col1：起始单元格列序号，从0开始计算；竖
     * row1：起始单元格行序号，从0开始计算，如例子中col1=0,row1=0就表示起始单元格为A1；横
     * col2：终止单元格列序号，从0开始计算；
     * row2：终止单元格行序号，从0开始计算，如例子中col2=2,row2=2就表示起始单元格为C3；
     * @param imageDefault
     * @param defaultRowHeight
     * @param defaultColWidth
     * @param columnlenObject
     * @param rowlenObject
     */
    public static Map<String, Integer> getColRowMap(JSONObject imageDefault,short defaultRowHeight , short defaultColWidth ,JSONObject columnlenObject, JSONObject rowlenObject){
        int left =  (int) imageDefault.get("left");
        int top =  (int) imageDefault.get("top");
        int width =  (int) imageDefault.get("width");
        int height =  (int) imageDefault.get("height");
        //算起始最大列
        int colMax1 = (int)Math.ceil((double)left/defaultColWidth);
        //算起始最大行
        int rowMax1 = (int)Math.ceil((double)top/defaultRowHeight);
        //算终止最大列
        int colMax2 = (int)Math.ceil((double)(left+width)/defaultColWidth);
        //算终止最大行
        int rowMax2 = (int)Math.ceil((double)(top+height)/defaultRowHeight);
//        int dx1 = left;//宽 行
//        int dy1 = top; //高 列
//        int dx2 = left+width;
//        int dy2 = top+height;
        BigDecimal dx1 = new BigDecimal(left);//宽 行
        BigDecimal dy1 = new BigDecimal(top); //高 列
        BigDecimal dx2 = new BigDecimal(left+width);
        BigDecimal dy2 = new BigDecimal(top+height);
        int col1 = 0;
        int row1 = 0;
        int col2 = 0;
        int row2 = 0;
        //算起始列的序号和偏移量
        for (int index = 0;index <= colMax1;index++){
            BigDecimal col = null;
            if (columnlenObject != null && columnlenObject.getString(Integer.toString(index)) != null){
                col = new BigDecimal(columnlenObject.getString(Integer.toString(index)));//看当前列是否重新赋值
            }
            //算起始列
            if (col == null && dx1.compareTo(new BigDecimal(defaultColWidth)) < 0){
                col1 = index;
                break;
            }
            //算起始X偏移
            if (col == null && dx1.compareTo(new BigDecimal(defaultColWidth)) >= 0){
                dx1 =dx1.subtract(new BigDecimal(defaultColWidth)) ;
            }

            //算起始列
            if (col != null && dx1.compareTo(col) < 0){
                col1 = index;
                break;
            }
            //算起始X偏移
            if (col != null ){
                dx1 = dx1.subtract(col) ;
            }

        }

        //算起始行的序号和偏移量
        for (int index = 0;index <= rowMax1;index++){
            BigDecimal row = null;
            if (rowlenObject != null && rowlenObject.getString(Integer.toString(index)) != null){
                row = new BigDecimal(rowlenObject.getString(Integer.toString(index)));//看当前行是否重新赋值
            }
            //算起始行
            if (row == null && dy1.compareTo(new BigDecimal(defaultRowHeight)) < 0){
                row1 = index;
                break;
            }
            //算起始y偏移
            if (row == null && dy1.compareTo(new BigDecimal(defaultRowHeight)) >= 0){
                dy1 =dy1.subtract(new BigDecimal(defaultRowHeight));
            }
            //算起始行
            if (row != null && dy1.compareTo(row) < 0){
                row1 = index;
                break;
            }
            //算起始y偏移
            if (row != null){
                dy1 = dy1.subtract(row) ;
            }
        }

        //算最终列的序号和偏移量
        for (int index = 0;index <= colMax2;index++){
            BigDecimal col = null;
            if (columnlenObject != null && columnlenObject.getString(Integer.toString(index)) != null){
                col = new BigDecimal(columnlenObject.getString(Integer.toString(index)));//看当前列是否重新赋值
            }
            //算最终列
            if (col == null && dx2.compareTo(new BigDecimal(defaultColWidth)) < 0){
                col2 = index;
                break;
            }
            //算最终X偏移
            if (col == null && dx2.compareTo(new BigDecimal(defaultColWidth)) >= 0){
                dx2 =dx2.subtract(new BigDecimal(defaultColWidth)) ;
            }

            //算最终列
            if (col != null && dx2.compareTo(col) < 0){
                col2 = index;
                break;
            }
            //算最终X偏移
            if (col != null ){
                dx2 = dx2.subtract(col) ;
            }

        }

        //算最终行的序号和偏移量
        for (int index = 0;index <= rowMax2;index++){
            //行高
            BigDecimal row = null;
            if (rowlenObject != null && rowlenObject.getString(Integer.toString(index)) != null){
                row = new BigDecimal(rowlenObject.getString(Integer.toString(index)));//看当前行是否重新赋值
            }
            //算最终行
            if (row == null && dy2.compareTo(new BigDecimal(defaultRowHeight)) < 0){
                row2 = index;
                break;
            }
            //算最终y偏移
            if (row == null && dy2.compareTo(new BigDecimal(defaultRowHeight)) >= 0){
                dy2 =dy2.subtract(new BigDecimal(defaultRowHeight));
            }
            //算最终行
            if (row != null && dy2.compareTo(row) < 0){
                row2 = index;
                break;
            }
            //算最终Y偏移
            if (row != null){
                dy2 = dy2.subtract(row) ;
            }
        }
        Map<String, Integer> map =new HashMap<>();
        map.put("dx1",dx1.multiply(new BigDecimal(Units.EMU_PER_PIXEL)).setScale(0,BigDecimal.ROUND_HALF_UP).intValue());
        map.put("dy1",dy1.multiply(new BigDecimal(Units.EMU_PER_PIXEL)).setScale(0,BigDecimal.ROUND_HALF_UP).intValue());
        map.put("dx2",dx2.multiply(new BigDecimal(Units.EMU_PER_PIXEL)).setScale(0,BigDecimal.ROUND_HALF_UP).intValue());
        map.put("dy2",dy2.multiply(new BigDecimal(Units.EMU_PER_PIXEL)).setScale(0,BigDecimal.ROUND_HALF_UP).intValue());
        map.put("col1",col1);
        map.put("row1",row1);
        map.put("col2",col2);
        map.put("row2",row2);
        return map;
    }

    /**
     * 行列冻结
     * @param sheet
     * @param frozen
     */
    private static void setFreezePane(XSSFSheet sheet, JSONObject frozen) {
        if (frozen != null){
            Map<String, Object> frozenMap = frozen.getInnerMap();
            //首行
            if ("row".equals(frozenMap.get("type").toString())){
                sheet.createFreezePane(0,1);
            }
            //首列
            if ("column".equals(frozenMap.get("type").toString())){
                sheet.createFreezePane(1,0);
            }
            //行列
            if ("both".equals(frozenMap.get("type").toString())){
                sheet.createFreezePane(1,1);
            }
            //几行
            if ("rangeRow".equals(frozenMap.get("type").toString()) ){
                JSONObject value = (JSONObject) frozenMap.get("range");
                sheet.createFreezePane(0,value.getInteger("row_focus")+1);
            }
            //几列
            if ("rangeColumn".equals(frozenMap.get("type").toString())){
                JSONObject value = (JSONObject) frozenMap.get("range");
                sheet.createFreezePane(value.getInteger("column_focus")+1,0);
            }
            //几行列
            if ("rangeBoth".equals(frozenMap.get("type").toString())){
                JSONObject value = (JSONObject) frozenMap.get("range");
                sheet.createFreezePane(value.getInteger("column_focus")+1,value.getInteger("row_focus")+1);
            }
        }
    }


    /**
     * 设置非默认宽高
     * @param sheet
     * @param columnlenObject
     * @param rowlenObject
     */
    private static void setCellWH(XSSFSheet sheet, JSONObject columnlenObject,JSONObject rowlenObject) {
        //我们都知道excel是表格，即由一行一行组成的，那么这一行在java类中就是一个XSSFRow对象，我们通过XSSFSheet对象就可以创建XSSFRow对象
        //如：创建表格中的第一行（我们常用来做标题的行)  XSSFRow firstRow = sheet.createRow(0); 注意下标从0开始
        //根据luckysheet创建行列
        //创建行和列
        if (rowlenObject != null){
            Map<String, Object> rowMap = rowlenObject.getInnerMap();
            for(Map.Entry<String, Object> rowEntry : rowMap.entrySet()) {
                XSSFRow row = sheet.createRow(Integer.parseInt(rowEntry.getKey()));//创建行
                BigDecimal hei=new BigDecimal(rowEntry.getValue() + "");
                //转化excle行高参数1
                BigDecimal excleHei1=new BigDecimal(72);
                //转化excle行高参数2
                BigDecimal excleHei2=new BigDecimal(96);
                row.setHeightInPoints(hei.multiply(excleHei1).divide(excleHei2).floatValue());//行高px值
                if (columnlenObject != null){
                    Map<String, Object> cloMap = columnlenObject.getInnerMap();
                    for(Map.Entry<String, Object> cloEntry : cloMap.entrySet()) {
                        BigDecimal wid=new BigDecimal(cloEntry.getValue().toString());
                        //转化excle列宽参数35.7   调试后我改为33   --具体多少没有算
                        BigDecimal excleWid=new BigDecimal(33);
                        sheet.setColumnWidth(Integer.parseInt(cloEntry.getKey()), wid.multiply(excleWid).setScale(0,BigDecimal.ROUND_HALF_UP).intValue());//列宽px值
                        row.createCell(Integer.parseInt(cloEntry.getKey()));//创建列
                    }
                }
            }
        }
    }

    /**
     *
     * @param wb
     * @param sheet
     * @param images 所有图片
     * @param columnlenObject
     * @param rowlenObject
     * @param defaultRowHeight
     * @param defaultColWidth
     */
    private static void setImages(XSSFWorkbook wb,XSSFSheet sheet, JSONObject images,JSONObject columnlenObject,JSONObject rowlenObject,short defaultRowHeight,short defaultColWidth){
        //图片插入
        if (images != null){
            Map<String, Object> map = images.getInnerMap();
            JSONObject finalColumnlenObject = columnlenObject;
            JSONObject finalRowlenObject = rowlenObject;
            for(Map.Entry<String, Object> entry : map.entrySet()) {
                XSSFDrawing patriarch = sheet.createDrawingPatriarch();
                //图片信息
                JSONObject iamgeData = (JSONObject) entry.getValue();
                //图片的位置宽 高 距离左 距离右
                JSONObject imageDefault = ((JSONObject) iamgeData.get("default"));
                //算坐标
                Map<String, Integer> colrowMap = getColRowMap(imageDefault, defaultRowHeight, defaultColWidth, finalColumnlenObject, finalRowlenObject);
                XSSFClientAnchor anchor = new XSSFClientAnchor(colrowMap.get("dx1"), colrowMap.get("dy1"), colrowMap.get("dx2"), colrowMap.get("dy2"), colrowMap.get("col1"), colrowMap.get("row1"), colrowMap.get("col2"), colrowMap.get("row2"));
                anchor.setAnchorType(ClientAnchor.AnchorType.byId(Integer.parseInt(iamgeData.get("type").toString())));
                byte[] decoderBytes = new byte[0];
                boolean flag = true;
                if (iamgeData.get("src") != null) {
                    decoderBytes = Base64.getDecoder().decode(iamgeData.get("src").toString().split(";base64,")[1]);
                    flag = iamgeData.get("src").toString().split(";base64,")[0].contains("png");
                }
                if (flag) {
                    patriarch.createPicture(anchor, wb.addPicture(decoderBytes, HSSFWorkbook.PICTURE_TYPE_PNG));
                } else {
                    patriarch.createPicture(anchor, wb.addPicture(decoderBytes, HSSFWorkbook.PICTURE_TYPE_JPEG));
                }
            }
        }
    }

    /**
     * 设置单元格
     * @param wb
     * @param sheet
     * @param jsonObjectList
     * @param columnlenObject
     * @param rowlenObject
     * @param defaultRowHeight
     * @param defaultColWidth
     */
    private static void setCellValue(XSSFWorkbook wb,XSSFSheet sheet,JSONArray jsonObjectList,JSONObject columnlenObject,JSONObject rowlenObject,short defaultRowHeight,short defaultColWidth) {
        for (int index = 0; index < jsonObjectList.size(); index++) {
            JSONObject object = jsonObjectList.getJSONObject(index);
            JSONObject jsonObjectValue = ((JSONObject) object.get("v"));
            log.info(jsonObjectValue.toJSONString());
            String value = "";
//            String m = "";
            if (jsonObjectValue.containsKey("v")) {
//                m = jsonObjectValue.get("m") + "";
                value = jsonObjectValue.get("v") + "";
            }
            if (sheet.getRow((int) object.get("r")) == null){
                sheet.createRow((int) object.get("r"));
            }
            XSSFRow row = sheet.getRow((int) object.get("r"));
            if (row.getCell((int) object.get("c")) == null){
                row.createCell((int) object.get("c"));
            }
            XSSFCell cell = row.getCell((int) object.get("c"));
            //设置单元格样式
            CellStyle cellStyle = ExcelUtils.createCellStyle(sheet,wb,jsonObjectValue);
            //如果单元格内容是数值类型，涉及到金钱（金额、本、利），则设置cell的类型为数值型，设置data的类型为数值类型
            XSSFDataFormat df = wb.createDataFormat(); // 此处设置数据格式
            Boolean isNumber = false;
            Boolean isString = false;
            Boolean isDate = false;
            SimpleDateFormat sdf = null;
            if (jsonObjectValue.get("ct") != null){
                cellStyle.setDataFormat(df.getFormat(((JSONObject) jsonObjectValue.get("ct")).getString("fa")));
                String t = ((JSONObject) jsonObjectValue.get("ct")).getString("t");
                if ("n".equals(t)){
                    isNumber = true;
                }
                if ("d".equals(t)){
                    isDate = true;
                }
                if ("s".equals(t)){
                    isString = true;
                }
            }
            if (isNumber){
                // 设置单元格格式
                cell.setCellStyle(cellStyle);
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(value);
            }
            else if (isDate){
                String fa = ((JSONObject) jsonObjectValue.get("ct")).getString("fa");
                if (fa.contains("AM/PM")){
                    sdf =  new SimpleDateFormat(fa.replaceAll("AM/PM","aa"), Locale.ENGLISH);
                }else {
                    sdf = new SimpleDateFormat(fa);
                }
                try {
                    Date date = sdf.parse(value);
                    cell.setCellStyle(cellStyle);
                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellValue(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            else if (isString){
                // 设置单元格格式
                cell.setCellStyle(cellStyle);
                cell.setCellType(CellType.STRING);
                cell.setCellValue(value);
            }else {
                //设置单元格格式
                cell.setCellStyle(cellStyle);
                cell.setCellValue(value);
            }
            //设置公式
            if (jsonObjectValue.get("f") != null){
                cell.setCellFormula(jsonObjectValue.get("f").toString().substring(1));
            }
            //设置批注
            if (jsonObjectValue.get("ps") != null){
                XSSFDrawing p = sheet.createDrawingPatriarch();
                //后四个坐标待定
                //前四个参数是坐标点,后四个参数是编辑和显示批注时的大小.
                JSONObject ps = (JSONObject)jsonObjectValue.get("ps");
                Map<String, Integer> colrowMapPS = getColRowMap(ps,defaultRowHeight,defaultColWidth, columnlenObject, rowlenObject);
                XSSFClientAnchor anchor = new XSSFClientAnchor(colrowMapPS.get("dx1"), colrowMapPS.get("dy1"), colrowMapPS.get("dx2"), colrowMapPS.get("dy2"), colrowMapPS.get("col1"), colrowMapPS.get("row1"), colrowMapPS.get("col2"), colrowMapPS.get("row2"));
                XSSFComment comment = p.createCellComment(anchor);
                // 输入批注信息
                comment.setString(new XSSFRichTextString(ps.getString("value")));
                // 添加状态
                comment.setVisible("true".equals(ps.getString("isshow")));
                // 将批注添加到单元格对象中
                cell.setCellComment(comment);
            }

        }
    }

    /**
     * 设置边框样式
     * @param borderInfoObjectList
     * @param sheet
     */
    private static void setBorder(JSONArray borderInfoObjectList, XSSFSheet sheet) {
        //设置边框样式map
        Map<Integer, BorderStyle> bordMap = new HashMap<>();
        bordMap.put(0, BorderStyle.NONE);
        bordMap.put(1, BorderStyle.THIN);
        bordMap.put(2, BorderStyle.HAIR);
        bordMap.put(3, BorderStyle.DOTTED);
        bordMap.put(4, BorderStyle.DASHED);
        bordMap.put(5, BorderStyle.DASH_DOT);
        bordMap.put(6, BorderStyle.DASH_DOT_DOT);
        bordMap.put(7, BorderStyle.DOUBLE);
        bordMap.put(8, BorderStyle.MEDIUM);
        bordMap.put(9, BorderStyle.MEDIUM_DASHED);
        bordMap.put(10, BorderStyle.MEDIUM_DASH_DOT);
        bordMap.put(11, BorderStyle.MEDIUM_DASH_DOT_DOT);
        bordMap.put(12, BorderStyle.SLANTED_DASH_DOT);
        bordMap.put(13, BorderStyle.THICK);

        //一定要通过 cell.getCellStyle()  不然的话之前设置的样式会丢失
        //设置边框
        for (int i = 0; i < borderInfoObjectList.size(); i++) {
            JSONObject borderInfoObject = (JSONObject) borderInfoObjectList.get(i);
            if ("cell".equals(borderInfoObject.get("rangeType"))) {//单个单元格
                JSONObject borderValueObject = borderInfoObject.getJSONObject("value");

                JSONObject l = borderValueObject.getJSONObject("l");
                JSONObject r = borderValueObject.getJSONObject("r");
                JSONObject t = borderValueObject.getJSONObject("t");
                JSONObject b = borderValueObject.getJSONObject("b");


                int row = borderValueObject.getInteger("row_index");
                int col = borderValueObject.getInteger("col_index");

                XSSFCell cell = sheet.getRow(row).getCell(col);
                XSSFCellStyle xssfCellStyle = cell.getCellStyle();

                if (l != null) {
                    xssfCellStyle.setBorderLeft(bordMap.get((int) l.get("style"))); //左边框
                    XSSFColor color = toColorFromString(l.getString("color"));
                    xssfCellStyle.setLeftBorderColor(color);//左边框颜色
                }
                if (r != null) {
                    xssfCellStyle.setBorderRight(bordMap.get((int) r.get("style"))); //右边框
                    XSSFColor color=toColorFromString(r.getString("color"));
                    xssfCellStyle.setRightBorderColor(color);//右边框颜色
                }
                if (t != null) {
                    xssfCellStyle.setBorderTop(bordMap.get((int) t.get("style"))); //顶部边框
                    XSSFColor color=toColorFromString(t.getString("color"));
                    xssfCellStyle.setTopBorderColor(color);//顶部边框颜色
                }
                if (b != null) {
                    xssfCellStyle.setBorderBottom(bordMap.get((int) b.get("style"))); //底部边框
                    XSSFColor color=toColorFromString(b.getString("color"));
                    xssfCellStyle.setBottomBorderColor(color);
                }
                cell.setCellStyle(xssfCellStyle);
            } else if ("range".equals(borderInfoObject.get("rangeType"))) {//选区
                XSSFColor color=toColorFromString(borderInfoObject.getString("color"));
                int style_ = borderInfoObject.getInteger("style");

                JSONObject rangObject = (JSONObject) ((JSONArray) (borderInfoObject.get("range"))).get(0);

                JSONArray rowList = rangObject.getJSONArray("row");
                JSONArray columnList = rangObject.getJSONArray("column");


                for (int row_ = rowList.getInteger(0); row_ < rowList.getInteger(rowList.size() - 1) + 1; row_++) {
                    for (int col_ = columnList.getInteger(0); col_ < columnList.getInteger(columnList.size() - 1) + 1; col_++) {
                        if (sheet.getRow(row_) == null){
                            sheet.createRow(row_);
                        }
                        if (sheet.getRow(row_).getCell(col_) == null){
                            sheet.getRow(row_).createCell(col_);
                        }
                        XSSFCell cell = sheet.getRow(row_).getCell(col_);
                        XSSFCellStyle xssfCellStyle = cell.getCellStyle();
                        xssfCellStyle.setBorderLeft(bordMap.get(style_)); //左边框
                        xssfCellStyle.setLeftBorderColor(color);//左边框颜色
                        xssfCellStyle.setBorderRight(bordMap.get(style_)); //右边框
                        xssfCellStyle.setRightBorderColor(color);//右边框颜色
                        xssfCellStyle.setBorderTop(bordMap.get(style_)); //顶部边框
                        xssfCellStyle.setTopBorderColor(color);//顶部边框颜色
                        xssfCellStyle.setBorderBottom(bordMap.get(style_)); //底部边框
                        xssfCellStyle.setBottomBorderColor(color);//底部边框颜色 }
                        cell.setCellStyle(xssfCellStyle);
                    }
                }


            }
        }
    }

    /**
     * 设置数据筛选
     * @param dataVerification 数据筛选规则
     * @param sheet
     */
    private static void settDataValidation(JSONObject dataVerification, XSSFSheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        Map<String, Integer> opTypeMap = new HashMap<>();
        opTypeMap.put("bw",DVConstraint.OperatorType.BETWEEN);//"bw"(介于)
        opTypeMap.put("nb",DVConstraint.OperatorType.NOT_BETWEEN);//"nb"(不介于)
        opTypeMap.put("eq",DVConstraint.OperatorType.EQUAL);//"eq"(等于)
        opTypeMap.put("ne",DVConstraint.OperatorType.NOT_EQUAL);//"ne"(不等于)
        opTypeMap.put("gt",DVConstraint.OperatorType.GREATER_THAN);//"gt"(大于)
        opTypeMap.put("lt",DVConstraint.OperatorType.LESS_THAN);//lt"(小于)
        opTypeMap.put("gte",DVConstraint.OperatorType.GREATER_OR_EQUAL);//"gte"(大于等于)
        opTypeMap.put("lte",DVConstraint.OperatorType.LESS_OR_EQUAL);//"lte"(小于等于)
        opTypeMap.put("number",DVConstraint.ValidationType.ANY);//数字
        opTypeMap.put("number_integer",DVConstraint.ValidationType.INTEGER);//整数
        opTypeMap.put("number_decimal",DVConstraint.ValidationType.DECIMAL);//小数
        opTypeMap.put("text_length",DVConstraint.ValidationType.TEXT_LENGTH);//文本长度
        opTypeMap.put("date",DVConstraint.ValidationType.DATE);//日期
        if (dataVerification != null){
            Map<String, Object> dataVe=dataVerification.getInnerMap();
            for(Map.Entry<String, Object> dataEntry : dataVe.entrySet()) {
                String[] colRow = dataEntry.getKey().split("_");
                CellRangeAddressList dstAddrList = new CellRangeAddressList(Integer.parseInt(colRow[0]), Integer.parseInt(colRow[0]), Integer.parseInt(colRow[1]), Integer.parseInt(colRow[1]));// 规则一单元格范围
                JSONObject dataVeValue = (JSONObject) dataEntry.getValue();
                DataValidation dstDataValidation = null;
                if ("dropdown".equals(dataVeValue.getString("type"))){
                    if(dataVeValue.getString("value1").contains(",")){
                        String[] textlist = dataVeValue.getString("value1").split(",");
                        dstDataValidation = helper.createValidation(helper.createExplicitListConstraint(textlist), dstAddrList);
                    }else {
                        dstDataValidation = helper.createValidation(helper.createFormulaListConstraint(dataVeValue.getString("value1")), dstAddrList);
                    }
                }
                if ("checkbox".equals(dataVeValue.getString("type"))){
                    // TODO: 2020/11/30
                }
                if ("number".equals(dataVeValue.getString("type"))){
                    //number判断是整数还是小数
                    Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                    Boolean booleanValue1 = false;
                    Boolean booleanValue2 = false;
                    booleanValue1 = pattern.matcher(dataVeValue.getString("value1")).matches();
                    booleanValue2 = pattern.matcher(dataVeValue.getString("value2")).matches();
                    DataValidationConstraint dvc = null;
                    if (booleanValue1 && booleanValue2){
                        dvc = helper.createIntegerConstraint(opTypeMap.get(dataVeValue.getString("type2")), dataVeValue.getString("value1"), dataVeValue.getString("value2"));
                    }else {
                        dvc = helper.createDecimalConstraint(opTypeMap.get(dataVeValue.getString("type2")), dataVeValue.getString("value1"), dataVeValue.getString("value2"));
                    }
                    dstDataValidation = helper.createValidation(dvc, dstAddrList);
                }
                if ("number_integer".equals(dataVeValue.getString("type"))
                        ||"number_decimal".equals(dataVeValue.getString("type"))
                        ||"text_length".equals(dataVeValue.getString("type"))){
                    DataValidationConstraint dvc = helper.createNumericConstraint(opTypeMap.get(dataVeValue.getString("type")), opTypeMap.get(dataVeValue.getString("type2")), dataVeValue.getString("value1"), dataVeValue.getString("value2"));
                    dstDataValidation = helper.createValidation(dvc, dstAddrList);
                }
                if ("date".equals(dataVeValue.getString("type"))){
                    //日期
                    DataValidationConstraint dvc = new XSSFDataValidationConstraint(opTypeMap.get(dataVeValue.getString("type")), opTypeMap.get(dataVeValue.getString("type2")), dataVeValue.getString("value1"), dataVeValue.getString("value2"));
                    dstDataValidation = helper.createValidation(dvc, dstAddrList);
                }
                if ("text_content".equals(dataVeValue.getString("type"))){
                    // TODO: 2020/11/30

                }
                if ("validity".equals(dataVeValue.getString("type"))){
                    // TODO: 2020/12/1
                }
                dstDataValidation.createPromptBox("提示:", dataVeValue.getString("hintText"));
                dstDataValidation.setShowErrorBox(dataVeValue.getBoolean("prohibitInput"));
                dstDataValidation.setShowPromptBox(dataVeValue.getBoolean("hintShow"));
                sheet.addValidationData(dstDataValidation);
            }
        }
//        CellReference cr = new CellReference("A1");
    }

}
