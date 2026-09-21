package com.example.papereyes.data.local

import com.example.papereyes.data.model.Paper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Executes real DAO business methods. Fake storage is NOT Room/transaction validation. */
class PaperDaoIdentityTest {
 private class MemoryDao:PaperDao() {
  val rows=mutableListOf<Paper>()
  override suspend fun insertPaperInternal(paper:Paper):Long {
   val id=(rows.maxOfOrNull{it.id}?:0)+1;rows+=paper.copy(id=id);return id.toLong()
  }
  override suspend fun updatePaperInternal(paper:Paper){rows[rows.indexOfFirst{it.id==paper.id}]=paper}
  override suspend fun deletePaper(paper:Paper){rows.removeAll{it.id==paper.id}}
  override fun getAllPapers():Flow<List<Paper>> = flowOf(rows.toList())
  override suspend fun findByIdentityKey(identityKey:String)=rows.firstOrNull{it.identityKey==identityKey}
  override suspend fun loadIdentityCandidates()=rows.toList()
  override suspend fun findById(paperId:Int)=rows.firstOrNull{it.id==paperId}
 }
 private fun paper(title:String,doi:String?=null,year:Int?=2023)=Paper(title=title,authors="Author",year=year,doi=doi,url=null)
 @Test fun whitespaceVariantUpgradesInPlaceAndPreservesId()=runBlocking {
  val dao=MemoryDao();val original=dao.insertOrGetPaper(paper("Generalized  Parton Distributions"))
  val updated=dao.insertOrGetPaper(paper(" Generalized Parton\tDistributions ","https://doi.org/10.1000/ABC"))
  assertFalse(updated.inserted);assertEquals(original.paper.id,updated.paper.id)
  assertEquals("10.1000/abc",updated.paper.doi);assertEquals(1,dao.rows.size)
 }
 @Test fun unicodeCaseUsesSameCanonicalizerAsIdentityKey()=runBlocking {
  val dao=MemoryDao();val first=dao.insertOrGetPaper(paper("ÉLECTRON correlations"))
  val second=dao.insertOrGetPaper(paper("électron correlations","10.1000/example"))
  assertEquals(first.paper.id,second.paper.id);assertEquals(1,dao.rows.size)
 }
 @Test fun differentKnownYearsOrDoisDoNotMerge()=runBlocking {
  val dao=MemoryDao()
  assertTrue(dao.insertOrGetPaper(paper("Same title","10.1000/a",2023)).inserted)
  assertTrue(dao.insertOrGetPaper(paper("Same title","10.1000/b",2023)).inserted)
  assertTrue(dao.insertOrGetPaper(paper("Same title",null,2024)).inserted)
  assertEquals(3,dao.rows.size)
 }
 @Test fun ambiguousTitleOnlyInputDoesNotPickOneOfTwoDois()=runBlocking {
  val dao=MemoryDao();dao.insertOrGetPaper(paper("Shared","10.1000/a"));dao.insertOrGetPaper(paper("Shared","10.1000/b"))
  assertTrue(dao.insertOrGetPaper(paper("Shared")).inserted)
 }
}
