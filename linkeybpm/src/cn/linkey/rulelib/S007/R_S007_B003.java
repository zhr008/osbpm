package cn.linkey.rulelib.S007;

import java.util.HashMap;

import cn.linkey.dao.Rdb;
import cn.linkey.doc.Document;
import cn.linkey.factory.BeanCtx;
import cn.linkey.org.LinkeyUser;
import cn.linkey.rule.LinkeyRule;
import cn.linkey.util.Tools;

/**
 * @RuleName:缺省职能部部门选择树
 * @author admin
 * @version: 8.0
 * @Created: 2014-04-20 23:11
 */
final public class R_S007_B003 implements LinkeyRule {
    @Override
    public String run(HashMap<String, Object> params) {
        //params为运行本规则时所传入的参数
        String parentid = BeanCtx.g("id");
        String orgClass = BeanCtx.getSystemConfig("DefaultOrgClass");
        StringBuilder jsonStr = new StringBuilder();
        String async = BeanCtx.g("async");
        if (Tools.isBlank(async)) {
            async = "true";
        }
        int i = 0;
        if (Tools.isBlank(parentid)) {
            String sql = "select OrgName,OrgClass from BPM_Organization where OrgClass='" + orgClass + "' order by SortNum";
            Document[] dc = Rdb.getAllDocumentsBySql(sql);
            jsonStr.append("[");
            for (Document doc : dc) {
                if (i == 0) {
                    i = 1;
                }
                else {
                    jsonStr.append(",");
                }
                String state = "open";
                jsonStr.append("{\"id\":\"" + doc.g("OrgClass") + "\",\"text\":\"" + doc.g("OrgName") + "\",\"OrgClass\":\"" + doc.g("OrgClass") + "\",\"state\":\"" + state
                        + "\",\"dtype\":\"orgclass\",\"children\":");
                jsonStr.append(getDeptJson("root", doc.g("OrgClass"), async));
                jsonStr.append("}");
            }
            jsonStr.append("]");
        }
        else {
            jsonStr = getDeptJson(parentid, orgClass, async);
        }
        //		 BeanCtx.out(jsonStr.toString());
        BeanCtx.p(jsonStr.toString());
        return "";
    }

    /**
     * 获得数据的字符串
     */
    public StringBuilder getDeptJson(String parentid, String orgClass, String async) {
        //params为运行本规则时所传入的参数
        //传false表示一次性全部加载，true表示异步加载
        StringBuilder jsonStr = new StringBuilder();
        String sql = "select * from BPM_OrgDeptList where OrgClass='" + orgClass + "' and ParentFolderid='" + parentid + "' order by SortNumber";
        Document[] dc = Rdb.getAllDocumentsBySql(sql);
        jsonStr.append("[");
        int i = 0;
        for (Document doc : dc) {
            if (i == 0) {
                i = 1;
            }
            else {
                jsonStr.append(",");
            }
            jsonStr.append(getAllSubFolder(doc, async, orgClass));
        }
        //jsonStr.append("]}]");
        jsonStr.append("]");
        // 		BeanCtx.out(jsonStr.toString());
        return jsonStr;
    }

    /**
     * 获得所有子部门的json
     */
    public StringBuilder getAllSubFolder(Document doc, String async, String orgClass) {
        String folderName = doc.g("FolderName");
        StringBuilder jsonStr = new StringBuilder("{\"id\":\"" + doc.g("Folderid") + "\",\"Deptid\":\"" + doc.g("Deptid") + "\",\"text\":\"" + folderName + "\",\"OrgClass\":\"" + orgClass + "\"");
        //看此文件夹是否有子文件夹
        String sql = "select * from BPM_OrgDeptList where OrgClass='" + orgClass + "' and ParentFolderid='" + doc.g("Folderid") + "' order by SortNumber";
        //BeanCtx.out("sql="+sql);
        Document[] dc = Rdb.getAllDocumentsBySql(sql);
        if (dc.length > 0) {
            //说明有子文件夹
            if (async.equals("false") || doc.g("Folderid").equals("001")) {
                //false表示一次性全部加载输出,进行递归调用
                LinkeyUser linkeyUser = (LinkeyUser) BeanCtx.getBean("LinkeyUser");
                String state = "closed";
                if (doc.g("Folderid").equals("001")) {
                    state = "open";
                }
                jsonStr.append(",\"state\":\"" + state + "\",\"children\":[");
                int i = 0;
                for (Document subdoc : dc) {
                    if (i == 0) {
                        i = 1;
                    }
                    else {
                        jsonStr.append(",");
                    }
                    jsonStr.append(getAllSubFolder(subdoc, async, orgClass));
                }
                jsonStr.append("]}");
            }
            else {
                //表示异步加载输出
                jsonStr.append(",\"state\":\"closed\"}");
            }
        }
        else {
            //没有子文件夹了
            jsonStr.append(",\"state\":\"open\"}");
        }

        return jsonStr;
    }

}