package cn.linkey.rulelib.S003;

import java.util.HashMap;

import cn.linkey.dao.Rdb;
import cn.linkey.doc.Document;
import cn.linkey.factory.BeanCtx;
import cn.linkey.form.HtmlParser;
import cn.linkey.form.ModForm;
import cn.linkey.rule.LinkeyRule;
import cn.linkey.util.DateUtil;
import cn.linkey.util.Tools;
import cn.linkey.wf.ProcessEngine;

/**
 * 本类主要生成流程处理单的Html Header以及通用操作条
 * 
 * @author Administrator 移动终端专用
 */
public class R_S003_B071 implements LinkeyRule {
    @Override
    public String run(HashMap<String, Object> params) throws Exception {
        ProcessEngine linkeywf = BeanCtx.getLinkeywf();
        Document formDoc = (Document) params.get("FormDoc"); /* 传入的表单模型文档对像 */
        StringBuilder formBody = new StringBuilder(2000); /* 传入的表单HTML代码 */

        /* 组装html header */
        String subject = linkeywf.getIsNewProcess() ? linkeywf.getProcessName() : BeanCtx.getLinkeywf().getDocument().g("Subject");
        formBody.append("<!DOCTYPE html><html><head><title>");
        formBody.append(subject);
        formBody.append("</title>");
        String headerConfigid = formDoc.g("HeaderConfigid");
        if (Tools.isBlank(headerConfigid)) {
            headerConfigid = "ProcessFormHtmlHeader";
        }
        if (BeanCtx.isMobile()) {
            headerConfigid = headerConfigid + "_Mobile"; //移动端表头
        }
        //		if(!BeanCtx.getCountry().equals("CN")){
        //			headerConfigid=headerConfigid.concat("_").concat(BeanCtx.getCountry()); //多语言时
        //		}

        //读取html头文件，如果应用中配置有则以应用优先2015.4.8
        String configHtml = "";
        if (Tools.isNotBlank(linkeywf.getAppid())) {
            configHtml = BeanCtx.getAppConfig(linkeywf.getAppid(), headerConfigid);
        }
        if (Tools.isBlank(configHtml)) {
            configHtml = BeanCtx.getSystemConfig(headerConfigid); //如果为空则到系统通用配置中去找
        }
        configHtml = configHtml.replace("{LANG}", BeanCtx.getUserLocale().getLanguage() + "_" + BeanCtx.getCountry());
        configHtml = configHtml.replace("{version}", BeanCtx.getSystemConfig("static_version"));
        formBody.append(configHtml);
        //读取结束

        formBody.append(formDoc.g("HtmlHeader"));

        /* 获得js header */
        HtmlParser htmlParser = ((HtmlParser) BeanCtx.getBean("HtmlParser"));
        formBody.append("\n<script>\n");
        String jsHeader = formDoc.g("JsHeader");
        if (Tools.isBlank(jsHeader)) {
            jsHeader = Rdb.getValueBySql("Select DefaultCode from BPM_DevDefaultCode where CodeType='ProcessForm_JsHeader'");
        }
        else {
            jsHeader = htmlParser.parserJsTagValue(jsHeader);/* 分析JS标签 */
            jsHeader = htmlParser.parserXTagValue(linkeywf.getDocument(), jsHeader); /* 分析一次x标签的值 */
        }
        formBody.append(jsHeader); /* 主表单的js代码不管是否只读都需要输出 */
        if (!linkeywf.isReadOnly()) {
            /* 只读时因为当前用户没有所处的环节，所以不需要拼接子表单的js代码 */
            String subFormNumber = linkeywf.getCurNodeSubFormList(true); /* 获得当前环节的所有子表单包含处理单 */

            /* 循环所有子表单并追加JsHeader */
            if (Tools.isNotBlank(subFormNumber)) {
                String[] subFormList = Tools.split(subFormNumber, ",");
                for (String formNumber : subFormList) {
                    Document subFormDoc = ((ModForm) BeanCtx.getBean("ModForm")).getFormDoc(formNumber);
                    if (!subFormDoc.isNull()) {
                        jsHeader = htmlParser.parserJsTagValue(subFormDoc.g("JsHeader"));/* 分析JS标签 */
                        jsHeader = htmlParser.parserXTagValue(linkeywf.getDocument(), jsHeader); /* 分析[X][/X]标签 */
                        formBody.append(jsHeader);
                    }
                }
            }
        }

        /* 追加Body标签 */
        formBody.append("\n</script>\n</head>\n<body style=\"margin:30px 0px 0px 0px;\" >\n");

        //		formBody.append(BeanCtx.getSystemConfig("EngineFormTopToolBar_Mobile"));
        formBody.append("<form action='' method='post' name='linkeyform' id='linkeyform' >\n");

        /* 追加流程表单的通用状态条 */
        if (linkeywf.getIsNewProcess()) {
            linkeywf.getDocument().s("WF_DocCreated", DateUtil.getNow());
            linkeywf.getDocument().s("WF_CurrentNodeName", linkeywf.getCurrentNodeName());
            linkeywf.getDocument().s("WF_AddName_CN", BeanCtx.getUserName());
            linkeywf.getDocument().s("WF_DocUnid", linkeywf.getDocUnid());
            linkeywf.getDocument().s("WF_Processid", linkeywf.getProcessid());
            linkeywf.getDocument().s("WF_ProcessName", linkeywf.getProcessName());
        }
        formBody.append(Tools.parserStrByDocument(linkeywf.getDocument(), BeanCtx.getSystemConfig("EngineFormTopBar_Mobile")));

        formBody.trimToSize();
        return formBody.toString(); /* 返回1表示运行成功 */
    }

}
