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

import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Mutation
import com.elisapolystar.blueprints.graphql.model.BlueprintData
import com.elisapolystar.blueprints.graphql.model.BlueprintOutput
import com.elisapolystar.blueprints.services.api.BlueprintLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

@Controller
class BlueprintMutations : Mutation {

    @Autowired
    private lateinit var blueprintLoader: BlueprintLoader

    fun createBlueprint(blueprint: BlueprintData): BlueprintOutput {
        val id = blueprintLoader.createBlueprint(blueprint)
        return BlueprintOutput(ID(id), blueprint)
    }

    fun deleteBlueprint(id: ID): BlueprintOutput {
        val blueprintData = blueprintLoader.deleteBlueprint(id.value)
        return BlueprintOutput(id, blueprintData)
    }

    fun updateBlueprint(id: ID, blueprint: BlueprintData): BlueprintOutput {
        blueprintLoader.updateBlueprint(id.value, blueprint)
        return BlueprintOutput(id, blueprint)
    }
}