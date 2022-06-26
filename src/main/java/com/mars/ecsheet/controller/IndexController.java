package com.mars.ecsheet.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mars.ecsheet.common.UploadDataDto;
import com.mars.ecsheet.entity.WorkBookEntity;
import com.mars.ecsheet.entity.WorkSheetEntity;
import com.mars.ecsheet.repository.WorkBookRepository;
import com.mars.ecsheet.repository.WorkSheetRepository;
import com.mars.ecsheet.utils.ExcelUtils;
import com.mars.ecsheet.utils.SheetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mars
 * @date 2020/10/28
 * @description
 */
@Slf4j
@RestController
public class IndexController {
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy年MM月");
    @Autowired
    private WorkBookRepository workBookRepository;

    @Autowired
    private WorkSheetRepository workSheetRepository;

    @GetMapping("index")
    public ModelAndView index() {
        List<WorkBookEntity> all = workBookRepository.findAll();
        return new ModelAndView("index", "all", all);
    }

    @GetMapping("import")
    public ModelAndView importdata() {

        return new ModelAndView("import", "import", null);
    }

    @PostMapping("index/upload")
    public String createwithData(HttpServletRequest request, HttpServletResponse response, @RequestBody UploadDataDto uploadData) throws IOException {
        WorkBookEntity wb = new WorkBookEntity();

        wb.setName(format.format(LocalDateTime.now()));
        JSONObject defautOption = SheetUtil.getDefautOption();
        defautOption.put("title", format.format(LocalDateTime.now()));
        wb.setOption(defautOption);
        workBookRepository.save(wb);
        //生成sheet数据
        log.debug("upload data:{}", JSON.toJSONString(uploadData));
        generateSheet(SheetUtil.splitSheet(uploadData.getExceldatas()), wb.getId());
        return wb.getId();
    }
    @GetMapping("index/export/{wbId}")
    public void exportExcle(HttpServletRequest request, HttpServletResponse response,@PathVariable(value = "wbId") String wbId){
        Optional<WorkBookEntity> owb = workBookRepository.findById(wbId);
        if (owb.isPresent()) {
            WorkBookEntity workBookEntity = owb.get();
            List<WorkSheetEntity> workSheetEntityList = workSheetRepository.findAllBywbId(workBookEntity.getId());
            List<JSONObject> cellDatas = workSheetEntityList.stream().map(WorkSheetEntity::getData).collect(Collectors.toList());
            ExcelUtils.exportLuckySheetXlsx(JSON.toJSONString(SheetUtil.mergeSheet(cellDatas)),workBookEntity.getName(),request,response);
        }
    }


    @GetMapping("index/create")
    public void create(HttpServletRequest request, HttpServletResponse response) throws IOException {
        WorkBookEntity wb = new WorkBookEntity();

        wb.setName("default");
        wb.setOption(SheetUtil.getDefautOption());
        workBookRepository.save(wb);
        //生成sheet数据
        generateSheet(wb.getId());
        response.sendRedirect("/index/" + wb.getId());
    }


    @GetMapping("/index/{wbId}")
    public ModelAndView index(@PathVariable(value = "wbId") String wbId) {
        Optional<WorkBookEntity> Owb = workBookRepository.findById(wbId);
        WorkBookEntity wb = new WorkBookEntity();
        if (!Owb.isPresent()) {
            wb.setId(wbId);
            wb.setName("default");
            wb.setOption(SheetUtil.getDefautOption());
            WorkBookEntity result = workBookRepository.save(wb);
            generateSheet(wbId);
        } else {
            wb = Owb.get();
        }

        return new ModelAndView("websocket", "wb", wb);
    }

    @GetMapping("delete/{wbId}")
    public Object delete(HttpServletRequest request, HttpServletResponse response, @PathVariable(value = "wbId") String wbId) throws IOException {
        Optional<WorkBookEntity> owb = workBookRepository.findById(wbId);
        if (owb.isPresent()) {
            WorkBookEntity workBookEntity = owb.get();
            List<WorkSheetEntity> workSheetEntityList = workSheetRepository.findAllBywbId(workBookEntity.getId());
            workBookRepository.deleteById(workBookEntity.getId());
            workSheetEntityList.forEach(workSheetEntity -> {
                workSheetRepository.delete(workSheetEntity);
            });
        }

        return "success";
    }

    @PostMapping("/load/{wbId}")
    public String load(@PathVariable(value = "wbId") String wbId) {

        List<WorkSheetEntity> wsList = workSheetRepository.findAllBywbId(wbId);
        List<JSONObject> list = new ArrayList<>();
        wsList.forEach(ws -> {
            list.add(ws.getData());
        });


        return JSON.toJSONString(list);
    }


    @PostMapping("/loadSheet/{wbId}")
    public String loadSheet(@PathVariable(value = "wbId") String wbId) {
        List<WorkSheetEntity> wsList = workSheetRepository.findAllBywbId(wbId);
        List<JSONObject> list = new ArrayList<>();
        wsList.forEach(ws -> {
            list.add(ws.getData());
        });
        if (!list.isEmpty()) {
            return SheetUtil.buildSheetData(list).toString();
        }
        return SheetUtil.getDefaultAllSheetData().toString();
    }


    private void generateSheet(String wbId) {
        SheetUtil.getDefaultSheetData().forEach(jsonObject -> {
            WorkSheetEntity ws = new WorkSheetEntity();
            ws.setData((JSONObject) JSON.toJSON(jsonObject));
            ws.setDeleteStatus(0);
            ws.setWbId(wbId);
            workSheetRepository.save(ws);
        });
    }

    private void generateSheet(List<JSONObject> sheetData, String wbId) {
        log.info("sheet data size: {}", sheetData.size());
        sheetData.forEach(jsonObject -> {
            WorkSheetEntity ws = new WorkSheetEntity();
            ws.setWbId(wbId);
            ws.setData(jsonObject);
            ws.setDeleteStatus(0);
            workSheetRepository.save(ws);
        });
    }



//
//    public static void main(String[] args) {
//        String json = "{\"exceldatas\":[{\"luckysheet_select_save\":[{\"row\":[22,22],\"column\":[3,3],\"sheetIndex\":1}],\"defaultColWidth\":72,\"defaultRowHeight\":19,\"showGridLines\":\"1\",\"calcChain\":[],\"name\":\"Sheet1\",\"index\":\"1\",\"celldata\":[{\"r\":0,\"c\":3,\"v\":{\"tb\":1,\"v\":\"项目\",\"qp\":1}},{\"r\":0,\"c\":4,\"v\":{\"tb\":1,\"v\":\"智能家电云平台基础业务建设项目\",\"qp\":1}},{\"r\":0,\"c\":5,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-公共\",\"qp\":1}},{\"r\":0,\"c\":6,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-视频\",\"qp\":1}},{\"r\":0,\"c\":7,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-游戏\",\"qp\":1}},{\"r\":0,\"c\":8,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-应用\",\"qp\":1}},{\"r\":0,\"c\":9,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-K歌\",\"qp\":1}},{\"r\":0,\"c\":10,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-音乐\",\"qp\":1}},{\"r\":0,\"c\":11,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-购物\",\"qp\":1}},{\"r\":0,\"c\":12,\"v\":{\"tb\":1,\"v\":\" 移动APP \",\"qp\":1}},{\"r\":0,\"c\":13,\"v\":{\"tb\":1,\"v\":\" 海信手机运营 \",\"qp\":1}},{\"r\":0,\"c\":14,\"v\":{\"tb\":1,\"v\":\"有线宝\",\"qp\":1}},{\"r\":0,\"c\":15,\"v\":{\"tb\":1,\"v\":\"海虹影院\",\"qp\":1}},{\"r\":0,\"c\":16,\"v\":{\"tb\":1,\"v\":\" 第三方用户-视频 \",\"qp\":1}},{\"r\":0,\"c\":17,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-广告\",\"qp\":1}},{\"r\":0,\"c\":18,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-少儿\",\"qp\":1}},{\"r\":0,\"c\":19,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-大屏端教育\",\"qp\":1}},{\"r\":0,\"c\":20,\"v\":{\"tb\":1,\"v\":\" 第三方用户-教育 \",\"qp\":1}},{\"r\":0,\"c\":21,\"v\":{\"tb\":1,\"v\":\" 移动端-教育 \",\"qp\":1}},{\"r\":0,\"c\":22,\"v\":{\"tb\":1,\"v\":\"家校产品\",\"qp\":1}},{\"r\":0,\"c\":23,\"v\":{\"tb\":1,\"v\":\"海外产品\",\"qp\":1}},{\"r\":0,\"c\":24,\"v\":{\"tb\":1,\"v\":\"其他内部技术服务\",\"qp\":1}},{\"r\":0,\"c\":25,\"v\":{\"tb\":1,\"v\":\"湖北广电产品投入\",\"qp\":1}},{\"r\":0,\"c\":26,\"v\":{\"tb\":1,\"v\":\"青岛广电产品投入\",\"qp\":1}},{\"r\":0,\"c\":27,\"v\":{\"tb\":1,\"v\":\"云视频\",\"qp\":1}},{\"r\":0,\"c\":28,\"v\":{\"tb\":1,\"v\":\"AR/VR\",\"qp\":1}},{\"r\":0,\"c\":29,\"v\":{\"tb\":1,\"v\":\"电视周边类\",\"qp\":1}},{\"r\":0,\"c\":30,\"v\":{\"tb\":1,\"v\":\" JUCLOUD \",\"qp\":1}},{\"r\":0,\"c\":31,\"v\":{\"tb\":1,\"v\":\"生活电子类\",\"qp\":1}},{\"r\":0,\"c\":32,\"v\":{\"tb\":1,\"v\":\"VIDAA电视\",\"qp\":1}},{\"r\":1,\"c\":5,\"v\":{\"ct\":{\"fa\":\"General\",\"t\":\"inlineStr\",\"s\":[{\"v\":\"AIoT产品化；APM一期；\\r\\nCDN去单一提供商；Hybridge 2.0.2；\\r\\nJuAPI网关V1.0研发内部项目立项书；\\r\\nJUUI支持根据机型版本显示差异化预置头像紧急需求；\\r\\nJUUI性能攻关二期预研；\\r\\n插件细粒度化；JUUI终端播放UI自动化；\\r\\n终端首页+模板+Middleware重构；UI动效预研；\\r\\nJUUI 7.1（详情页及播放器交互优化）；\\r\\n收银台2.0；JUUI7适配；\\r\\n够级面对面游戏1.0；JUUI7.0；公有云成本管控项目；\\r\\n内容子系统-媒资全链路审核和整改提效1.3、1.2、1.1；\\r\\nAmlogic T963芯片适配验证；\\r\\n国内升级子系统2022功能演进；小聚识图V3.5；\\r\\n异地双活故障切换2期；\\r\\n权益稳定率日志上报优化项目；个性化推荐关键技术研究；\\r\\n收银台移动端优化V2.1；IP漂移专项攻关项目；\\r\\n混合云自动扩缩容降成本项目：JUUI平台化改造；\\r\\n自研播放器优化预研；设备版本基础信息及其应用平台1.1；\\r\\n运营后台安全整改1.0；五级审核和运营提效1.0版本；\\r\\n新芯片机型E5H-M9653-HQH适配JUUI；\\r\\n首页可用性99.99\\r\\n\\r\\n\\r\\n\",\"ff\":\"等线\",\"fc\":\"#000000\",\"fs\":11}]},\"fs\":11,\"fc\":\"#000000\",\"ff\":\"等线\",\"tb\":2}},{\"r\":1,\"c\":6,\"v\":{\"tb\":1,\"v\":\"JUUI芒果牌照适配\",\"qp\":1}},{\"r\":1,\"c\":9,\"v\":{\"tb\":1,\"v\":\"K歌体验提升专项\\u2014\\u2014产品功能及性能优化\",\"qp\":1}},{\"r\":1,\"c\":12,\"v\":{\"tb\":1,\"v\":\"手机聚好看5.9\",\"qp\":1}},{\"r\":1,\"c\":14,\"v\":{\"tb\":1,\"v\":\"有线宝（甘肃省份）适配攻关\",\"qp\":1}},{\"r\":1,\"c\":17,\"v\":{\"tb\":1,\"v\":\"广告ADService V4.6\",\"qp\":1}},{\"r\":1,\"c\":19,\"v\":{\"tb\":1,\"v\":\"教育产品V5.1\",\"qp\":1}},{\"r\":1,\"c\":21,\"v\":{\"tb\":1,\"v\":\" 教育移动端3.9.0 \",\"qp\":1}},{\"r\":1,\"c\":23,\"v\":{\"tb\":1,\"v\":\"海外Android和VIDAA云平台拆分\",\"qp\":1}},{\"r\":1,\"c\":24,\"v\":{\"tb\":1,\"v\":\"冰箱屏智慧生活跳转页面更改\",\"qp\":1}},{\"r\":1,\"c\":27,\"v\":{\"tb\":1,\"v\":\"CDN降成本、多CDN融合\",\"qp\":1}},{\"r\":1,\"c\":28,\"v\":{\"tb\":1,\"v\":\"云XR直播平台2.1.0\",\"qp\":1}},{\"r\":1,\"c\":30,\"v\":{\"tb\":1,\"v\":\"Jucloud1.3.1\",\"qp\":1}},{\"r\":2,\"c\":6,\"v\":{\"tb\":1,\"v\":\"腾讯切换至64位账号\",\"qp\":1}},{\"r\":2,\"c\":19,\"v\":{\"tb\":1,\"v\":\"健身V3.0版本（联屏健身）\",\"qp\":1}},{\"r\":2,\"c\":21,\"v\":{\"tb\":1,\"v\":\" 知渔学堂2.3 \",\"qp\":1}},{\"r\":2,\"c\":23,\"v\":{\"tb\":1,\"v\":\"海外Vidaa按月迭代\",\"qp\":1}},{\"r\":2,\"c\":24,\"v\":{\"tb\":1,\"v\":\"商显应用商店1.2.0\",\"qp\":1}},{\"r\":2,\"c\":27,\"v\":{\"tb\":1,\"v\":\"云视频平台项目V1.2.8\",\"qp\":1}},{\"r\":2,\"c\":28,\"v\":{\"tb\":1,\"v\":\"云VR直播平台2.0\",\"qp\":1}},{\"r\":2,\"c\":30,\"v\":{\"tb\":1,\"v\":\"JuDB产业集团重点项目\",\"qp\":1}},{\"r\":3,\"c\":19,\"v\":{\"tb\":1,\"v\":\"健身3.0.1\",\"qp\":1}},{\"r\":3,\"c\":23,\"v\":{\"tb\":1,\"v\":\"海外在线可用性提升\",\"qp\":1}},{\"r\":3,\"c\":27,\"v\":{\"tb\":1,\"v\":\"聚连会议Windows客户端支持虚拟会议预研\",\"qp\":1}},{\"r\":3,\"c\":28,\"v\":{\"tb\":1,\"v\":\"超写实数字人V1.0\",\"qp\":1}},{\"r\":3,\"c\":30,\"v\":{\"tb\":1,\"v\":\"JuDB2.0.6\",\"qp\":1}},{\"r\":4,\"c\":27,\"v\":{\"tb\":1,\"v\":\"聚连会议组件V1.0\",\"qp\":1}},{\"r\":4,\"c\":28,\"v\":{\"tb\":1,\"v\":\"虚拟社交2.1\",\"qp\":1}},{\"r\":4,\"c\":30,\"v\":{\"tb\":1,\"v\":\"JuDB2.0.5\",\"qp\":1}},{\"r\":5,\"c\":27,\"v\":{\"tb\":1,\"v\":\"Android TV V2.1.0/Android Phone V2.1.1/iOS V2.1.2\",\"qp\":1}},{\"r\":6,\"c\":27,\"v\":{\"tb\":1,\"v\":\"HiRTC平台传输性能优化公有云版本\",\"qp\":1}},{\"r\":7,\"c\":27,\"v\":{\"tb\":1,\"v\":\"聚连课堂1.0\",\"qp\":1}},{\"r\":19,\"c\":0,\"v\":{\"tb\":1,\"v\":\"员工编码\",\"qp\":1}},{\"r\":19,\"c\":1,\"v\":{\"tb\":1,\"v\":\"姓名\",\"qp\":1}},{\"r\":19,\"c\":2,\"v\":{\"tb\":1,\"v\":\"二级部门\",\"qp\":1}},{\"r\":19,\"c\":3,\"v\":{\"tb\":1,\"v\":\"一级部门\",\"qp\":1}},{\"r\":19,\"c\":4,\"v\":{\"tb\":1,\"v\":\"智能家电云平台基础业务建设项目\",\"qp\":1}},{\"r\":19,\"c\":5,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-公共\",\"qp\":1}},{\"r\":19,\"c\":6,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-视频\",\"qp\":1}},{\"r\":19,\"c\":7,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-游戏\",\"qp\":1}},{\"r\":19,\"c\":8,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-应用\",\"qp\":1}},{\"r\":19,\"c\":9,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-K歌\",\"qp\":1}},{\"r\":19,\"c\":10,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-音乐\",\"qp\":1}},{\"r\":19,\"c\":11,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-购物\",\"qp\":1}},{\"r\":19,\"c\":12,\"v\":{\"tb\":1,\"v\":\" 移动APP \",\"qp\":1}},{\"r\":19,\"c\":13,\"v\":{\"tb\":1,\"v\":\" 海信手机运营 \",\"qp\":1}},{\"r\":19,\"c\":14,\"v\":{\"tb\":1,\"v\":\"有线宝\",\"qp\":1}},{\"r\":19,\"c\":15,\"v\":{\"tb\":1,\"v\":\"海虹影院\",\"qp\":1}},{\"r\":19,\"c\":16,\"v\":{\"tb\":1,\"v\":\" 第三方用户-视频 \",\"qp\":1}},{\"r\":19,\"c\":17,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-广告\",\"qp\":1}},{\"r\":19,\"c\":18,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-少儿\",\"qp\":1}},{\"r\":19,\"c\":19,\"v\":{\"tb\":1,\"v\":\"基于云平台的智能电视应用项目-大屏端教育\",\"qp\":1}},{\"r\":19,\"c\":20,\"v\":{\"tb\":1,\"v\":\" 第三方用户-教育 \",\"qp\":1}},{\"r\":19,\"c\":21,\"v\":{\"tb\":1,\"v\":\" 移动端-教育 \",\"qp\":1}},{\"r\":19,\"c\":22,\"v\":{\"tb\":1,\"v\":\"家校产品\",\"qp\":1}},{\"r\":19,\"c\":23,\"v\":{\"tb\":1,\"v\":\"海外产品\",\"qp\":1}},{\"r\":19,\"c\":24,\"v\":{\"tb\":1,\"v\":\"其他内部技术服务\",\"qp\":1}},{\"r\":19,\"c\":25,\"v\":{\"tb\":1,\"v\":\"湖北广电产品投入\",\"qp\":1}},{\"r\":19,\"c\":26,\"v\":{\"tb\":1,\"v\":\"青岛广电产品投入\",\"qp\":1}},{\"r\":19,\"c\":27,\"v\":{\"tb\":1,\"v\":\"云视频\",\"qp\":1}},{\"r\":19,\"c\":28,\"v\":{\"tb\":1,\"v\":\"AR/VR\",\"qp\":1}},{\"r\":19,\"c\":29,\"v\":{\"tb\":1,\"v\":\"电视周边类\",\"qp\":1}},{\"r\":19,\"c\":30,\"v\":{\"tb\":1,\"v\":\" 智能产品类 \",\"qp\":1}},{\"r\":19,\"c\":31,\"v\":{\"tb\":1,\"v\":\"生活电子类\",\"qp\":1}},{\"r\":19,\"c\":32,\"v\":{\"tb\":1,\"v\":\"VIDAA电视\",\"qp\":1}},{\"r\":19,\"c\":33,\"v\":{\"tb\":1,\"v\":\"合计\",\"qp\":1}},{\"r\":19,\"c\":34,\"v\":{\"tb\":1,\"v\":\"备注\",\"qp\":1}},{\"r\":20,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20135027\"}},{\"r\":20,\"c\":1,\"v\":{\"tb\":1,\"v\":\"杜佳昱\",\"qp\":1}},{\"r\":20,\"c\":2,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":20,\"c\":3,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":20,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}},{\"r\":21,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20161936\"}},{\"r\":21,\"c\":1,\"v\":{\"tb\":1,\"v\":\"宋云芳\",\"qp\":1}},{\"r\":21,\"c\":2,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":21,\"c\":3,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":21,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}},{\"r\":22,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20200858\"}},{\"r\":22,\"c\":1,\"v\":{\"tb\":1,\"v\":\"李浩\",\"qp\":1}},{\"r\":22,\"c\":2,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":22,\"c\":3,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":22,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}},{\"r\":23,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20201701\"}},{\"r\":23,\"c\":1,\"v\":{\"tb\":1,\"v\":\"王昭林\",\"qp\":1}},{\"r\":23,\"c\":2,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":23,\"c\":3,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":23,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}},{\"r\":24,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20201743\"}},{\"r\":24,\"c\":1,\"v\":{\"tb\":1,\"v\":\"姚进根\",\"qp\":1}},{\"r\":24,\"c\":2,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":24,\"c\":3,\"v\":{\"tb\":1,\"v\":\"VIDAA事业部\",\"qp\":1}},{\"r\":24,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}},{\"r\":25,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20080800\"}},{\"r\":25,\"c\":1,\"v\":{\"tb\":1,\"v\":\"徐延霞\",\"qp\":1}},{\"r\":25,\"c\":2,\"v\":{\"tb\":1,\"v\":\"云视频产品事业部\",\"qp\":1}},{\"r\":25,\"c\":3,\"v\":{\"tb\":1,\"v\":\"云视频产品事业部\",\"qp\":1}},{\"r\":25,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}},{\"r\":26,\"c\":0,\"v\":{\"tb\":1,\"v\":\"20100556\"}},{\"r\":26,\"c\":1,\"v\":{\"tb\":1,\"v\":\"吴成义\",\"qp\":1}},{\"r\":26,\"c\":2,\"v\":{\"tb\":1,\"v\":\"云视频产品事业部\",\"qp\":1}},{\"r\":26,\"c\":3,\"v\":{\"tb\":1,\"v\":\"云视频产品事业部\",\"qp\":1}},{\"r\":26,\"c\":33,\"v\":{\"tb\":1,\"v\":\" 请检查合计是否为1 \",\"qp\":1}}],\"zoomRatio\":1,\"config\":{\"columnlen\":{\"2\":112,\"33\":169},\"customWidth\":{\"2\":1,\"33\":1}},\"status\":\"1\",\"order\":\"0\"}]}";
//        UploadDataDto uploadDataDto = JSON.parseObject(json, UploadDataDto.class);
//        List<JSONObject> result = splitSheet(uploadDataDto.getExceldatas());
//        log.info("result: {}", JSON.toJSONString(result));
//    }
}
