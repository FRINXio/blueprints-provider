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

import com.elisapolystar.blueprints.commons.createBlueprintId
import com.elisapolystar.blueprints.commons.parseBlueprintDatabaseId
import com.elisapolystar.blueprints.commons.toGraphQLData
import com.elisapolystar.blueprints.commons.toJpaEntity
import com.elisapolystar.blueprints.database.jpa.Status
import com.elisapolystar.blueprints.database.repository.BlueprintRepository
import com.elisapolystar.blueprints.graphql.model.BlueprintData
import com.elisapolystar.blueprints.services.api.BlueprintLoader
import com.elisapolystar.blueprints.services.api.TemplateParser
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
internal class BlueprintDatabaseLoader(
    private val blueprintRepository: BlueprintRepository,
    private val templateParser: TemplateParser
) : BlueprintLoader {

    companion object {
        private val LOG = LoggerFactory.getLogger(BlueprintDatabaseLoader::class.java)
    }

    override fun createBlueprint(blueprint: BlueprintData): String {
        LOG.info("Creating blueprint: {}", blueprint)
        require(!blueprintRepository.existsLockedBlueprintByName(blueprint.name)) {
            "Blueprint with name '${blueprint.name}' already exists"
        }
        templateParser.validateTemplate(blueprint.template)

        val entity = blueprint.toJpaEntity()
        val savedEntity = blueprintRepository.save(entity)
        return savedEntity.id.createBlueprintId().also {
            LOG.info("Created blueprint with ID: {}", it)
        }
    }

    override fun deleteBlueprint(id: String): BlueprintData {
        LOG.info("Deleting blueprint with ID: {}", id)
        val blueprintId = id.parseBlueprintDatabaseId()
        val existingBlueprint = blueprintRepository.findLockedBlueprintById(blueprintId)

        require(existingBlueprint != null) {
            "Blueprint with ID '$id' does not exist"
        }
        if (existingBlueprint.status == Status.DELETED) {
            throw IllegalArgumentException("Archived blueprint with ID '$id' cannot be deleted")
        }

        existingBlueprint.status = Status.DELETED
        existingBlueprint.name = "${existingBlueprint.name}__${System.currentTimeMillis()}"
        existingBlueprint.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        return blueprintRepository.save(existingBlueprint)
            .let {
                existingBlueprint.toGraphQLData()
            }
            .also {
                LOG.info("Deleted blueprint with ID: {}", id)
            }
    }

    override fun updateBlueprint(id: String, blueprint: BlueprintData) {
        LOG.info("Updating blueprint with ID: {}: {}", id, blueprint)
        val blueprintId = id.parseBlueprintDatabaseId()
        val existingBlueprint = blueprintRepository.findLockedBlueprintById(blueprintId)

        require(existingBlueprint != null) {
            "Blueprint with ID '$id' does not exist"
        }
        require(existingBlueprint.status != Status.DELETED) {
            "Deleted blueprint with ID '$id' cannot be updated"
        }
        require(existingBlueprint.name == blueprint.name) {
            "Blueprint name cannot be changed"
        }
        if (existingBlueprint.toGraphQLData() == blueprint) {
            LOG.info("Blueprint with ID '{}' does not require update", id)
            return
        }
        if (existingBlueprint.template != blueprint.template) {
            templateParser.validateTemplate(blueprint.template)
        }

        val entity = blueprint.toJpaEntity(blueprintId)
        entity.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        blueprintRepository.save(entity).also {
            LOG.info("Updated blueprint with ID: {}", id)
        }
    }

    override fun updateBlueprint(blueprint: BlueprintData) {
        val existingBlueprint = blueprintRepository.findLockedBlueprintByName(blueprint.name)

        require(existingBlueprint != null) {
            "Blueprint with name '${blueprint.name}' does not exist"
        }
        require(existingBlueprint.status != Status.DELETED) {
            "Deleted blueprint with name '${blueprint.name}' cannot be updated"
        }
        if (existingBlueprint.toGraphQLData() == blueprint) {
            LOG.info("Blueprint with name '{}' does not require update", blueprint.name)
            return
        }
        if (existingBlueprint.template != blueprint.template) {
            templateParser.validateTemplate(blueprint.template)
        }

        val entity = blueprint.toJpaEntity(existingBlueprint.id)
        entity.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        blueprintRepository.save(entity).also {
            LOG.info("Updated blueprint with name: {}", blueprint.name)
        }
    }
}