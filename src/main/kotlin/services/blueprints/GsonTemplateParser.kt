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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.elisapolystar.blueprints.graphql.model.VariableType
import com.elisapolystar.blueprints.services.api.TemplateParser
import org.springframework.stereotype.Service

@Service
internal class GsonTemplateParser : TemplateParser {

    companion object {
        private val VARIABLE_PATTERN = Regex("""\s*\{\{\s*<(?<variableType>\w+)>\s*(?<variableId>\w+)\s*}}\s*?""")
        private val VARIABLE_TYPES = VariableType.entries.map { it.name }

        private fun parseJsonTemplate(template: String): JsonObject {
            try {
                val jsonElement = JsonParser.parseString(template)
                if (!jsonElement.isJsonObject) {
                    throw IllegalArgumentException("Template is not a JSON object but another JSON element")
                }
                return jsonElement as JsonObject
            } catch (e: JsonSyntaxException) {
                throw IllegalArgumentException("Template is not a valid JSON object: malformed JSON element", e)
            } catch (e: JsonParseException) {
                throw IllegalArgumentException("Template is not a valid JSON object: invalid JSON", e)
            }
        }

        private fun validateVariableTypes(jsonTemplate: JsonObject) {
            for ((_, value) in jsonTemplate.entrySet()) {
                if (value.isStringJsonPrimitive()) {
                    validateStringJsonPrimitive(value.asString)
                } else if (value.isJsonObject) {
                    validateVariableTypes(value.asJsonObject)
                } else if (value.isJsonArray) {
                    for (element in value.asJsonArray) {
                        if (element.isJsonObject) {
                            validateVariableTypes(element.asJsonObject)
                        } else if (element.isStringJsonPrimitive()) {
                            validateStringJsonPrimitive(element.asString)
                        }
                    }
                }
            }
        }

        private fun validateStringJsonPrimitive(stringValue: String) {
            val matchResult = VARIABLE_PATTERN.find(stringValue) ?: return
            val variableType = matchResult.groups["variableType"]!!.value
            if (variableType.uppercase() !in VARIABLE_TYPES) {
                throw IllegalArgumentException(
                    "Unsupported variable type '$variableType' in template; supported types are: $VARIABLE_TYPES"
                )
            }
        }

        private fun JsonElement.isStringJsonPrimitive() = this.isJsonPrimitive && this.asJsonPrimitive.isString
    }

    override fun validateTemplate(template: String) {
        val jsonTemplate = parseJsonTemplate(template)
        validateVariableTypes(jsonTemplate)
    }
}