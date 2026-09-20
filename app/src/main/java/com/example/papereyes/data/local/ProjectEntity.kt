package com.example.papereyes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "projects",
    indices = [
        Index(
            value = ["name"],
            unique = true
        )
    ]
)
data class ProjectEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(
        collate = ColumnInfo.NOCASE
    )
    val name: String,

    val createdAt: Long =
        System.currentTimeMillis()
)