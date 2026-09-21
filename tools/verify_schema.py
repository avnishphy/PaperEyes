#!/usr/bin/env python3
"""Actual SQLite checks against exported Room DDL. NOT Room codegen/instrumentation."""
import json, pathlib, sqlite3, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SCHEMA=ROOT/"app/schemas/com.example.papereyes.data.local.PaperDatabase/4.json"
class SchemaTest(unittest.TestCase):
 def setUp(self):
  self.db=sqlite3.connect(":memory:");self.db.execute("PRAGMA foreign_keys=ON")
  schema=json.loads(SCHEMA.read_text())["database"]
  self.assertEqual(schema["version"],4)
  for entity in schema["entities"]:
   self.db.execute(entity["createSql"].replace("${TABLE_NAME}",entity["tableName"]))
   for index in entity["indices"]:
    self.db.execute(index["createSql"].replace("${TABLE_NAME}",entity["tableName"]))
 def tearDown(self): self.db.close()
 def paper(self,key="doi:10.1000/a"):
  return self.db.execute("INSERT INTO papers(title,authors,savedAt,identityKey) VALUES('Title','Author',0,?)",(key,)).lastrowid
 def project(self):return self.db.execute("INSERT INTO projects(name,createdAt) VALUES('GPD',0)").lastrowid
 def test_identity_unique_index(self):
  self.paper()
  with self.assertRaises(sqlite3.IntegrityError):self.paper()
 def test_distinct_doi_identity_remains_distinct(self):
  self.paper("doi:10.1000/a");self.paper("doi:10.1000/b")
  self.assertEqual(2,self.db.execute("SELECT COUNT(*) FROM papers").fetchone()[0])
 def test_membership_duplicate_is_rejected(self):
  p,j=self.paper(),self.project();self.db.execute("INSERT INTO paper_project_cross_ref VALUES(?,?)",(p,j))
  with self.assertRaises(sqlite3.IntegrityError):self.db.execute("INSERT INTO paper_project_cross_ref VALUES(?,?)",(p,j))
 def test_stale_paper_or_project_is_rejected(self):
  p,j=self.paper(),self.project()
  for pair in [(999,j),(p,999)]:
   with self.assertRaises(sqlite3.IntegrityError):self.db.execute("INSERT INTO paper_project_cross_ref VALUES(?,?)",pair)
 def test_paper_delete_cascades_only_membership(self):
  p,j=self.paper(),self.project();self.db.execute("INSERT INTO paper_project_cross_ref VALUES(?,?)",(p,j))
  self.db.execute("DELETE FROM papers WHERE id=?",(p,))
  self.assertEqual(0,self.db.execute("SELECT COUNT(*) FROM paper_project_cross_ref").fetchone()[0])
  self.assertEqual(1,self.db.execute("SELECT COUNT(*) FROM projects").fetchone()[0])
 def test_project_delete_cascades_only_membership(self):
  p,j=self.paper(),self.project();self.db.execute("INSERT INTO paper_project_cross_ref VALUES(?,?)",(p,j))
  self.db.execute("DELETE FROM projects WHERE id=?",(j,))
  self.assertEqual(0,self.db.execute("SELECT COUNT(*) FROM paper_project_cross_ref").fetchone()[0])
  self.assertEqual(1,self.db.execute("SELECT COUNT(*) FROM papers").fetchone()[0])
 def test_transaction_rollback(self):
  self.db.execute("BEGIN")
  self.paper();self.project();self.db.rollback()
  self.assertEqual(0,self.db.execute("SELECT COUNT(*) FROM papers").fetchone()[0])
if __name__=="__main__":unittest.main(verbosity=2)
