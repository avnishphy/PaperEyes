// Deliberately no networking. Production API interfaces/DTOs and repositories
// are compiled; tests inject local fake APIs. Real client wiring is not tested.
package com.example.papereyes.data.remote
object CrossrefClient {val api:CrossrefApi get()=error("Inject a test API")}
object InspireClient {val api:InspireApi get()=error("Inject a test API")}
object ScholarlyHttpClient {val client:Any get()=error("No OkHttp runtime")}

object ArxivClient {val api:ArxivApi get()=error("Inject a test API")}
object SemanticScholarClient {val api:SemanticScholarApi get()=error("Inject a test API")}
