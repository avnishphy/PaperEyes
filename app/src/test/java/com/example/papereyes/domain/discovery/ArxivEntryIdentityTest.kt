package com.example.papereyes.domain.discovery
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
class ArxivEntryIdentityTest {
 @Test fun errorAndUnrelatedEntriesAreRejected() {
  assertFalse(ArxivEntryIdentity.matches("2609.20448","http://arxiv.org/api/errors#incorrect_id_format","Error"))
  assertFalse(ArxivEntryIdentity.matches("2609.20448","http://arxiv.org/abs/2609.12345v1","Another paper"))
 }
 @Test fun versionAndLegacyIdentityAreComparedSafely() {
  assertTrue(ArxivEntryIdentity.matches("2609.20448","http://arxiv.org/abs/2609.20448v2","Paper"))
  assertFalse(ArxivEntryIdentity.matches("2609.20448v1","http://arxiv.org/abs/2609.20448v2","Paper"))
  assertTrue(ArxivEntryIdentity.matches("hep-ph/9901234","http://arxiv.org/abs/hep-ph/9901234v2","Paper"))
 }
}
