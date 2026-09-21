# Source and configuration inventory

The supplied implementation contained 42 production Kotlin files, nine JVM test classes and three instrumentation classes. The final application has 59 production Kotlin files, 26 JVM test classes and three instrumentation classes. All supplied production/test Kotlin and text build/resource/schema configurations were read; this inventory is not a claim of successful Android execution. Launcher artwork is inventoried as binary assets, not visually certified.

Unchanged source/configuration files are included: review scope was not limited to the modified files. Generated reports/logs and portable stub artifacts are not used to infer Android release readiness.

| Path | Review / role | SHA-256 |
|---|---|---|
| `.gitignore` | Source/build/tool support inspected | `50a0cd701468f4270b60d31f76f79ba64db66bc573cb0131dec54266f522dab5` |
| `app/.gitignore` | Source/build/tool support inspected | `5c3174b74edc6f2b9e0743594b8bd3c7f31e4f4405758cc9872b3fe1de1d28b4` |
| `app/build.gradle.kts` | Text configuration / resource / schema inspected | `ed1d4802cb822d4a47ab5e5e40556156fd00ea9e54ca0eb9d34b1c95f3ceed13` |
| `app/schemas/.gitkeep` | Source/build/tool support inspected | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| `app/schemas/com.example.papereyes.data.local.PaperDatabase/1.json` | Text configuration / resource / schema inspected | `e37472f0e62274b9643066ead1c4e405332d5ea77b44aa40fd6fe07d8f8cadd9` |
| `app/schemas/com.example.papereyes.data.local.PaperDatabase/4.json` | Text configuration / resource / schema inspected | `eba491016dbbda979b4c2f5172436c30e72a72dbff61c121150a6cd1f5ac17eb` |
| `app/src/androidTest/java/com/example/papereyes/data/local/PaperDatabaseTest.kt` | Kotlin source / test read; app tree syntax parsed | `368cc2d07c8b93699cb152c9233d1f40aa31da59c0bd26f4e4dd2c5d144b671b` |
| `app/src/androidTest/java/com/example/papereyes/ui/MainActivitySmokeTest.kt` | Kotlin source / test read; app tree syntax parsed | `260414aa53215cdc1699c8d8860388ccf29fbe71cf0ab21e07d63bc506109e73` |
| `app/src/androidTest/java/com/example/papereyes/util/ExternalLinkOpenerTest.kt` | Kotlin source / test read; app tree syntax parsed | `2977db71b5e8a7ad8c3e396fdb97c471537bb8505b74f58c5e6b12d3345d3127` |
| `app/src/main/AndroidManifest.xml` | Text configuration / resource / schema inspected | `7cc9f5d4e7a2173b5192c4ebf3c98f4df7855c3e7576166ff8eee222b46ccb40` |
| `app/src/main/java/com/example/papereyes/MainActivity.kt` | Kotlin source / test read; app tree syntax parsed | `b0fd0c18f07fd39c325d6eb21277fe5dd8152c64ec024ccb83790a0bfae43806` |
| `app/src/main/java/com/example/papereyes/data/local/LibraryRepository.kt` | Kotlin source / test read; app tree syntax parsed | `fee481125f683de3838dca6e8869e5062180795a554e12fdc2d20d8a5536ea90` |
| `app/src/main/java/com/example/papereyes/data/local/PaperDao.kt` | Kotlin source / test read; app tree syntax parsed | `aba8454b01b0bcebff1cfff02481bce8d665f94f0fde9f59a76fa902912da239` |
| `app/src/main/java/com/example/papereyes/data/local/PaperDatabase.kt` | Kotlin source / test read; app tree syntax parsed | `54bcafdce722ea87aec156e0c4392c1cdcd4c7fec0f9f68f6876d47f3b89f91c` |
| `app/src/main/java/com/example/papereyes/data/local/PaperProjectCrossRef.kt` | Kotlin source / test read; app tree syntax parsed | `552f8ec68a7c28f60fa511fcad5301a89c51a11db0c8f1de7522a0681965d418` |
| `app/src/main/java/com/example/papereyes/data/local/PaperWithProjects.kt` | Kotlin source / test read; app tree syntax parsed | `62e3a9bf02fa7d357dc5d93e2da5901c68ab1f651e25db05b745e60f41ec5838` |
| `app/src/main/java/com/example/papereyes/data/local/ProjectDao.kt` | Kotlin source / test read; app tree syntax parsed | `61b5cf94a4152835d1a3ec1316f61772afff84fc8b178b655f04f55952643d2c` |
| `app/src/main/java/com/example/papereyes/data/local/ProjectEntity.kt` | Kotlin source / test read; app tree syntax parsed | `22efbae6303cc5a7cebe49f641e40f1ab0a5bc9965144f112d2dd69f18132459` |
| `app/src/main/java/com/example/papereyes/data/local/ProjectWithPapers.kt` | Kotlin source / test read; app tree syntax parsed | `0cc9c42ddfaecb6c365e6aacd9a2bf2dd531e6cbc959c2df1a58db04c11ff4d1` |
| `app/src/main/java/com/example/papereyes/data/model/ScholarlyIdentifiers.kt` | Kotlin source / test read; app tree syntax parsed | `19fe3cae12a6499b3273093cf031daa2082a6c11ac5ca9647f65edc1821eca09` |
| `app/src/main/java/com/example/papereyes/data/model/paper.kt` | Kotlin source / test read; app tree syntax parsed | `bbcd66fe6145b9b64efac97496592e76367509f5790f557b5b70e6cb26f19f6e` |
| `app/src/main/java/com/example/papereyes/data/remote/ArxivApi.kt` | Kotlin source / test read; app tree syntax parsed | `0e413f2dcfbb49be36b7f8e2c483e77b11e942fff6fd9cb0b9c162bd1bd05a4a` |
| `app/src/main/java/com/example/papereyes/data/remote/ArxivClient.kt` | Kotlin source / test read; app tree syntax parsed | `38b6ef98e300a5639c888d99b66e05dcec5d0f28ba25172327c085f12771d7e9` |
| `app/src/main/java/com/example/papereyes/data/remote/CrossrefApi.kt` | Kotlin source / test read; app tree syntax parsed | `1f0fe5476fd53219899edfb39e452fe120f38e86325b097dd6adea6f0e26426d` |
| `app/src/main/java/com/example/papereyes/data/remote/CrossrefClient.kt` | Kotlin source / test read; app tree syntax parsed | `99fb44049f0f8f682f7b09c46ce9be8feb350e3e7fcefd2c12c3b0b4fb4aa549` |
| `app/src/main/java/com/example/papereyes/data/remote/InspireApi.kt` | Kotlin source / test read; app tree syntax parsed | `1680fcc1553d2e117f33212a8d24580002f37339dfcfb861a252316b045e2d00` |
| `app/src/main/java/com/example/papereyes/data/remote/InspireClient.kt` | Kotlin source / test read; app tree syntax parsed | `b06759711e6680a96854afa867ed5ffdbacf5e32124425b5fd92a29e5e81d39a` |
| `app/src/main/java/com/example/papereyes/data/remote/InspireRepository.kt` | Kotlin source / test read; app tree syntax parsed | `ebee8f3f2141ef8996a7b58506128b0b45d42a524c841f980bfe4193faa2ac7f` |
| `app/src/main/java/com/example/papereyes/data/remote/OpenAlexApi.kt` | Kotlin source / test read; app tree syntax parsed | `3f869ca867be8a3ede4e05872028d618291becc6d7c570993b96684889d15e86` |
| `app/src/main/java/com/example/papereyes/data/remote/OpenAlexProvider.kt` | Kotlin source / test read; app tree syntax parsed | `7de2ffe90d94e9a4355d1cdd55a6c7d6ec3da0c36923fef85b9db81032f1e920` |
| `app/src/main/java/com/example/papereyes/data/remote/PaperRepository.kt` | Kotlin source / test read; app tree syntax parsed | `baa130a18cea9c6f0e83da8cdaf376a06847c3e39205bbe3e5d7887a81de9b6a` |
| `app/src/main/java/com/example/papereyes/data/remote/RetryAfter.kt` | Kotlin source / test read; app tree syntax parsed | `1fbde5edbccfee2f1ca4b5e2defdb2125ad07123a37bd5020615b0cee760b23d` |
| `app/src/main/java/com/example/papereyes/data/remote/ScholarlyHttpClient.kt` | Kotlin source / test read; app tree syntax parsed | `7a2b5e25fbe70617e0663cb8d4c856ceb2a26bd8b62115b04e373da0978f154d` |
| `app/src/main/java/com/example/papereyes/data/remote/ScholarlyRequestPolicy.kt` | Kotlin source / test read; app tree syntax parsed | `03b9b81c685c6f3e21f250d2344437d70acd97beadf1e6c47e8574f3552507b9` |
| `app/src/main/java/com/example/papereyes/data/remote/SemanticScholarApi.kt` | Kotlin source / test read; app tree syntax parsed | `b7c975af157742710cd5680f0f6bcf1a1cf6a232f4b08706649e1fb6f6586d13` |
| `app/src/main/java/com/example/papereyes/data/remote/SemanticScholarClient.kt` | Kotlin source / test read; app tree syntax parsed | `85207b46125e2f3b7bd199779b2a1f367fa1315bbb1c8932d75355b6ecf2afdf` |
| `app/src/main/java/com/example/papereyes/domain/ArxivFeedParser.kt` | Kotlin source / test read; app tree syntax parsed | `93f3041f331aa771265cb1f0c800eab3fcdcbe396a536eea2bcc519c883439a4` |
| `app/src/main/java/com/example/papereyes/domain/PaperResolver.kt` | Kotlin source / test read; app tree syntax parsed | `079a5a7a717af1959a5ea9868baf52478f18f38469dc1aeb5e455b8294ec82e2` |
| `app/src/main/java/com/example/papereyes/domain/citation/JournalAliases.kt` | Kotlin source / test read; app tree syntax parsed | `5ca7e5ed12d0789643875ade133036b566d58c026d2f27cb978f0fe4f6279972` |
| `app/src/main/java/com/example/papereyes/domain/citation/JournalCitation.kt` | Kotlin source / test read; app tree syntax parsed | `b86c6d37671ea7fd78a2341deb9b807e04bb4a31f23c291c8a02cdcd7d49bb38` |
| `app/src/main/java/com/example/papereyes/domain/citation/JournalCitationParser.kt` | Kotlin source / test read; app tree syntax parsed | `6c4ebb6f608dabe341b6bf5d8ad3e222239da5fcbd87c72f72ac4b8005acba56` |
| `app/src/main/java/com/example/papereyes/domain/discovery/AbstractReconstruction.kt` | Kotlin source / test read; app tree syntax parsed | `d468ea563e868fcfa0f3ce48fa401dffa2c8da30d4788450e6de9035fb155a3d` |
| `app/src/main/java/com/example/papereyes/domain/discovery/ArxivEntryIdentity.kt` | Kotlin source / test read; app tree syntax parsed | `33abc5bbaf637a5f46ffbc34d5dac18728142363257a7d964cf2de989893329b` |
| `app/src/main/java/com/example/papereyes/domain/discovery/InteriorPageDiscovery.kt` | Kotlin source / test read; app tree syntax parsed | `bc17481ad1290dbfce09272c3b58d3a9e15f2cab80cd1b479605a15b3f097d38` |
| `app/src/main/java/com/example/papereyes/domain/discovery/PaperDiscoveryProvider.kt` | Kotlin source / test read; app tree syntax parsed | `0fa24b38b28ae11599c827745a1887e753aca62013e70753fba7a799c1e2ec45` |
| `app/src/main/java/com/example/papereyes/domain/discovery/PhraseQuery.kt` | Kotlin source / test read; app tree syntax parsed | `d5311f85f540c856ff29d811d48cbcd5be428643e80f80e1be5691e0927caaf1` |
| `app/src/main/java/com/example/papereyes/domain/evidence/DocumentEvidence.kt` | Kotlin source / test read; app tree syntax parsed | `b255dab19eea47cb55379d6bc1c9101d37c8e0bdfecec04aa1c573da81ab7a51` |
| `app/src/main/java/com/example/papereyes/domain/evidence/FingerprintExtractor.kt` | Kotlin source / test read; app tree syntax parsed | `5ac5d5cb8052fc649baef3a8b6bb8c45527bd5dbd9eaa1d7552e9eab0b1e30e6` |
| `app/src/main/java/com/example/papereyes/domain/telemetry/ScanTrace.kt` | Kotlin source / test read; app tree syntax parsed | `e7d2d5293e18008f38a27eee7c3d053584a34113525322fb4b4c8f8031ea67cb` |
| `app/src/main/java/com/example/papereyes/ocr/DocumentLayoutAnalyzer.kt` | Kotlin source / test read; app tree syntax parsed | `1c0332aea01f9ceb42766df5360135cfb41a4caa022d5c5c6c1d1dd819c52e62` |
| `app/src/main/java/com/example/papereyes/ocr/ImageSampling.kt` | Kotlin source / test read; app tree syntax parsed | `f3dc6b39bbbed290cdc48713e3183950260f43366dfb4885d2b551e61979ecca` |
| `app/src/main/java/com/example/papereyes/ocr/TextCandidateExtractor.kt` | Kotlin source / test read; app tree syntax parsed | `bdeb4a61452bb501b565ded962fdc3b28648caa5a6ad6f8d015d9752e03b56da` |
| `app/src/main/java/com/example/papereyes/ocr/TextRecognizerService.kt` | Kotlin source / test read; app tree syntax parsed | `c4df60af7d56420c6f3f5129df57826bde897c39254499221a75b699beb309ff` |
| `app/src/main/java/com/example/papereyes/ui/common/UserFacingError.kt` | Kotlin source / test read; app tree syntax parsed | `d4c539151c97bb3120249826f35778a7ca33c862d1606d75df738c99f3e6a9e8` |
| `app/src/main/java/com/example/papereyes/ui/detail/PaperDetailScreen.kt` | Kotlin source / test read; app tree syntax parsed | `305f1e5763c8b8ece739ad92384a87a8b596d2c64b1635854306db99db1a7b97` |
| `app/src/main/java/com/example/papereyes/ui/library/LibraryScreen.kt` | Kotlin source / test read; app tree syntax parsed | `8ec53bd668d8247486f770b3cf1560db1346f1e6ea15a4238ac82d36ccf3f781` |
| `app/src/main/java/com/example/papereyes/ui/library/ProjectScreen.kt` | Kotlin source / test read; app tree syntax parsed | `56de2d7a9021beb8ad3826bb90c4d310344d7948e612baf414ce82c4d679fead` |
| `app/src/main/java/com/example/papereyes/ui/live/LiveScanScreen.kt` | Kotlin source / test read; app tree syntax parsed | `b2b834c3377bc1dfe7f212e14262b899576dae93b325af71c5183484dcf7e49f` |
| `app/src/main/java/com/example/papereyes/ui/live/LiveScanUi.kt` | Kotlin source / test read; app tree syntax parsed | `3242ece69feb41e165ddf8d3c947de760c41ea082909af402651f77a9873d38a` |
| `app/src/main/java/com/example/papereyes/ui/live/LiveScanUtils.kt` | Kotlin source / test read; app tree syntax parsed | `bc96b18d61362a76c44f8cdb79be110f5de81ba95a4ac176732253c17c843b55` |
| `app/src/main/java/com/example/papereyes/ui/scan/ScanHomeUi.kt` | Kotlin source / test read; app tree syntax parsed | `e65455d855bffa2a275e244405ca4f3b46e482b4c2b6c45f93cf1294c1c4e873` |
| `app/src/main/java/com/example/papereyes/ui/scan/ScanScreen.kt` | Kotlin source / test read; app tree syntax parsed | `92a32ec360c8b6535b21f797489f1016c84f3cfa1c95bdb2bdd1da8daa71e10f` |
| `app/src/main/java/com/example/papereyes/ui/theme/Color.kt` | Kotlin source / test read; app tree syntax parsed | `6ed37a829cd7f127cd32ef9662e59d2309411d0e49b728c0238fcfd48103dc7d` |
| `app/src/main/java/com/example/papereyes/ui/theme/Theme.kt` | Kotlin source / test read; app tree syntax parsed | `d0a43fdf4af27789da76e9e7d8b7f5cc90310a7b971c267541505047de07bbae` |
| `app/src/main/java/com/example/papereyes/ui/theme/Type.kt` | Kotlin source / test read; app tree syntax parsed | `82b95ecf6f17a626ab28e4e02410aec5ae77ca174c8e1b883f5f059fb35d7232` |
| `app/src/main/java/com/example/papereyes/util/ExternalLinkOpener.kt` | Kotlin source / test read; app tree syntax parsed | `cb11ee36f0f6061a4edfe5fea4021fd17787e18cee7fe3f235d890cb11792b26` |
| `app/src/main/java/com/example/papereyes/util/concurrency/CompletionGate.kt` | Kotlin source / test read; app tree syntax parsed | `f4266a8b85ac03be92155e1ed21c6914abbe0d0fe2f699724c973341d3a40544` |
| `app/src/main/java/com/example/papereyes/util/concurrency/RequestGate.kt` | Kotlin source / test read; app tree syntax parsed | `a3d3c9e1ce93d32aa375866c38a2a912a2200c30931aebb0ad6abdfd49993a46` |
| `app/src/main/java/com/example/papereyes/util/concurrency/SuspendQueryCache.kt` | Kotlin source / test read; app tree syntax parsed | `dded577c454ea892db63dfc68e46ae8a7baf7a3a3ae2c6de1f1823f28442fb1d` |
| `app/src/main/keepRules/network-models.keep` | Text configuration / resource / schema inspected | `aaa41dfd8b7b1ca0b1c7d62a4b1f104a34715a9d3c0fe4d1723b22153eb3d765` |
| `app/src/main/keepRules/openalex.keep` | Text configuration / resource / schema inspected | `00e48e9bafc22856143044c3d10919630130fe513eb17ab7f2c2d1b5e2e80985` |
| `app/src/main/keepRules/rules.keep` | Text configuration / resource / schema inspected | `cd9f36ade2d8604e1b7cf24eff5b171f9c0816c0aa013ba7eb55be4fe2e1dac7` |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Text configuration / resource / schema inspected | `ed423c73a6f40a4d2909f0901e60527b3a807cd59e1b5593bcaae1808b1c6321` |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Text configuration / resource / schema inspected | `01d1a6a6c1234eb7fe270d097eb283d72b9c95ae5118886f1b6573aad280f1f7` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Text configuration / resource / schema inspected | `88f7653499ef524126ea5018a99baf9cc3269e7e584d6205dfdd76db39f39cc0` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Text configuration / resource / schema inspected | `88f7653499ef524126ea5018a99baf9cc3269e7e584d6205dfdd76db39f39cc0` |
| `app/src/main/res/mipmap-hdpi/ic_launcher.webp` | Retained binary launcher artwork (inventory only) | `dd00996198640ed28fbc09cdcd7a3807cf8707f3eb255b659634da3ca6a6ff01` |
| `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` | Retained binary launcher artwork (inventory only) | `1ed73f5341a69d3b41c7e02e126803f50cc8c3284adf4bbb737f0c93577aef07` |
| `app/src/main/res/mipmap-mdpi/ic_launcher.webp` | Retained binary launcher artwork (inventory only) | `846219e6f72fe9a6c104ca8919cbee36a101e7d2ff8da9da67b689a5888f060d` |
| `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` | Retained binary launcher artwork (inventory only) | `4e2c58b91de01130e6479e00cbbaaf6e77cd961dd2c8e303cf13cd077fa92632` |
| `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` | Retained binary launcher artwork (inventory only) | `398340dad816fc9a6338cb151a8cf1e45b926f9bfb70628b24c4bd2523cc94d4` |
| `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` | Retained binary launcher artwork (inventory only) | `91b490aef86574901137f0a252272e1f1add99c1ad709e0696ed68929b88d261` |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` | Retained binary launcher artwork (inventory only) | `58ae87fa0c5b5d1562d27fd648d2c061553fe20e3ed570bde588162d01ea7a27` |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` | Retained binary launcher artwork (inventory only) | `3009fad079f5772f30ecd767f98924367fbe0f81c30048d672d4fda2d5ca7d12` |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` | Retained binary launcher artwork (inventory only) | `f98fef5bc3bfe5b65692c40ad1cbae2bec4faf9f1b249c397626740db71b62d8` |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` | Retained binary launcher artwork (inventory only) | `5faf033745c8c882c43bb1b372d4e4a5890caec63d9892a2e756c954108b85dc` |
| `app/src/main/res/values/strings.xml` | Text configuration / resource / schema inspected | `5d57f2338ff28748790a4a40368216f51480e44eb24d7fb81f6af59d91342ad9` |
| `app/src/main/res/values/themes.xml` | Text configuration / resource / schema inspected | `88fa7364a3c1c82138977b0eaf8569abf4ee7e13921b7b896f3b38f2af1e66d0` |
| `app/src/main/res/xml/backup_rules.xml` | Text configuration / resource / schema inspected | `d56a2afaa615c7ff3b2ddcc9ee97625cf115c3abc7c24bdf552c62b9ced822bc` |
| `app/src/main/res/xml/data_extraction_rules.xml` | Text configuration / resource / schema inspected | `d981222d0f7721d7c0f2180605bf9ed7d3685352c36aee24e52d6b7f215aa471` |
| `app/src/test/java/com/example/papereyes/data/local/PaperDaoIdentityTest.kt` | Kotlin source / test read; app tree syntax parsed | `1f90075301c3c28ef2eefb10ef841df79d791d98a0a82e94bb58ffd1edec57ef` |
| `app/src/test/java/com/example/papereyes/data/model/PaperIdentityTest.kt` | Kotlin source / test read; app tree syntax parsed | `b194d545075c31730a6f252583b56b9918aa565171ea641f6fb51a2bae3eb202` |
| `app/src/test/java/com/example/papereyes/data/model/ScholarlyIdentifiersTest.kt` | Kotlin source / test read; app tree syntax parsed | `8b42d26444a20c5884e5e7900967f3f77f5e0f09ab3b8d36c34567d9c2f49628` |
| `app/src/test/java/com/example/papereyes/data/remote/ApiEncodingTest.kt` | Kotlin source / test read; app tree syntax parsed | `57e0b2888a6faa42654a12022c7aa5ba8a6985d6e5f0f7311bf858fb30726ef8` |
| `app/src/test/java/com/example/papereyes/data/remote/CitationSafetyRegressionTest.kt` | Kotlin source / test read; app tree syntax parsed | `53ba34865ff4745ac4f686c89b35c1267102d7a6305d7d8c2f1517061e632928` |
| `app/src/test/java/com/example/papereyes/data/remote/InspireEvidenceTest.kt` | Kotlin source / test read; app tree syntax parsed | `4a86f719cc7e86d946dcc2322a9df1ef5866fc553ece673a840028301ea2b6f5` |
| `app/src/test/java/com/example/papereyes/data/remote/OpenAlexProviderTest.kt` | Kotlin source / test read; app tree syntax parsed | `2ab373fe49e2c76d1bde0b78a766443743cf7eb9447688114504b2f72cd9a73a` |
| `app/src/test/java/com/example/papereyes/data/remote/PaperRepositoryNetworkTest.kt` | Kotlin source / test read; app tree syntax parsed | `f2f2b65e56935a598ebcdacf02c505093177803038614d57d506d6496df48f26` |
| `app/src/test/java/com/example/papereyes/domain/ArxivFeedParserTest.kt` | Kotlin source / test read; app tree syntax parsed | `aeb9cda774fe64b84c372bcb1797406995eba87e4bc957b85479fee9da55d2b1` |
| `app/src/test/java/com/example/papereyes/domain/PaperResolverEvidenceTest.kt` | Kotlin source / test read; app tree syntax parsed | `6d372040212919a187e7124fdc2f19b1732cb1b1bdb062630e8fc9444dba99de` |
| `app/src/test/java/com/example/papereyes/domain/citation/JournalCitationParserTest.kt` | Kotlin source / test read; app tree syntax parsed | `64d389f5cd0ed2d10da24d21bba3abba3aeccbd6f3085a01614d86fdc6a3b9da` |
| `app/src/test/java/com/example/papereyes/domain/discovery/AbstractReconstructionTest.kt` | Kotlin source / test read; app tree syntax parsed | `86e2e4df23de4ebe7b6eba062a3d4f6e78aa67ccc28bd0e2a137ec1abe7d6307` |
| `app/src/test/java/com/example/papereyes/domain/discovery/ArxivEntryIdentityTest.kt` | Kotlin source / test read; app tree syntax parsed | `b16d62a6b30d61f0a1bfbe36d9fce742aa8c4ba9af7302ff02f910a91b31e67a` |
| `app/src/test/java/com/example/papereyes/domain/discovery/InteriorPageDiscoveryTest.kt` | Kotlin source / test read; app tree syntax parsed | `98d2b6196f15cc5faa7369431d0301544f486196b40ab3cbe659a44e92473df6` |
| `app/src/test/java/com/example/papereyes/domain/discovery/PhraseQueryTest.kt` | Kotlin source / test read; app tree syntax parsed | `6ce0f1348b1833b5ba22197281b8fe19cd092d12705c514f7562367a4bb045d9` |
| `app/src/test/java/com/example/papereyes/domain/discovery/RetryAfterTest.kt` | Kotlin source / test read; app tree syntax parsed | `cd4821e15f16b9a0e62c2a5a5b4436009171ef02204656f0f9a7f287d4370d1e` |
| `app/src/test/java/com/example/papereyes/domain/evidence/FingerprintExtractorTest.kt` | Kotlin source / test read; app tree syntax parsed | `7d4595ed575289d90dc1ee9a69c1f57f02d776d099b554664c9defd4bc4c28bd` |
| `app/src/test/java/com/example/papereyes/domain/telemetry/ScanTraceTest.kt` | Kotlin source / test read; app tree syntax parsed | `36986478015ddfd00d09386ef562117a984d1e3a90adfa91c3b9d6283e7805e7` |
| `app/src/test/java/com/example/papereyes/ocr/AuditRegressionTest.kt` | Kotlin source / test read; app tree syntax parsed | `702353b5cf003233c0f8fb624fd880b6a2dffd23caabb607f59c2b14f79145c7` |
| `app/src/test/java/com/example/papereyes/ocr/DocumentLayoutAnalyzerTest.kt` | Kotlin source / test read; app tree syntax parsed | `5d008735f1be1c6a4c6c71a265e534fd310a43df4d758463c8d1e2c8ed0563d0` |
| `app/src/test/java/com/example/papereyes/ocr/ImageSamplingTest.kt` | Kotlin source / test read; app tree syntax parsed | `f62bd67561854ab0f11d188427436ff0e0d98ac6c6a2cb52865a34b68f65be48` |
| `app/src/test/java/com/example/papereyes/ocr/TextCandidateExtractorTest.kt` | Kotlin source / test read; app tree syntax parsed | `84d856873d1b8ab63b2266e7f5d995084ca8a1fdb448a130b4bff0b38c2ebd3d` |
| `app/src/test/java/com/example/papereyes/ui/live/LiveScanUtilsTest.kt` | Kotlin source / test read; app tree syntax parsed | `f6320280deebbb405292380e4a3e319459a782d436c87923b2decf92185487cf` |
| `app/src/test/java/com/example/papereyes/util/concurrency/CompletionGateTest.kt` | Kotlin source / test read; app tree syntax parsed | `714e115d58a1a66ebc83635583fdedaa24f7225c48c01e59b738a8bcd6663809` |
| `app/src/test/java/com/example/papereyes/util/concurrency/RequestGateTest.kt` | Kotlin source / test read; app tree syntax parsed | `5da0c10160137400234f7851337aedc938c2e5096735c8ae62db811d73764cca` |
| `app/src/test/java/com/example/papereyes/util/concurrency/SuspendQueryCacheTest.kt` | Kotlin source / test read; app tree syntax parsed | `70ad0bc2a90cf889efbfdb63d968ae1c7625d5bf54296454b504613129523724` |
| `build.gradle.kts` | Text configuration / resource / schema inspected | `be7dbfa1841b18b020f744e203b995d3dac5709c2a6b8e310fbcfaed0364919a` |
| `gradle/libs.versions.toml` | Text configuration / resource / schema inspected | `aefb74494811481c0761e802237b21bb054d450d474ece0c293069b7c67773ce` |
| `gradle/wrapper/gradle-wrapper.jar` | Retained Gradle wrapper distribution bootstrap binary | `76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3` |
| `gradle/wrapper/gradle-wrapper.properties` | Text configuration / resource / schema inspected | `420df2df02f6aa3e211cab38a8ea7e5ad1bcebe9c18bdf957f415c97794bd66b` |
| `gradle.properties` | Text configuration / resource / schema inspected | `cf774135f23cd1e6c054168ce772e24140800347f95b29819744c96f97e4ef26` |
| `gradlew` | Source/build/tool support inspected | `3238afb2aed5cb16eb7d6718077e7138059108f007b54179e9cc157c5a6e0e89` |
| `gradlew.bat` | Source/build/tool support inspected | `94102713eb8fb22d032397924c0f38ab2da783ba60d07054339f1190a0c4e2cd` |
| `settings.gradle.kts` | Text configuration / resource / schema inspected | `f0aff741d164cc947977ede8d5abdb51873f86cc199da3d52ebb1c4863386240` |
| `tools/benchmark_providers.py` | Source/build/tool support inspected | `c07cbdc942775f0389f7e9673bdfecd7c0326a2636c2ecdfcfe5fec871853ac5` |
| `tools/check_kotlin_syntax.py` | Source/build/tool support inspected | `ac421600a0ef5f94b3acba2f9db92f0ce97a59aa98b45f89a1b8ee32aa8041e7` |
| `tools/check_release_invariants.py` | Source/build/tool support inspected | `041b603dae564e5b5399a383c4475d142f67e78d8ae157686d5bcaf76b9e5dfc` |
| `tools/offline/CoreMicrobenchmark.kt` | Portable harness / explicit type shim | `379e32e8e8abfee7513630b08ffa8d102e5aed395196508e7a10bbb44939ec72` |
| `tools/offline/Runner.kt` | Portable harness / explicit type shim | `1362942eb33ba68c966c78e9195ca17212dd9cfb3689e4546496f373ebf87b11` |
| `tools/offline/SyntaxCheck.kt` | Portable harness / explicit type shim | `a4b98e14ef5d5dc78d82d804c18cfde619103639931c48cc6cbf1c1c68c23fb4` |
| `tools/offline/stubs/AndroidXml.kt` | Portable harness / explicit type shim | `f535cf58113b43461782c189749e56c34fb03acaa0e1f706240a2f723e73d5b6` |
| `tools/offline/stubs/GsonFactory.kt` | Portable harness / explicit type shim | `0adace77e747f00b83bf18979f230a6b6ac74680b6808704f4ee9d74ef21bcb5` |
| `tools/offline/stubs/Junit.kt` | Portable harness / explicit type shim | `3a7eaf259a4f906c572ca1f23ed2293c75bf8bb2b3f0ad72aebce15c23251c8a` |
| `tools/offline/stubs/MlKitText.kt` | Portable harness / explicit type shim | `7217ed0d50b1ba454f36591cd20e678ee60c49d00e544363b64adb60957a3ede` |
| `tools/offline/stubs/NetworkClients.kt` | Portable harness / explicit type shim | `a60b195f2206d772c6a6decabc5f090e410c09afc318549ec268aaf269c8c32f` |
| `tools/offline/stubs/Rect.kt` | Portable harness / explicit type shim | `7c20312add5f579330058dc5f6f5a9283840aa8dc8f5c404abe5d697047d4942` |
| `tools/offline/stubs/ResponseBody.kt` | Portable harness / explicit type shim | `0f27e1e1aed82cb7c414ae834b306e52b8feddc04cd6947a7d0d68cfb10a149f` |
| `tools/offline/stubs/Retrofit.kt` | Portable harness / explicit type shim | `d7ee3f198d3d66e2017540ab23d7aaeb5de82e38d936da3b47bbe120692afa21` |
| `tools/offline/stubs/RetrofitAnnotations.kt` | Portable harness / explicit type shim | `2bfe79b7a22fa2607b12bb22e4a14df6f25e9305ae0893e22cb3c1673e07dfbf` |
| `tools/offline/stubs/Room.kt` | Portable harness / explicit type shim | `c53566285f5732e6ab58fd465f1b187ed4cdcb49a913bb9266971b1bef21db5f` |
| `tools/offline/stubs/SerializedName.kt` | Portable harness / explicit type shim | `07fd0f505dffea3d3b30039b06490abbdd281bbadfe0ab0afa4badde5cf1aad7` |
| `tools/offline/stubs/XmlPull.kt` | Portable harness / explicit type shim | `f3f205a0e772185e8cdda0cd067136c04f34fc29da058472be707ac065abe2da` |
| `tools/test_benchmark_tools.py` | Source/build/tool support inspected | `825e8a25db914b61c964e69e68cd69c8513fec2dfc3a8c4975265a607acaa91d` |
| `tools/verify.ps1` | Source/build/tool support inspected | `d692aff05a934b2097f04c3dc0e63283aada4b7c593444713b52fc0090da62a6` |
| `tools/verify.sh` | Source/build/tool support inspected | `79030996d3b3b6bdcc532a60ddfc9ce092906c9d61ec745dab928ebdab183773` |
| `tools/verify_portable.py` | Source/build/tool support inspected | `dfd35afc5e15c07ed6d48256d94e7c1f7752eea04bdfe1243f15d1b4a57b9826` |
| `tools/verify_schema.py` | Source/build/tool support inspected | `0d647387f571339685e4f369fed8f221edaa86fbbbd5e214b620f99e47d799b1` |
