package com.example.papereyes.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.papereyes.data.model.Paper


data class ProjectWithPapers(

    @Embedded
    val project: ProjectEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PaperProjectCrossRef::class,
            parentColumn = "projectId",
            entityColumn = "paperId"
        )
    )
    val papers: List<Paper>
)