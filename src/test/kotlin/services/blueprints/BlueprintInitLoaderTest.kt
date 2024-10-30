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

import com.elisapolystar.blueprints.Application
import com.elisapolystar.blueprints.PostgresContainerSetup
import com.elisapolystar.blueprints.database.repository.BlueprintRepository
import com.google.gson.Gson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [Application::class])
@TestPropertySource(
    properties = [
        "init.blueprintsDirectory=src/test/resources/blueprints"
    ]
)
internal class BlueprintInitLoaderTest(
    @Autowired private val blueprintRepository: BlueprintRepository,
    @Autowired private val blueprintInitLoader: BlueprintInitLoader
) : PostgresContainerSetup() {

    @AfterEach
    fun setUp() {
        blueprintRepository.deleteAll()
    }

    @Test
    fun `verify loaded default blueprints`() {
        var blueprints = blueprintRepository.findAll()
        assertEquals(2, blueprints.size)

        assertDoesNotThrow {
            blueprintInitLoader.init()
        }
        blueprints = blueprintRepository.findAll()
        assertEquals(2, blueprints.size)

        val bluprintNames = blueprints
            .map { it.name }
            .toSet()
        assertEquals(
            setOf("cli", "gnmi"),
            bluprintNames
        )

        val gson = Gson()
        val blueprintTemplates = blueprints
            .map { it.template }
        blueprintTemplates.forEach {
            assertTrue {
                try {
                    gson.fromJson(it, Any::class.java)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}