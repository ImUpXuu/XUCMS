package com.upxuu.xucms.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.upxuu.xucms.editor.model.BlockType
import com.upxuu.xucms.editor.model.InlineMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorStateTest {

  private fun EditorState.typeInto(index: Int, text: String) {
    val block = blocks[index]
    focus(block.id)
    onTextChange(block.id, TextFieldValue(text, TextRange(text.length)))
  }

  @Test
  fun `typing a hash converts the block to a heading and drops the marker`() {
    val state = EditorState("")
    state.typeInto(0, "# ")
    assertEquals(BlockType.H1, state.blocks[0].type)
    assertEquals("", state.blocks[0].text)
  }

  @Test
  fun `typing a dash converts the block to a bullet`() {
    val state = EditorState("")
    state.typeInto(0, "- ")
    assertEquals(BlockType.BULLET, state.blocks[0].type)
    assertEquals("", state.blocks[0].text)
  }

  @Test
  fun `enter splits a paragraph at the caret`() {
    val state = EditorState("hello world")
    val block = state.blocks[0]
    state.focus(block.id)
    state.onTextChange(block.id, block.value.copy(selection = TextRange(5)))
    state.onEnter(block.id)
    assertEquals(2, state.blocks.size)
    assertEquals("hello", state.blocks[0].text)
    assertEquals(" world", state.blocks[1].text)
  }

  @Test
  fun `enter continues a list`() {
    val state = EditorState("- item")
    val block = state.blocks[0]
    state.focus(block.id)
    state.onEnter(block.id)
    assertEquals(BlockType.BULLET, state.blocks[1].type)
  }

  @Test
  fun `enter on an empty list item exits the list`() {
    val state = EditorState("- ")
    val block = state.blocks[0]
    state.focus(block.id)
    state.onEnter(block.id)
    assertEquals(1, state.blocks.size)
    assertEquals(BlockType.PARAGRAPH, state.blocks[0].type)
  }

  @Test
  fun `backspace at the start of a heading demotes it to a paragraph`() {
    val state = EditorState("## Section")
    val block = state.blocks[0]
    state.focus(block.id)
    assertTrue(state.onBackspaceAtStart(block.id))
    assertEquals(BlockType.PARAGRAPH, state.blocks[0].type)
    assertEquals("Section", state.blocks[0].text)
  }

  @Test
  fun `backspace merges a paragraph into the previous one`() {
    val state = EditorState("first\n\nsecond")
    assertEquals(2, state.blocks.size)
    val second = state.blocks[1]
    state.focus(second.id)
    assertTrue(state.onBackspaceAtStart(second.id))
    assertEquals(1, state.blocks.size)
    assertEquals("firstsecond", state.blocks[0].text)
  }

  @Test
  fun `toggling bold over a selection marks that range`() {
    val state = EditorState("hello world")
    val block = state.blocks[0]
    state.focus(block.id)
    state.onTextChange(block.id, block.value.copy(selection = TextRange(0, 5)))
    state.toggleInline(InlineMark.BOLD)
    assertEquals("**hello** world", state.toMarkdown())
  }

  @Test
  fun `inserting a divider keeps a trailing paragraph to type in`() {
    val state = EditorState("text")
    state.focus(state.blocks[0].id)
    state.insertDivider()
    assertEquals(BlockType.DIVIDER, state.blocks[1].type)
    assertEquals(BlockType.PARAGRAPH, state.blocks[2].type)
  }

  @Test
  fun `inserting an image produces an image block`() {
    val state = EditorState("")
    state.focus(state.blocks[0].id)
    state.insertImage("https://cdn.example/a.png")
    assertTrue(state.toMarkdown().contains("![](https://cdn.example/a.png)"))
  }

  @Test
  fun `undo restores the previous document`() {
    val state = EditorState("start")
    state.typeInto(0, "start more")
    assertTrue(state.canUndo)
    state.undo()
    assertEquals("start", state.blocks[0].text)
  }

  @Test
  fun `setBlockType toggles back to paragraph when reapplied`() {
    val state = EditorState("line")
    state.focus(state.blocks[0].id)
    state.setBlockType(BlockType.QUOTE)
    assertEquals(BlockType.QUOTE, state.blocks[0].type)
    state.setBlockType(BlockType.QUOTE)
    assertEquals(BlockType.PARAGRAPH, state.blocks[0].type)
  }

  @Test
  fun `markdown survives a load and save cycle`() {
    val source = "# T\n\nbody with **bold**\n\n- a\n- b"
    val state = EditorState(source)
    assertEquals(source, state.toMarkdown())
  }

  @Test
  fun `blank document is reported as blank`() {
    assertTrue(EditorState("").isBlank)
    assertFalse(EditorState("x").isBlank)
  }
}
