package com.zode.api.markdown.util;


import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class MarkdownToHtmlConverter {

    /**
     * 将 Markdown 字符串转换为 HTML 字符串
     * @param markdown 输入的 Markdown 内容
     * @return 转换后的 HTML 内容
     */
    public static String convertMarkdownToHtml(String markdown) {
        // 配置解析和渲染选项
        MutableDataSet options = new MutableDataSet();

        // 启用扩展 (表格、任务列表等)[2](@ref)[3](@ref)
        options.set(Parser.EXTENSIONS, Arrays.asList(
                com.vladsch.flexmark.ext.tables.TablesExtension.create(),
                com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension.create()
                // 可以添加更多扩展，如：AutolinkExtension、FootnotesExtension 等
        ));

        // 配置表格选项（可选）[2](@ref)
        options.set(com.vladsch.flexmark.ext.tables.TablesExtension.COLUMN_SPANS, false);
        options.set(com.vladsch.flexmark.ext.tables.TablesExtension.APPEND_MISSING_COLUMNS, true);
        options.set(com.vladsch.flexmark.ext.tables.TablesExtension.DISCARD_EXTRA_COLUMNS, true);
        options.set(com.vladsch.flexmark.ext.tables.TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

        // 配置其他选项，例如将软换行转换为硬换行[2](@ref)
        // options.set(HtmlRenderer.SOFT_BREAK, "<br />");

        // 构建解析器和渲染器
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // 解析和渲染
        Node document = parser.parse(markdown);
        String html = renderer.render(document);

        // 可以在这里包装完整的 HTML 结构并添加 CSS 样式（见下一步）
        return wrapWithHtmlTemplate(html);
    }

    /**
     * 内联样式CSS
     * @param html
     * @return
     */
    public static String inlineCss(String html) {
        Document doc = Jsoup.parse(html);
        Elements styleElements = doc.select("style"); // 获取所有<style>标签

        for (Element styleElement : styleElements) {
            String styleRules = styleElement.html().replace("\n", "").trim(); // 获取样式规则并清理
            StringTokenizer tokenizer = new StringTokenizer(styleRules, "{}", true); // 使用{}作为分隔符

            StringBuilder currentSelector = new StringBuilder();
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (token.equals("{")) {
                    // 此时currentSelector中存储的是选择器
                    String properties = tokenizer.nextToken(); // 获取属性部分
                    if (!tokenizer.hasMoreTokens()) break;
                    tokenizer.nextToken(); // 跳过接下来的"}"

                    if (!currentSelector.toString().contains(":")) { // 简单跳过伪类
                        Elements selectedElements = doc.select(currentSelector.toString());
                        for (Element elem : selectedElements) {
                            String existingStyle = elem.attr("style");
                            String newStyle = existingStyle + (existingStyle.isEmpty() ? "" : "; ") + properties;
                            elem.attr("style", newStyle.trim());
                        }
                    }
                    currentSelector.setLength(0); // 清空选择器以备下次使用
                } else if (!token.equals("}")) {
                    currentSelector.append(token);
                }
            }
            styleElement.remove(); // 可选：移除已内联的<style>标签
        }
        return doc.body().toString();
    }

    /**
     * 将渲染后的 HTML 内容包装到完整的 HTML 文档中，并添加 CSS 样式
     * @param content 渲染后的 HTML 内容
     * @return 完整的 HTML 文档
     */
    private static String wrapWithHtmlTemplate(String content) {
        // 这里是一个简单的 GitHub Flavored Markdown 风格的 CSS，你可以替换或扩展它[2](@ref)
        String style = "<style>\n" +
                "body {width: 375px;padding: 20px;position: relative;font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;font-size: 14px;line-height: 1.75;text-align: left;pointer-events: initial !important;}\n" +
                "h1{font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;padding: 0.5em 1em;border-bottom: 2px solid #FA5151;font-size: 19.6px;text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);margin: 0.67em auto;font-weight: bold;text-align: center;color: hsl(0 0% 6% / 1);}\n" +
                "h2{font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;padding: 0.3em 1em;border-radius: 8px;font-size: 18.2px;box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);margin: 2.5em auto 2em;color: #fff;background: #FA5151;font-weight: bold;text-align: center;}\n" +
                "h3{font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;margin: 2em 8px 0.75em 0;color: #3f3f3f;font-weight: bold;line-height: 1.2;padding-left: 12px;font-size: 16.8px;border-left: 4px solid #FA5151;border-bottom: 1px dashed #FA5151;}\n" +
                "h4{font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;margin: 2em 8px 0.5em;color: #FA5151;font-size: 15.4px;font-weight: bold;}\n" +
                "h5{font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;margin: 1.5em 8px 0.5em;color: #FA5151;font-size: 14px;font-weight: bold;}\n" +
                "h6{font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;margin: 1.5em 8px 0.5em;font-size: 14px;color: #FA5151;}\n" +
                "a {font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;color: #0366d6; text-decoration: none;}\n" +
                "a:hover {text-decoration: underline;}\n" +
                "code {background-color: #f6f8fa; padding: 0.2em 0.4em; border-radius: 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;}\n" +
                "pre {background-color: #f6f8fa; padding: 1em; overflow: auto; border-radius: 3px;}\n" +
                "pre code {padding: 0;}\n" +
                "strong {color: #FA5151;font-weight: bold;font-size: inherit;}\n" +
                "blockquote {font-size: 14px;line-height: 1.75;text-align: left;background: #f7f7f7;border-color: hsl(0 0% 89.8%);font-style: italic;padding: 1em 1em 1em 2em;border-left: 4px solid #FA5151;border-radius: 6px;color: rgba(0, 0, 0, 0.6);box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);margin:  0 0 1em 0;}\n" +
                "table {font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;font-size: 14px;line-height: 1.75;text-align: left;margin-top: 0 !important;border-collapse: separate;border-radius: 8px;border-spacing: 0;margin: 1em 0;color: hsl(var(0 0% 3.9%));box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);}\n" +
                "thead {font-weight: bold;}\n" +
                "th {border: 1px solid #dfdfdf;color: hsl(0 0% 3.9%);word-break: keep-all;background: rgba(0, 0, 0, 0.05);padding: 0.25em 0.5em;}\n" +
                "td {padding: 0.5em 1em;border: 1px solid #dfdfdf;padding: 0.25em 0.5em;color: hsl(0 0% 3.9%);word-break: normal;}\n" +
                "hr {height: 1px;border: none;margin: 2em 0;background: linear-gradient(to right, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0));}\n" +
                "ul, ol {list-style-type: disc;padding-left: 1.5em;margin: 10px 0;}\n" +
                "li {font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;font-size: 14px;line-height: 1.75;text-align: left;margin: 0.5em 8px;color: hsl(0 0% 6% / 1);}\n" +
                "p {font-family: Optima-Regular, Optima, PingFangSC-light, PingFangTC-light, 'PingFang SC', Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;font-size: 14px;line-height: 1.75;text-align: left;letter-spacing: 0.1em;color: hsl(0 0% 3.9%);border-color: hsl(0 0% 89.8%);}\n" +
                "</style>";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Markdown 转换结果</title>\n" +
                style +
                "</head>\n" +
                "<body class=\"markdown-body\">\n" + // 添加一个类名以便更精确地应用样式
                content +
                "</body>\n" +
                "</html>";
    }

    public static String parseMarkdown(String content) {
        // 1. (可选但重要) 如果内容来自不可信源，先进行 HTML 转义以防止 XSS[9](@ref)[10](@ref)
        // 使用 Spring 的 HtmlUtils (推荐) 或 Apache Commons Text 的 StringEscapeUtils
        // String safeMarkdown = HtmlUtils.htmlEscape(compressedMarkdown); // 转义 HTML 特殊字符
        // 注意：转义后，Markdown 中的特殊符号也会被转义，所以通常是在最终渲染 HTML 输出前对动态部分进行转义，而不是在 Markdown 转换前。
        // 更佳实践：在将最终生成的 HTML 输出到前端页面时，由模板引擎（如 Thymeleaf, FreeMarker）自动转义，或使用 HtmlUtils.htmlEscape() 处理其中嵌入的不信任变量。

        System.out.println(content);
        content = content.replaceAll("\\n", "\n");
        content = content.replaceAll("\\\\n", "\n");
        content = content.replaceAll(Pattern.quote("****"), "**");
        // 2. 转换 Markdown 到 HTML
        String styledHtml = MarkdownToHtmlConverter.convertMarkdownToHtml(content);
        String inlinedHtml = inlineCss(styledHtml);
        // 3. 输出或使用生成的 HTML
        System.out.println(inlinedHtml); // 输出到控制台
        // 或者写入文件、发送到 HTTP 响应等
        return inlinedHtml;
    }
}
