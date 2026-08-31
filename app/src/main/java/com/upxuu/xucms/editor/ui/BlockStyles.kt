package com.upxuu.xucms.editor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.upxuu.xucms.editor.model.BlockType
import com.upxuu.xucms.editor.model.InlineMark
import com.upxuu.xucms.editor.model.MarkSpan

/** Text style each block type is edited *and* displayed with — one source of truth. */
@Composable
fun blockTextStyle(type: BlockType): TextStyle {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography
  return when (type) {
    BlockType.H1 -> typography.headlineMedium.copy(color = colors.primary, fontWeight = FontWeight.ExtraBold)
    BlockType.H2 -> typography.headlineSmall.copy(color = colors.primary, fontWeight = FontWeight.Bold)
    BlockType.H3 -> typography.titleLarge.copy(color = colors.onSurface, fontWeight = FontWeight.Bold)
    BlockType.QUOTE -> typography.bodyLarge.copy(color = colors.onSurfaceVariant)
    BlockType.CODE -> typography.bodyMedium.copy(
      fontFamily = FontFamily.Monospace,
      color = colors.onSurface,
      fontSize = 13.sp,
      lineHeight = 20.sp,
    )
    else -> typography.bodyLarge.copy(color = colors.onSurface)
  }
}

/**
 * Paints inline marks directly on the editable text. Offsets are untouched, so the
 * caret and selection stay exactly where the user put them — this is the core of
 * the what-you-see-is-what-you-get behaviour.
 */
@Composable
fun rememberMarkTransformation(marks: List<MarkSpan>): VisualTransformation {
  val colors = MaterialTheme.colorScheme
  return remember(marks, colors) {
    VisualTransformation { original ->
      val builder = AnnotatedString.Builder(original.text)
      marks.forEach { span ->
        val start = span.start.coerceIn(0, original.text.length)
        val end = span.end.coerceIn(0, original.text.length)
        if (end <= start) return@forEach
        builder.addStyle(spanStyleFor(span.mark, colors.primary, colors.tertiaryContainer, colors.onSurface), start, end)
      }
      TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
  }
}

private fun spanStyleFor(
  mark: InlineMark,
  accent: Color,
  codeBackground: Color,
  onSurface: Color,
): SpanStyle = when (mark) {
  InlineMark.BOLD -> SpanStyle(fontWeight = FontWeight.Bold, color = onSurface)
  InlineMark.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
  InlineMark.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
  InlineMark.CODE -> SpanStyle(
    fontFamily = FontFamily.Monospace,
    background = codeBackground,
    fontSize = 14.sp,
  )
  InlineMark.LINK -> SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
}
