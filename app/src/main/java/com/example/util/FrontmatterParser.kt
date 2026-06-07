package com.example.util

data class FrontmatterResult(
    val title: String,
    val published: String, // or date for talk
    val category: String,
    val tags: List<String>,
    val description: String,
    val image: String,
    val draft: Boolean,
    val sticky: Int,
    val body: String
)

object FrontmatterParser {
    fun parseFrontmatter(content: String): FrontmatterResult {
        var title = ""
        var published = ""
        var category = ""
        var tags = emptyList<String>()
        var description = ""
        var image = ""
        var draft = false
        var sticky = 0
        var body = ""

        val lines = content.lines()
        var bodyStartIndex = 0

        if (lines.isNotEmpty() && lines[0].startsWith("---")) {
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.startsWith("---")) {
                    bodyStartIndex = i + 1
                    break
                }
                val colonPos = line.indexOf(':')
                if (colonPos != -1) {
                    val key = line.substring(0, colonPos).trim().lowercase()
                    val value = line.substring(colonPos + 1).trim()
                    val cleanValue = value.removeSurrounding("\"").removeSurrounding("'")
                    when (key) {
                        "title" -> title = cleanValue
                        "published", "date" -> published = cleanValue
                        "category" -> category = cleanValue
                        "tags" -> {
                            val tagsStr = value.removeSurrounding("[").removeSurrounding("]")
                            tags = tagsStr.split(",").map { it.trim().removeSurrounding("\"").removeSurrounding("'") }.filter { it.isNotEmpty() }
                        }
                        "description" -> description = cleanValue
                        "image" -> image = cleanValue
                        "draft" -> draft = cleanValue.toBooleanStrictOrNull() ?: false
                        "sticky" -> sticky = cleanValue.toIntOrNull() ?: 0
                    }
                }
            }
        }

        body = if (bodyStartIndex > 0) {
            lines.subList(bodyStartIndex, lines.size).joinToString("\n").trim()
        } else {
            content.trim()
        }

        return FrontmatterResult(title, published, category, tags, description, image, draft, sticky, body)
    }

    fun buildPostFrontmatter(fm: FrontmatterResult): String {
        val build = StringBuilder()
        build.append("---\n")
        build.append("title: \"${fm.title}\"\n")
        build.append("published: \"${fm.published}\"\n")
        if (fm.category.isNotEmpty()) build.append("category: \"${fm.category}\"\n")
        if (fm.tags.isNotEmpty()) {
            val tagsStr = fm.tags.joinToString(", ") { "\"$it\"" }
            build.append("tags: [$tagsStr]\n")
        }
        if (fm.description.isNotEmpty()) build.append("description: \"${fm.description}\"\n")
        if (fm.image.isNotEmpty()) build.append("image: \"${fm.image}\"\n")
        if (fm.draft) build.append("draft: true\n")
        if (fm.sticky > 0) build.append("sticky: ${fm.sticky}\n")
        build.append("---\n\n")
        build.append(fm.body)
        return build.toString()
    }

    fun buildTalkFrontmatter(title: String, date: String, tags: List<String>, body: String): String {
        val build = StringBuilder()
        build.append("---\n")
        if (title.isNotEmpty()) build.append("title: \"$title\"\n")
        build.append("date: \"$date\"\n")
        if (tags.isNotEmpty()) {
            val tagsStr = tags.joinToString(", ") { "\"$it\"" }
            build.append("tags: [$tagsStr]\n")
        }
        build.append("---\n\n")
        build.append(body)
        return build.toString()
    }

    fun generateFilename(title: String, dateOrPublished: String): String {
        val datePrefix = if (dateOrPublished.length >= 10) dateOrPublished.substring(0, 10) else "1970-01-01"
        val safeTitle = title.replace(Regex("[/\\\\:*?\"<>|\\s]+"), "-")
            .replace(Regex("-+"), "-")
            .take(30)
        return "$datePrefix-$safeTitle.md"
    }
}
