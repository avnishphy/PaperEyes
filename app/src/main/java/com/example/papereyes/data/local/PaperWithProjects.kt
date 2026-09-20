package com.example.papereyes.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.papereyes.data.model.Paper


data class PaperWithProjects(

    @Embedded
    val paper: Paper,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PaperProjectCrossRef::class,
            parentColumn = "paperId",
            entityColumn = "projectId"
        )
    )
    val projects: List<ProjectEntity>
)