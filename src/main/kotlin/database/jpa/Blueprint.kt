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
package com.elisapolystar.blueprints.database.jpa

import com.elisapolystar.blueprints.commons.ConnectionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType

@Entity
@Table(
    name = "blueprint",
    uniqueConstraints = [
        UniqueConstraint(
            name = "blueprint_name_key",
            columnNames = ["name"]
        )
    ]
)
internal class Blueprint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int,

    @Column(name = "name", nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", columnDefinition = "connection_type", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var connectionType: ConnectionType,

    @Column(name = "vendor_pattern", nullable = true)
    var vendorPattern: String?,

    @Column(name = "model_pattern", nullable = true)
    var modelPattern: String?,

    @Column(name = "version_pattern", nullable = true)
    var versionPattern: String?,

    @Column(name = "template", nullable = false)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType::class)
    var template: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "status", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var status: Status = Status.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
)
