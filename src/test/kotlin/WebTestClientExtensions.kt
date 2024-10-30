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
package com.elisapolystar.blueprints

import org.hamcrest.core.StringContains
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec

internal fun BodyContentSpec.getDataOnPath(expectedQuery: String, path: String) =
    this.jsonPath("$DATA_JSON_PATH.$expectedQuery.$path")

internal fun ResponseSpec.verifyOnlyDataExists(expectedQuery: String) = this.expectBody()
    .jsonPath("$DATA_JSON_PATH.$expectedQuery").exists()
    .jsonPath(ERRORS_JSON_PATH).doesNotExist()
    .jsonPath(EXTENSIONS_JSON_PATH).doesNotExist()

internal fun ResponseSpec.verifyError(expectedErrorSubString: String) = this.expectStatus().isOk
    .expectBody()
    .jsonPath(DATA_JSON_PATH)
    .doesNotExist()
    .jsonPath("$ERRORS_JSON_PATH.[0].message")
    .value(StringContains.containsString(expectedErrorSubString))
    .jsonPath(EXTENSIONS_JSON_PATH).doesNotExist()
