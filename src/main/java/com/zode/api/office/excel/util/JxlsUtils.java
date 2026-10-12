package com.zode.api.office.excel.util;

import org.jxls.builder.xls.AreaCommand;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.command.*;
import org.jxls.common.Context;
import org.jxls.expression.JexlExpressionEvaluator;
import org.jxls.transform.Transformer;
import org.jxls.util.JxlsHelper;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class JxlsUtils {

    static {
        //添加自定义指令（可覆盖jxls原指令）
        XlsCommentAreaBuilder.addCommandMapping(AutoMergeCommand.COMMAND_NAME, AutoMergeCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(FontCommand.COMMAND_NAME, FontCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(MergeCommand.COMMAND_NAME, MergeCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(ImageCommand.COMMAND_NAME, ImageCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(EachCommand.COMMAND_NAME, EachCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(IfCommand.COMMAND_NAME, IfCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(AreaCommand.COMMAND_NAME, AreaCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(GridCommand.COMMAND_NAME, GridCommand.class);
        XlsCommentAreaBuilder.addCommandMapping(UpdateCellCommand.COMMAND_NAME, UpdateCellCommand.class);
    }

    public static void downExport(HttpServletResponse response, InputStream is, String fileName, Map<String, Object> map) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try {
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
            response.setCharacterEncoding("UTF-8");
            OutputStream os = response.getOutputStream();
            exportExcel(is, os, map);
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportExcel(InputStream is, OutputStream os, Map<String, Object> model) throws IOException {
        Context context = new Context();
        if (model != null) {
            for (String key : model.keySet()) {
                context.putVar(key, model.get(key));
            }
        }
        JxlsHelper jxlsHelper = JxlsHelper.getInstance();
        Transformer transformer = jxlsHelper.createTransformer(is, os);
        JexlExpressionEvaluator evaluator = (JexlExpressionEvaluator) transformer.getTransformationConfig().getExpressionEvaluator();
        Map<String, Object> funcs = new HashMap<>();
        funcs.put("utils", new JxlsUtils());
        // 添加自定义函数功能
        // evaluator.getJexlEngine().setFunctions(funcs);
        jxlsHelper.processTemplate(context, transformer);
    }

    public static void exportExcel(File xls, File out, Map<String, Object> model) throws FileNotFoundException, IOException {
        exportExcel(new FileInputStream(xls), new FileOutputStream(out), model);
    }

    public static void exportExcel(String templatePath, OutputStream os, Map<String, Object> model) throws Exception {
        File template = getTemplate(templatePath);
        if(template != null){
            exportExcel(new FileInputStream(template), os, model);
        } else {
            throw new Exception("Excel 模板未找到。");
        }
    }

    //获取jxls模版文件
    public static File getTemplate(String path){
        File template = new File(path);
        if(template.exists()){
            return template;
        }
        return null;
    }

    public String dateFmt(Date date, String fmt) {
        if (date == null) {
            return null;
        }
        try {
            SimpleDateFormat dateFmt = new SimpleDateFormat(fmt);
            return dateFmt.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object ifElse(boolean b, Object o1, Object o2) {
        return b ? o1 : o2;
    }
}
