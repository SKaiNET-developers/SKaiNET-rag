package sk.ainet.rag.core

/**
 * The retrieval evaluation set: questions phrased the way a developer types them, each paired
 * with the document that should answer. Written for this repository against the Java LTS corpus.
 *
 * Expectations are per document, not per chunk — BM25 with field weights is reliable at picking
 * the right document and nearly arbitrary at ordering sections inside it, so a chunk-level
 * expectation would pin noise. The dense leg is what makes section choice meaningful, and this
 * set is the baseline it gets measured against.
 */
object EvalQueries {

    data class Case(val query: String, val expectedDocId: String)

    val cases: List<Case> = listOf(
        // Java 21
        Case("which jep introduced virtual threads", "java-21-api"),
        Case("virtual threads final release", "java-21-api"),
        Case("sequenced collections interface", "java-21-api"),
        Case("key encapsulation mechanism", "java-21-api"),
        Case("string templates", "java-21-language"),
        Case("record patterns", "java-21-language"),
        Case("unnamed variables and underscore", "java-21-language"),
        Case("generational zgc", "java-21-jvm"),
        Case("dynamic loading of agents warning", "java-21-jvm"),
        Case("when did java 21 come out", "java-21"),
        // Java 17
        Case("sealed classes", "java-17-language"),
        Case("always strict floating point semantics", "java-17-language"),
        Case("deprecate the security manager", "java-17-api"),
        Case("remove rmi activation", "java-17-api"),
        Case("applet api deprecated", "java-17-api"),
        Case("new pseudo random number generators", "java-17-api"),
        Case("macos metal rendering pipeline", "java-17-jvm"),
        Case("strongly encapsulate jdk internals", "java-17-jvm"),
        Case("class file major version of java 17", "java-17"),
        // Java 11
        Case("http client standardized", "java-11-api"),
        Case("java ee and corba removed", "java-11-api"),
        Case("nashorn javascript engine deprecated", "java-11-api"),
        Case("epsilon no-op garbage collector", "java-11-jvm"),
        Case("nest based access control", "java-11-jvm"),
        Case("low overhead heap profiling", "java-11-jvm"),
        Case("var in lambda parameters", "java-11-language"),
        Case("launch single file source code", "java-11-tools"),
        Case("flight recorder", "java-11-tools"),
        Case("javadoc frames gone", "java-11-tools"),
        Case("pack200 deprecated", "java-11-tools"),
        Case("java 11 unicode version", "java-11"),
        // Java 8
        Case("lambda expressions", "java-8-language"),
        Case("default methods in interfaces", "java-8-language"),
        Case("repeating annotations", "java-8-language"),
        Case("effectively final variables", "java-8-language"),
        Case("streams api", "java-8-api"),
        Case("new date and time api", "java-8-api"),
        Case("permanent generation removed", "java-8-jvm"),
        Case("aes cpu instructions", "java-8-jvm"),
        Case("when did java 8 come out", "java-8"),
        // Java 25
        Case("compact object headers", "java-25-jvm"),
        Case("generational shenandoah", "java-25-jvm"),
        Case("ahead of time method profiling", "java-25-jvm"),
        Case("module import declarations", "java-25-language"),
        Case("flexible constructor bodies", "java-25-language"),
        Case("primitive types in patterns", "java-25-language"),
        Case("stable values", "java-25-api"),
        Case("pem encodings of cryptographic objects", "java-25-api"),
        Case("key derivation function api", "java-25-api"),
        // Cross-release
        Case("which java releases are lts", "java-lts-overview"),
        Case("lts release dates", "java-lts-overview"),
    )
}
