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

import com.elisapolystar.blueprints.database.repository.BlueprintRepository
import com.elisapolystar.blueprints.graphql.model.BlueprintData
import com.elisapolystar.blueprints.properties.InitBlueprintsProperties
import com.elisapolystar.blueprints.services.api.BlueprintLoader
import com.fasterxml.jackson.core.PrettyPrinter
import com.google.gson.Gson
import jakarta.annotation.PostConstruct
import java.nio.file.Files.isRegularFile
import java.nio.file.Files.lines
import java.nio.file.Files.walk
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.stream.Stream
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
@Profile("!test") // exclude this service from component/integration tests with this tag
internal class BlueprintInitLoader(
    private val blueprintRepository: BlueprintRepository,
    private val blueprintLoader: BlueprintLoader,
    private val txManager: PlatformTransactionManager,
    private val initBlueprintsProperties: InitBlueprintsProperties
) {
    companion object {
        private const val BLUEPRINTS_DIRECTORY = "blueprints"
        private val GSON = Gson()

        private fun streamAllJsonFilesFromResources(directory: String = BLUEPRINTS_DIRECTORY): Stream<String> {
            val path = Paths.get(directory)
            return if (!path.toFile().exists()) {
                Stream.empty()
            } else walk(path)
                .filter { isRegularFile(it) && it.toString().endsWith(".json") }
                .map { lines(it).collect(Collectors.joining(System.lineSeparator())) }
        }

        private fun parseBlueprintData(jsonFileContent: String): BlueprintData? {
            return GSON.fromJson(jsonFileContent, BlueprintData::class.java)
        }
    }

    @PostConstruct
    fun init() {
        val tx = TransactionTemplate(txManager)
        tx.execute {
            streamAllJsonFilesFromResources(initBlueprintsProperties.blueprintsDirectory)
                .map { parseBlueprintData(it) }
                .filter { it != null }
                .forEach { saveBlueprintInDatabase(it!!) }
        }
    }

    private fun saveBlueprintInDatabase(blueprintData: BlueprintData) {
        if (blueprintRepository.existsLockedBlueprintByName(blueprintData.name)) {
            blueprintLoader.updateBlueprint(blueprintData)
        } else {
            blueprintLoader.createBlueprint(blueprintData)
        }
    }
}