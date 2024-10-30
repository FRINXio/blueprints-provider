/*
 * MIT License
 *
 * Copyright (c) 2024 Elisa Polystar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.elisapolystar.blueprints.services.blueprints

import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GsonTemplateParserTest {

    private val parser = GsonTemplateParser()

    @Test
    fun `validation should pass for valid template`() {
        val validTemplate = """
            {
                "address": "{{<STRING> address}}",
                "port": "{{<NUMBER> port}}"
            }
        """.trimIndent()

        assertDoesNotThrow {
            parser.validateTemplate(validTemplate)
        }
    }

    @Test
    fun `validation should throw IllegalArgumentException for invalid JSON`() {
        val invalidJsonTemplate = """
            {
                "address": "{{<STRING> address}}"
                "port": "{{<NUMBER> port}}"
            }
        """.trimIndent()

        val exception = assertThrows<IllegalArgumentException> {
            parser.validateTemplate(invalidJsonTemplate)
        }
        assertTrue(exception.cause is JsonSyntaxException)
    }

    @Test
    fun `validation should throw IllegalArgumentException for unsupported variable type`() {
        val unsupportedVariableTypeTemplate = """
            {
                "address": "{{<UNSUPPORTED> address}}"
            }
        """.trimIndent()

        val exception = assertThrows<IllegalArgumentException> {
            parser.validateTemplate(unsupportedVariableTypeTemplate)
        }
        assertTrue(exception.message?.contains("Unsupported variable type") ?: false)
    }

    @Test
    fun `validation should pass for nested valid template`() {
        val nestedValidTemplate = """
            {
                "streams": {
                    "name": "{{<STRING> name}}",
                    "details": {
                        "direct": "{{<BOOLEAN> direct_stream}}"
                    }
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            parser.validateTemplate(nestedValidTemplate)
        }
    }

    @Test
    fun `validation should throw IllegalArgumentException for invalid nested template`() {
        val invalidNestedTemplate = """
            {
                "streams": {
                    "name": "{{<STRING> name}}",
                    "details": {
                        "direct": "{{<UNSUPPORTED> direct}}"
                    }
                }
            }
        """.trimIndent()

        val exception = assertThrows<IllegalArgumentException> {
            parser.validateTemplate(invalidNestedTemplate)
        }
        assertTrue(exception.message?.contains("Unsupported variable type") ?: false)
    }
}