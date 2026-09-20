package com.example.papereyes.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.papereyes.data.model.Paper


@Entity(
    tableName = "paper_project_cross_ref",

    primaryKeys = [
        "paperId",
        "projectId"
    ],

    foreignKeys = [

        ForeignKey(
            entity = Paper::class,
            parentColumns = ["id"],
            childColumns = ["paperId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index("paperId"),
        Index("projectId")
    ]
)
data class PaperProjectCrossRef(

    val paperId: Int,

    val projectId: Int
)