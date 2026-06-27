package com.example.util

/**
 * 简易 Markdown 解析器 - 无依赖实现
 * 支持：标题、粗体、斜体、删除线、链接、图片、代码块、列表、引用、分割线
 */
object SimpleMarkdownParser {

    fun parse(markdown: String): String {
        if (markdown.isBlank()) return ""
        
        val lines = markdown.lines()
        val result = StringBuilder()
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()
        var inList = false
        var listType: ListType? = null
        var listItems = mutableListOf<String>()

        for (i in lines.indices) {
            var line = lines[i]

            // 代码块处理
            if (line.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeBlockContent.clear()
                } else {
                    inCodeBlock = false
                    result.append("<pre style=\"background:#f5f5f5;padding:12px;border-radius:6px;overflow-x:auto;margin:12px 0;\"><code>")
                    result.append(escapeHtml(codeBlockContent.toString().trimEnd()))
                    result.append("</code></pre>")
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n")
                continue
            }

            // 关闭列表
            if (!line.startsWith("- ") && !line.startsWith("* ") && !line.matches(Regex("^\\d+\\.\\s"))) {
                if (inList) {
                    result.append(if (listType == ListType.UNORDERED) "</ul>" else "</ol>")
                    inList = false
                    listType = null
                    listItems.clear()
                }
            }

            // 空行
            if (line.isBlank()) {
                result.append("<br/>")
                continue
            }

            // 分割线
            if (line.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$"))) {
                result.append("<hr style=\"border:none;border-top:1px solid #ddd;margin:16px 0;\"/>")
                continue
            }

            // 引用
            if (line.startsWith("> ")) {
                result.append("<blockquote style=\"border-left:4px solid #ddd;margin:12px 0;padding:8px 16px;color:#666;background:#fafafa;\">")
                result.append(parseInline(line.removePrefix("> ")))
                result.append("</blockquote>")
                continue
            }

            // 标题
            val headingMatch = Regex("^(#{1,6})\\s+(.*)$").find(line)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2]
                result.append("<h$level style=\"margin:16px 0 8px;font-weight:600;line-height:1.4;\">")
                result.append(parseInline(text))
                result.append("</h$level>")
                continue
            }

            // 无序列表
            if (line.startsWith("- ") || line.startsWith("* ")) {
                if (!inList || listType != ListType.UNORDERED) {
                    if (inList) result.append(if (listType == ListType.ORDERED) "</ol>" else "</ul>")
                    result.append("<ul style=\"margin:8px 0;padding-left:24px;\">")
                    inList = true
                    listType = ListType.UNORDERED
                }
                result.append("<li style=\"margin:4px 0;\">${parseInline(line.removePrefix("- ").removePrefix("* "))}</li>")
                continue
            }

            // 有序列表
            val orderedMatch = Regex("^(\\d+)\\.\\s+(.*)$").find(line)
            if (orderedMatch != null) {
                if (!inList || listType != ListType.ORDERED) {
                    if (inList) result.append(if (listType == ListType.UNORDERED) "</ul>" else "</ol>")
                    result.append("<ol style=\"margin:8px 0;padding-left:24px;\">")
                    inList = true
                    listType = ListType.ORDERED
                }
                result.append("<li style=\"margin:4px 0;\">${parseInline(orderedMatch.groupValues[2])}</li>")
                continue
            }

            // 普通段落
            result.append("<p style=\"margin:8px 0;line-height:1.6;\">")
            result.append(parseInline(line))
            result.append("</p>")
        }

        // 关闭未结束的列表
        if (inList) {
            result.append(if (listType == ListType.UNORDERED) "</ul>" else "</ol>")
        }

        // 关闭未结束的代码块
        if (inCodeBlock) {
            result.append("<pre style=\"background:#f5f5f5;padding:12px;border-radius:6px;overflow-x:auto;margin:12px 0;\"><code>")
            result.append(escapeHtml(codeBlockContent.toString().trimEnd()))
            result.append("</code></pre>")
        }

        return result.toString()
    }

    private fun parseInline(text: String): String {
        var result = text
        
        // 转义 HTML
        result = escapeHtml(result)
        
        // 图片 ![alt](url)
        result = Regex("!\\[([^\\]]*)\\]\\(([^)]+)\\)").replace(result) {
            "<img src=\"${it.groupValues[2]}\" alt=\"${it.groupValues[1]}\" style=\"max-width:100%;height:auto;display:block;margin:12px 0;border-radius:6px;\"/>"
        }
        
        // 链接 [text](url)
        result = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)").replace(result) {
            "<a href=\"${it.groupValues[2]}\" style=\"color:#2196F3;text-decoration:none;\">${it.groupValues[1]}</a>"
        }
        
        // 行内代码 `code`
        result = Regex("`([^`]+)`").replace(result) {
            "<code style=\"background:#f0f0f0;padding:2px 6px;border-radius:3px;font-family:monospace;font-size:0.9em;\">${it.groupValues[1]}</code>"
        }
        
        // 粗体 **text** 或 __text__
        result = Regex("(\\*\\*|__)(.+?)\\1").replace(result) {
            "<strong>${it.groupValues[2]}</strong>"
        }
        
        // 斜体 *text* 或 _text_
        result = Regex("(\\*|_)(.+?)\\1").replace(result) {
            "<em>${it.groupValues[2]}</em>"
        }
        
        // 删除线 ~~text~~
        result = Regex("~~(.+?)~~").replace(result) {
            "<del>${it.groupValues[1]}</del>"
        }
        
        // 下划线 <u>text</u> (保留 HTML 标签)
        // 注意：由于已经转义了 HTML，这里需要特殊处理
        result = result.replace("&lt;u&gt;", "<u>").replace("&lt;/u&gt;", "</u>")
        
        return result
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private enum class ListType { UNORDERED, ORDERED }
}
