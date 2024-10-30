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
package com.elisapolystar.blueprints.graphql.mutations

import com.elisapolystar.blueprints.Application
import com.elisapolystar.blueprints.GRAPHQL_ENDPOINT
import com.elisapolystar.blueprints.GRAPHQL_MEDIA_TYPE
import com.elisapolystar.blueprints.PostgresContainerSetup
import com.elisapolystar.blueprints.commons.ConnectionType.CLI
import com.elisapolystar.blueprints.commons.ConnectionType.GNMI
import com.elisapolystar.blueprints.commons.createBlueprintId
import com.elisapolystar.blueprints.database.jpa.Blueprint
import com.elisapolystar.blueprints.database.jpa.Status
import com.elisapolystar.blueprints.database.repository.BlueprintRepository
import com.elisapolystar.blueprints.getDataOnPath
import com.elisapolystar.blueprints.verifyError
import com.elisapolystar.blueprints.verifyOnlyDataExists
import kotlin.jvm.optionals.getOrNull
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [Application::class])
@AutoConfigureWebTestClient
@ActiveProfiles("test")
internal class BlueprintMutationsTest(
    @Autowired private val testClient: WebTestClient,
    @Autowired private val blueprintRepository: BlueprintRepository
) : PostgresContainerSetup() {

    @BeforeEach
    fun setUp() {
        blueprintRepository.deleteAll()
    }

    @Test
    fun `create blueprint`() {
        val blueprintName = "test"
        val mutation = """
          mutation test {
            createBlueprint(blueprint: {
              name: "$blueprintName",
              connectionType: GNMI,
              template: "{\"address\": \"192.168.10.1\"}",
              vendorPattern: "OpenWRT",
              modelPattern: "VSR"
              versionPattern: "21.*?"
            }) {
              id,
              blueprintData {
                name
              }
            }
          }
        """.trimIndent()

        val operationId = "createBlueprint"
        testClient.post()
            .uri(GRAPHQL_ENDPOINT)
            .accept(APPLICATION_JSON)
            .contentType(GRAPHQL_MEDIA_TYPE)
            .bodyValue(mutation)
            .exchange()
            .verifyOnlyDataExists(operationId)
            .getDataOnPath(operationId, "id").value(startsWith("blueprint/"))
            .getDataOnPath(operationId, "blueprintData.name").isEqualTo(blueprintName)
        assertTrue(blueprintRepository.existsBlueprintByName(blueprintName))
    }

    @Test
    fun `update blueprint`() {
        val blueprint = Blueprint(
            id = 0,
            name = "test",
            connectionType = GNMI,
            template = "{\"address\": \"192.168.10.1\"}",
            vendorPattern = null,
            modelPattern = null,
            versionPattern = null
        )
        blueprintRepository.save(blueprint)

        val blueprintId = blueprint.id.createBlueprintId()
        val mutation = """
            mutation test {
              updateBlueprint(id: "$blueprintId", blueprint: {
                name: "test",
                connectionType: CLI,
                template: "{\"address\": \"1.1.1.1\"}"
              }) {
                id
              }
            }
        """.trimIndent()

        val operationId = "updateBlueprint"
        testClient.post()
            .uri(GRAPHQL_ENDPOINT)
            .accept(APPLICATION_JSON)
            .contentType(GRAPHQL_MEDIA_TYPE)
            .bodyValue(mutation)
            .exchange()
            .verifyOnlyDataExists(operationId)
            .getDataOnPath(operationId, "id").isEqualTo(blueprintId)

        val updatedBlueprint = blueprintRepository.findById(blueprint.id).get()
        assertEquals(blueprint.name, updatedBlueprint.name)
        assertTrue(updatedBlueprint.connectionType == CLI)
        assertTrue(updatedBlueprint.template == "{\"address\": \"1.1.1.1\"}")
    }

    @Test
    fun `delete blueprint`() {
        val blueprint = Blueprint(
            id = 0,
            name = "test",
            connectionType = GNMI,
            template = "{\"address\": \"192.168.10.1\"}",
            vendorPattern = "OpenWRT",
            modelPattern = null,
            versionPattern = null
        )
        blueprintRepository.save(blueprint)

        val blueprintId = blueprint.id.createBlueprintId()
        val mutation = """
          mutation test {
            deleteBlueprint(id: "$blueprintId") {
              id
              blueprintData {
                name
                connectionType
                modelPattern
                vendorPattern
                versionPattern
                template
              }
            }
          }
        """.trimIndent()

        val operationId = "deleteBlueprint"
        testClient.post()
            .uri(GRAPHQL_ENDPOINT)
            .accept(APPLICATION_JSON)
            .contentType(GRAPHQL_MEDIA_TYPE)
            .bodyValue(mutation)
            .exchange()
            .verifyOnlyDataExists(operationId)
            .getDataOnPath(operationId, "id").isEqualTo(blueprintId)
            .getDataOnPath(operationId, "blueprintData.name").value(startsWith("${blueprint.name}__"))
            .getDataOnPath(operationId, "blueprintData.connectionType").isEqualTo(GNMI.name)
            .getDataOnPath(operationId, "blueprintData.modelPattern").doesNotExist()
            .getDataOnPath(operationId, "blueprintData.vendorPattern").isEqualTo(blueprint.vendorPattern!!)
            .getDataOnPath(operationId, "blueprintData.versionPattern").doesNotExist()
            .getDataOnPath(operationId, "blueprintData.template").isEqualTo(blueprint.template)

        val removedBlueprint = blueprintRepository.findById(blueprint.id).getOrNull()
        assertNotNull(removedBlueprint)
        assertEquals(Status.DELETED, removedBlueprint!!.status)
    }

    @Test
    fun `delete invalid blueprint`() {
        val blueprintId = 1.createBlueprintId()
        val mutation = """
          mutation test {
            deleteBlueprint(id: "$blueprintId") {
              id
            }
          }
        """.trimIndent()

        testClient.post()
            .uri(GRAPHQL_ENDPOINT)
            .accept(APPLICATION_JSON)
            .contentType(GRAPHQL_MEDIA_TYPE)
            .bodyValue(mutation)
            .exchange()
            .verifyError("Blueprint with ID '$blueprintId' does not exist")
    }
}