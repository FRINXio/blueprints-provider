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
import com.elisapolystar.blueprints.commons.ConnectionType.CLI
import com.elisapolystar.blueprints.commons.createBlueprintId
import com.elisapolystar.blueprints.commons.parseBlueprintDatabaseId
import com.elisapolystar.blueprints.commons.toGraphQLData
import com.elisapolystar.blueprints.database.jpa.Status
import com.elisapolystar.blueprints.database.repository.BlueprintRepository
import com.elisapolystar.blueprints.graphql.model.BlueprintData
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
internal class BlueprintDatabaseLoaderTest(
    @Autowired private val blueprintRepository: BlueprintRepository,
    @Autowired private val blueprintDatabaseLoader: BlueprintDatabaseLoader
) : PostgresContainerSetup() {

    @BeforeEach
    fun setUp() {
        blueprintRepository.deleteAll()
    }

    @Test
    fun `create new blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI,
            versionPattern = "1.0",
            vendorPattern = "Vendor"
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        val savedBlueprint = blueprintRepository.findById(id.parseBlueprintDatabaseId()).orElse(null)

        assertNotNull(savedBlueprint)
        assertEquals(blueprintData, savedBlueprint.toGraphQLData())
        assertEquals(Status.ACTIVE, savedBlueprint.status)
        assertTrue(savedBlueprint.id > 0)
    }

    @Test
    fun `create duplicate blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        blueprintDatabaseLoader.createBlueprint(blueprintData)
        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.createBlueprint(blueprintData)
        }
    }

    @Test
    fun `create blueprint with invalid template`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{",
            connectionType = CLI
        )

        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.createBlueprint(blueprintData)
        }
    }

    @Test
    fun `delete existing blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        val beforeRemovalTime = LocalDateTime.now(ZoneOffset.UTC)
        blueprintDatabaseLoader.deleteBlueprint(id)

        val deletedBlueprint = blueprintRepository.findById(id.parseBlueprintDatabaseId())
        assertTrue(deletedBlueprint.isPresent)
        assertEquals(Status.DELETED, deletedBlueprint.get().status)
        assertTrue(deletedBlueprint.get().name.startsWith("${blueprintData.name}__"))
        assertTrue(deletedBlueprint.get().updatedAt.isAfter(beforeRemovalTime))
    }

    @Test
    fun `delete invalid blueprint`() {
        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.deleteBlueprint(1.createBlueprintId())
        }
    }

    @Test
    fun `delete already deleted blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        blueprintDatabaseLoader.deleteBlueprint(id)

        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.deleteBlueprint(id)
        }
    }

    @Test
    fun `update existing blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        val beforeUpdateTime = LocalDateTime.now(ZoneOffset.UTC)
        val updatedBlueprintData = blueprintData.copy(
            template = "{\"updated\": true}"
        )

        blueprintDatabaseLoader.updateBlueprint(id, updatedBlueprintData)
        val updatedBlueprint = blueprintRepository.findById(id.parseBlueprintDatabaseId()).orElse(null)

        assertNotNull(updatedBlueprint)
        assertEquals(updatedBlueprintData, updatedBlueprint.toGraphQLData())
        assertTrue(updatedBlueprint.updatedAt.isAfter(beforeUpdateTime))
    }

    @Test
    fun `update deleted blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        blueprintDatabaseLoader.deleteBlueprint(id)

        val updatedBlueprintData = blueprintData.copy(
            template = "{\"updated\": true}"
        )

        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.updateBlueprint(id, updatedBlueprintData)
        }
    }

    @Test
    fun `update blueprint name`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        val updatedBlueprintData = blueprintData.copy(
            name = "Updated Blueprint"
        )

        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.updateBlueprint(id, updatedBlueprintData)
        }
    }

    @Test
    fun `update invalid blueprint`() {
        val blueprintData = BlueprintData(
            name = "Test Blueprint",
            template = "{}",
            connectionType = CLI
        )

        val id = blueprintDatabaseLoader.createBlueprint(blueprintData)
        blueprintDatabaseLoader.deleteBlueprint(id)

        assertThrows(IllegalArgumentException::class.java) {
            blueprintDatabaseLoader.updateBlueprint(id, blueprintData)
        }
    }
}