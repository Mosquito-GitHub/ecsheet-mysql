package com.mars.ecsheet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mars.ecsheet.entity.WorkBookEntity;
import com.mars.ecsheet.entity.WorkSheetEntity;
import com.mars.ecsheet.repository.WorkBookRepository;
import com.mars.ecsheet.repository.WorkSheetRepository;
import com.mars.ecsheet.service.IMessageProcess;
import com.mars.ecsheet.utils.SheetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Mars
 * @date 2020/10/28
 * @description
 */
@Slf4j
@Service
public class MessageProcess implements IMessageProcess {


    @Autowired
    private WorkSheetRepository workSheetRepository;

    @Autowired
    private WorkBookRepository workBookRepository;

    @Override
    public void process(String wbId, JSONObject message) {
        //获取操作名
        String action = message.getString("t");
        //获取sheet的index值
        String index = message.getString("i");

        //如果是复制sheet，index的值需要另取
        if ("shc".equals(action)) {
            index = message.getJSONObject("v").getString("copyindex");
        }

        //如果是删除sheet，index的值需要另取
        if ("shd".equals(action)) {
            index = message.getJSONObject("v").getString("deleIndex");
        }

        //如果是恢复sheet，index的值需要另取
        if ("shre".equals(action)) {
            index = message.getJSONObject("v").getString("reIndex");
        }
        WorkSheetEntity ws = null;
        if (StringUtils.hasText(index)) {
            ws = workSheetRepository.findByindexAndwbId(index, wbId);
        }

        switch (action) {
            //单个单元格刷新
            case "v":
                ws = singleCellRefresh(ws, message);
                break;
            //范围单元格刷新
            case "rv":
                ws = rangeCellRefresh(ws, message);
                break;
            //config操作
            case "cg":
                ws = configRefresh(ws, message);
                break;
            //通用保存
            case "all":
                ws = allRefresh(ws, message);
                break;
            //函数链操作
            case "fc":
                ws = calcChainRefresh(ws, message);
                break;
            //删除行或列
            case "drc":
                ws = drcRefresh(ws, message);
                break;
            //增加行或列
            case "arc":
                ws = arcRefresh(ws, message);
                break;
            //清除筛选
            case "fsc":
                ws = fscRefresh(ws, message);
                break;
            //恢复筛选
            case "fsr":
                ws = fscRefresh(ws, message);
                break;
            //新建sheet
            case "sha":
                ws = shaRefresh(wbId, message);
                break;
            //切换到指定sheet
            case "shs":
                shsRefresh(wbId, message);
                break;
            //复制sheet
            case "shc":
                ws = shcRefresh(ws, message);
                break;
            //修改工作簿名称
            case "na":
                naRefresh(wbId, message);
                break;
            //删除sheet
            case "shd":
                ws.setDeleteStatus(1);
                break;
            //删除sheet后恢复操作
            case "shre":
                ws.setDeleteStatus(0);
                break;
            //调整sheet位置
            case "shr":
                shrRefresh(wbId, message);
                break;
            //sheet属性(隐藏或显示)
            case "sh":
                ws = shRefresh(ws, message);
                break;
            default:
                break;
        }
        if (ObjectUtils.isEmpty(ws)) {
            return;
        }
        workSheetRepository.save(ws);

    }

    /**
     * 单个单元格刷新
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity singleCellRefresh(WorkSheetEntity ws, JSONObject message) {
        //对celldata进行深拷贝
        JSONArray celldata = JSON.parseArray(ws.getData().getJSONArray("celldata").toJSONString());
        if (!StringUtils.hasText(message.getString("v"))) {
            celldata.forEach(c -> {
                JSONObject jsonObject = (JSONObject)JSON.toJSON(c);
                if (!jsonObject.isEmpty()) {
                    if (jsonObject.getLong("r") == message.getLong("r") && jsonObject.getLong("c") == message.getLong("c")) {
                        ws.getData().getJSONArray("celldata").remove(jsonObject);
                    }
                }
            });
        } else {
            Object vValue = message.getString("v").startsWith("{")?message.getJSONObject("v"):message.getString("v");
            JSONObject collectData = new JSONObject();
            collectData.put("r", message.getLong("r"));
            collectData.put("c", message.getLong("c"));
            collectData.put("v", vValue);

            List<String> flag = new ArrayList<>();
            celldata.forEach(c -> {
                JSONObject jsonObject = (JSONObject)JSON.toJSON(c);
                if (!jsonObject.isEmpty()) {
                    if (jsonObject.getLong("r") == message.getLong("r") && jsonObject.getLong("c") == message.getLong("c")) {
                        ws.getData().getJSONArray("celldata").remove(jsonObject);
                        ws.getData().getJSONArray("celldata").add(collectData);
                        flag.add("used");
                    }
                }
            });
            if (flag.isEmpty()) {
                ws.getData().getJSONArray("celldata").add(collectData);
            }
        }
        return ws;
    }


    /**
     * 范围单元格刷新
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity rangeCellRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONArray rowArray = message.getJSONObject("range").getJSONArray("row");
        JSONArray columnArray = message.getJSONObject("range").getJSONArray("column");
        JSONArray vArray = message.getJSONArray("v");
        JSONArray celldata = JSON.parseArray(ws.getData().getJSONArray("celldata").toJSONString());
        int countRowIndex = 0;

        //遍历行列，对符合行列的内容进行更新
        for (int ri = (int) rowArray.get(0); ri <= (int) rowArray.get(1); ri++) {
            int countColumnIndex = 0;
            for (int ci = (int) columnArray.get(0); ci <= (int) columnArray.get(1); ci++) {
                List<String> flag = new ArrayList<>();
                Object newCell = JSON.parseArray(JSON.toJSONString(vArray.get(countRowIndex))).get(countColumnIndex);
                JSONObject collectData = new JSONObject();
                collectData.put("r", ri);
                collectData.put("c", ci);
                collectData.put("v", newCell);
                int rowIndex = ri;
                int columnIndex = ci;
                celldata.forEach(cell -> {
                    JSONObject jsonObject = (JSONObject)JSON.toJSON(cell);

                    if (!jsonObject.isEmpty()) {
                        if (jsonObject.getInteger("r") == rowIndex && jsonObject.getInteger("c") == columnIndex) {
                            if (ObjectUtils.isEmpty(newCell)) {
                                ws.getData().getJSONArray("celldata").remove(jsonObject);
                            } else {
                                ws.getData().getJSONArray("celldata").remove(jsonObject);
                                ws.getData().getJSONArray("celldata").add(collectData);

                            }
                            flag.add("used");
                        }
                    }
                });
                if (flag.isEmpty() && !ObjectUtils.isEmpty(newCell)) {
                    ws.getData().getJSONArray("celldata").add(collectData);
                }
                countColumnIndex++;
            }
            countRowIndex++;
        }


        return ws;
    }


    /**
     * config更新
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity configRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONObject v = message.getJSONObject("v");
        JSONObject newConfig = new JSONObject();
        newConfig.put(message.getString("k"), v);
        if (ws.getData().getJSONObject("config").isEmpty()) {
            ws.getData().put("config", newConfig);
        } else {
            ws.getData().getJSONObject("config").put(message.getString("k"), v);
        }

        return ws;
    }


    /**
     * 通用保存
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity allRefresh(WorkSheetEntity ws, JSONObject message) {
        String temp = message.getString("v");
        if(temp.isEmpty()){
            ws.getData().remove(message.getString("k"));
        }else{
            if (JSON.isValid(temp)){
                ws.getData().put(message.getString("k"), message.get("v"));
            }else{
                ws.getData().put(message.getString("k"), temp);
            }
        }

        return ws;
    }


    /**
     * 函数链操作
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity calcChainRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONObject value = message.getJSONObject("v");
        if (!ws.getData().containsKey("calcChain")) {
            ws.getData().put("calcChain", new JSONArray());
        }
        JSONArray calcChain = ws.getData().getJSONArray("calcChain");
        if ("add".equals(message.getString("op"))) {
            calcChain.add(value);
        } else if ("update".equals(message.getString("op"))) {
            calcChain.remove(calcChain.get(message.getInteger("pos")));
            calcChain.add(value);
        } else if ("del".equals(message.getString("op"))) {
            calcChain.remove(calcChain.get(message.getInteger("pos")));
        }
        return ws;
    }


    /**
     * 删除行或列
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity drcRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONArray celldata = JSON.parseArray(ws.getData().getJSONArray("celldata").toJSONString());
        int index = message.getJSONObject("v").getInteger("index");
        int len = message.getJSONObject("v").getInteger("len");
        if ("r".equals(message.getString("rc"))) {
            ws.getData().put("row", ws.getData().getInteger("row") - len);
        } else {
            ws.getData().put("column", ws.getData().getInteger("column") - len);
        }
        for (Object cell : celldata) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(cell);
            if ("r".equals(message.getString("rc"))) {
                //删除行所在区域的内容
                if (jsonObject.getInteger("r") >= index && jsonObject.getInteger("r") < index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                }
                //增加大于 最大删除行的的行号
                if (jsonObject.getInteger("r") >= index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("r", jsonObject.getInteger("r") - len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
            } else {
                //删除列所在区域的内容
                if (jsonObject.getInteger("c") >= index && jsonObject.getInteger("c") < index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                }
                //增加大于 最大删除列的的列号
                if (jsonObject.getInteger("c") >= index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("c", jsonObject.getInteger("c") - len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
            }
        }

        return ws;
    }


    /**
     * 增加行或列,暂未实现插入数据的情况
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity arcRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONArray celldata = JSON.parseArray(ws.getData().getJSONArray("celldata").toJSONString());
        int index = message.getJSONObject("v").getInteger("index");
        int len = message.getJSONObject("v").getInteger("len");

        for (Object cell : celldata) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(cell);
            if ("r".equals(message.getString("rc"))) {
                //如果是增加行，且是向左增加
                if (jsonObject.getInteger("r") >= index && "lefttop".equals(message.getJSONObject("v").getString("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("r", jsonObject.getInteger("r") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
                //如果是增加行，且是向右增加
                if (jsonObject.getInteger("r") > index && "rightbottom".equals(message.getJSONObject("v").getString("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("r", jsonObject.getInteger("r") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }


            } else {
                //如果是增加列，且是向上增加
                if (jsonObject.getInteger("c") >= index && "lefttop".equals(message.getJSONObject("v").getString("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("c", jsonObject.getInteger("c") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
                //如果是增加列，且是向下增加
                if (jsonObject.getInteger("c") > index && "rightbottom".equals(message.getJSONObject("v").getString("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("c", jsonObject.getInteger("c") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }

            }
        }
        JSONArray vArray = message.getJSONObject("v").getJSONArray("data");
        if ("r".equals(message.getString("rc"))) {
            ws.getData().put("row", ws.getData().getInteger("row") + len);
            for (int r = 0; r < vArray.size(); r++) {
                for (int c = 0; c < ((JSONArray)JSON.toJSON(vArray.get(0))).size(); c++) {
                    if (vArray.getJSONArray(r).get(c) == null) {
                        continue;
                    }
                    JSONObject newCell = new JSONObject();
                    newCell.put("r", r + index);
                    newCell.put("c", c);
                    newCell.put("v", vArray.getJSONArray(r).get(c));
                    ws.getData().getJSONArray("celldata").add(newCell);
                }
            }

        } else {
            ws.getData().put("column", ws.getData().getInteger("column") + len);
            for (int r = 0; r < vArray.size(); r++) {
                for (int c = 0; c < vArray.getJSONArray(0).size(); c++) {
                    if (vArray.getJSONArray(r).get(c) == null) {
                        continue;
                    }
                    JSONObject newCell = new JSONObject();
                    newCell.put("r", r);
                    newCell.put("c", c + index);
                    newCell.put("v", vArray.getJSONArray(r).get(c));
                    ws.getData().getJSONArray("celldata").add(newCell);
                }
            }
        }


        return ws;
    }


    /**
     * 筛选操作
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity fscRefresh(WorkSheetEntity ws, JSONObject message) {

        if (message.getJSONObject("v").isEmpty()) {
            ws.getData().remove("filter");
            ws.getData().remove("filter_select");
        } else {
            ws.getData().put("filter", message.getJSONObject("v").getJSONArray("filter"));
            ws.getData().put("filter_select", message.getJSONObject("v").getJSONObject("filter_select"));
        }
        return ws;
    }


    /**
     * 新建sheet
     *
     * @param wbId
     * @param message
     * @return
     */
    private WorkSheetEntity shaRefresh(String wbId, JSONObject message) {
        WorkSheetEntity ws = new WorkSheetEntity();
        ws.setWbId(wbId);
        ws.setId(null);
        ws.setData(message.getJSONObject("v"));
        return ws;
    }


    /**
     * 复制sheet
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity shcRefresh(WorkSheetEntity ws, JSONObject message) {

        String index = message.getString("i");
        ws.setId(null);
        ws.getData().put("index", index);
        ws.getData().put("name", message.getJSONObject("v").getString("name"));
        return ws;
    }

    /**
     * 调整sheet位置
     *
     * @param wbId
     * @param message
     */
    private void shrRefresh(String wbId, JSONObject message) {
        List<WorkSheetEntity> allSheets = workSheetRepository.findAllBywbId(wbId);

        allSheets.forEach(sheet -> {
            sheet.getData().put("order", message.getJSONObject("v").getInteger(sheet.getData().getString("index")));
            workSheetRepository.save(sheet);
        });

    }

    /**
     * 切换到指定sheet
     *
     * @param wbId
     * @param message
     * @return
     */
    private void shsRefresh(String wbId, JSONObject message) {
        List<WorkSheetEntity> lastWsList = workSheetRepository.findBystatusAndwbId(1, wbId);
        if(CollectionUtils.isEmpty(lastWsList))return;
        WorkSheetEntity lastWs = lastWsList.get(0);
        lastWs.getData().put("status", 0);
        WorkSheetEntity thisWs = workSheetRepository.findByindexAndwbId(message.getString("v"), wbId);
        thisWs.getData().put("status", 1);
        workSheetRepository.save(lastWs);
        workSheetRepository.save(thisWs);
    }


    /**
     * sheet属性(隐藏或显示)
     *
     * @param ws
     * @param message
     */
    private WorkSheetEntity shRefresh(WorkSheetEntity ws, JSONObject message) {
        Integer hideStatus = message.getInteger("v");
        ws.getData().put("hide", hideStatus);

        WorkSheetEntity curWs = new WorkSheetEntity();

        if ("hide".equals(message.getString("op"))) {
            ws.getData().put("status", 0);
            String cur = message.getString("cur");
            curWs =  workSheetRepository.findByindexAndwbId(cur, ws.getWbId());
            curWs.getData().put("status", 1);

        } else {
            List<WorkSheetEntity> workSheetEntityList = workSheetRepository.findBystatusAndwbId(1, ws.getWbId());
            if (!CollectionUtils.isEmpty(workSheetEntityList)) {
                curWs = workSheetEntityList.get(0);
                curWs.getData().put("status", 0);
            }
        }

        workSheetRepository.save(curWs);
        return ws;
    }

    /**
     * 修改工作簿名称
     *
     * @param wbId
     * @param message
     * @return
     */
    private void naRefresh(String wbId, JSONObject message) {
        Optional<WorkBookEntity> wb = workBookRepository.findById(wbId);
        if (wb.isPresent()) {
            WorkBookEntity workBookEntity = wb.get();
            workBookEntity.getOption().put("title", message.getString("v"));
            workBookRepository.save(workBookEntity);
        }
    }


}
