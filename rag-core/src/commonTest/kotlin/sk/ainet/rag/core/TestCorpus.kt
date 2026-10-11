package sk.ainet.rag.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The frozen evaluation corpus: the Java LTS documents produced by `samples/corpus-java` from
 * Java Version Almanac data (CC BY-SA 4.0 - attribution in samples/corpus-java/data/SNAPSHOT.md),
 * embedded as base64 because Kotlin/Native test binaries have no classpath to read resources
 * from, plus one hand-written draft document exercising the unpublished path.
 *
 * Frozen on purpose: the golden retrieval tests must move when the chunker or the tokenizer
 * moves, never because somebody regenerated the live corpus. To refresh after a deliberate
 * corpus change, base64-encode each file under samples/docs-java (UTF-8, unwrapped) into [raw].
 */
@OptIn(ExperimentalEncodingApi::class)
object TestCorpus {

    /**
     * Fixture-only, not part of the shipped corpus: a draft document that must never reach an
     * index. Its marker vocabulary (xylophone compaction) exists nowhere else, so a query for it
     * proves drafts stay out.
     */
    val draftDocument: String = """
        |---
        |id: java-next-preview
        |title: Early look at the next Java
        |category: release
        |aliases: [vaporware]
        |last_reviewed: 2026-10-10
        |status: draft
        |---
        |
        |Working notes on features still in flight. Nothing in this file is committed anywhere.
        |
        |## Speculative features
        |Rumored experiments live here until a JEP exists. The marker term xylophone compaction
        |appears only in this draft, so retrieval tests can prove drafts are never indexed.
    """.trimMargin()

    val files: Map<String, String> by lazy {
        raw.mapValues { (_, encoded) -> Base64.decode(encoded).decodeToString() } +
            ("java-next-preview.md" to draftDocument)
    }

    private val raw: Map<String, String> = mapOf(
        "java-11-api.md" to "LS0tCmlkOiBqYXZhLTExLWFwaQp0aXRsZTogSmF2YSAxMSBBUEkgY2hhbmdlcwpjYXRlZ29yeTogYXBpCmFsaWFzZXM6IFtK" +
            "RVAgMzIxLCBKRVAgMzIwLCBKRVAgMzI3LCBKRVAgMzM1LCBKRVAgMzI0LCBKRVAgMzI5LCBKRVAgMzMyXQpsYXN0X3Jldmll" +
            "d2VkOiAyMDI2LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAxMSBzaGlwcGVkIDYgQVBJIGNoYW5nZXMuIEVh" +
            "Y2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBIVFRQIENsaWVudApIVFRQIENsaWVudCBpcyBhbiBBUEkgY2hh" +
            "bmdlIGluIEphdmEgMTEsIHNwZWNpZmllZCBieSBKRVAgMzIxLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBv" +
            "cnRlZCBjaGFuZ2UgaW4gSmF2YSAxMS4KCiMjIEphdmEgRUUgYW5kIENPUkJBIHJlbW92ZWQKSmF2YSBFRSBhbmQgQ09SQkEg" +
            "cmVtb3ZlZCBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMTEsIHNwZWNpZmllZCBieSBKRVAgMzIwLgpJdCBzaGlwcGVkIGFz" +
            "IGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAxMS4KCiMjIFVuaWNvZGUgMTAgU3VwcG9ydApVbmlj" +
            "b2RlIDEwIFN1cHBvcnQgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDExLCBzcGVjaWZpZWQgYnkgSkVQIDMyNy4KSXQgc2hp" +
            "cHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMTEuCgojIyBOYXNob3JuIEphdmFTY3Jp" +
            "cHQgRW5naW5lIGRlcHJlY2F0ZWQKTmFzaG9ybiBKYXZhU2NyaXB0IEVuZ2luZSBkZXByZWNhdGVkIGlzIGFuIEFQSSBjaGFu" +
            "Z2UgaW4gSmF2YSAxMSwgc3BlY2lmaWVkIGJ5IEpFUCAzMzUuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9y" +
            "dGVkIGNoYW5nZSBpbiBKYXZhIDExLgoKIyMgTmV3IENyeXB0b2dyYXBoaWMgQWxnb3JpdGhtcwpOZXcgQ3J5cHRvZ3JhcGhp" +
            "YyBBbGdvcml0aG1zIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSAxMSwgc3BlY2lmaWVkIGJ5IEpFUCAzMjQgYW5kIEpFUCAz" +
            "MjkuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDExLgoKIyMgVExTIDEu" +
            "MwpUTFMgMS4zIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSAxMSwgc3BlY2lmaWVkIGJ5IEpFUCAzMzIuCkl0IHNoaXBwZWQg" +
            "YXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDExLgoK",
        "java-11-jvm.md" to "LS0tCmlkOiBqYXZhLTExLWp2bQp0aXRsZTogSmF2YSAxMSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlcwpjYXRlZ29yeToganZt" +
            "CmFsaWFzZXM6IFtKRVAgMzA5LCBKRVAgMzE4LCBKRVAgMzMzLCBKRVAgMTgxLCBKRVAgMzMxLCBKRVAgMzE1XQpsYXN0X3Jl" +
            "dmlld2VkOiAyMDI2LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAxMSBzaGlwcGVkIDYgSlZNIGFuZCBydW50" +
            "aW1lIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBDT05TVEFOVF9EeW5hbWljCkNPTlNU" +
            "QU5UX0R5bmFtaWMgaXMgYSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlIGluIEphdmEgMTEsIHNwZWNpZmllZCBieSBKRVAgMzA5" +
            "LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAxMS4KCiMjIEVwc2lsb24g" +
            "R0MKRXBzaWxvbiBHQyBpcyBhIEpWTSBhbmQgcnVudGltZSBjaGFuZ2UgaW4gSmF2YSAxMSwgc3BlY2lmaWVkIGJ5IEpFUCAz" +
            "MTguCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDExLgoKIyMgWkdDClpH" +
            "QyBpcyBhIEpWTSBhbmQgcnVudGltZSBjaGFuZ2UgaW4gSmF2YSAxMSwgc3BlY2lmaWVkIGJ5IEpFUCAzMzMuCkl0IHNoaXBw" +
            "ZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDExLgoKIyMgTmVzdC1CYXNlZCBBY2Nlc3Mg" +
            "Q29udHJvbApOZXN0LUJhc2VkIEFjY2VzcyBDb250cm9sIGlzIGEgSlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDEx" +
            "LCBzcGVjaWZpZWQgYnkgSkVQIDE4MS4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGlu" +
            "IEphdmEgMTEuCgojIyBMb3ctT3ZlcmhlYWQgSGVhcCBQcm9maWxpbmcKTG93LU92ZXJoZWFkIEhlYXAgUHJvZmlsaW5nIGlz" +
            "IGEgSlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDExLCBzcGVjaWZpZWQgYnkgSkVQIDMzMS4KSXQgc2hpcHBlZCBh" +
            "cyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMTEuCgojIyBJbXByb3ZlIEFhcmNoNjQgSW50cmlu" +
            "c2ljcwpJbXByb3ZlIEFhcmNoNjQgSW50cmluc2ljcyBpcyBhIEpWTSBhbmQgcnVudGltZSBjaGFuZ2UgaW4gSmF2YSAxMSwg" +
            "c3BlY2lmaWVkIGJ5IEpFUCAzMTUuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBK" +
            "YXZhIDExLgoK",
        "java-11-language.md" to "LS0tCmlkOiBqYXZhLTExLWxhbmd1YWdlCnRpdGxlOiBKYXZhIDExIGxhbmd1YWdlIGNoYW5nZXMKY2F0ZWdvcnk6IGxhbmd1" +
            "YWdlCmFsaWFzZXM6IFtKRVAgMzIzXQpsYXN0X3Jldmlld2VkOiAyMDI2LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoK" +
            "SmF2YSAxMSBzaGlwcGVkIDEgbGFuZ3VhZ2UgY2hhbmdlcy4gRWFjaCBzZWN0aW9uIGNvdmVycyBvbmUgb2YgdGhlbS4KCiMj" +
            "IExvY2FsIFZhcmlhYmxlIFN5bnRheCBmb3IgTGFtYmRhIFBhcmFtZXRlcnMKTG9jYWwgVmFyaWFibGUgU3ludGF4IGZvciBM" +
            "YW1iZGEgUGFyYW1ldGVycyBpcyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZhIDExLCBzcGVjaWZpZWQgYnkgSkVQIDMyMy4K" +
            "SXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMTEuCgo=",
        "java-11-tools.md" to "LS0tCmlkOiBqYXZhLTExLXRvb2xzCnRpdGxlOiBKYXZhIDExIHRvb2xpbmcgY2hhbmdlcwpjYXRlZ29yeTogdG9vbHMKYWxp" +
            "YXNlczogW0pFUCAzMzAsIEpFUCAzMjgsIEpFUCAzMzZdCmxhc3RfcmV2aWV3ZWQ6IDIwMjYtMTAtMTAKc3RhdHVzOiBwdWJs" +
            "aXNoZWQKLS0tCgpKYXZhIDExIHNoaXBwZWQgNCB0b29saW5nIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9m" +
            "IHRoZW0uCgojIyBTaW5nbGUgU291cmNlIEZpbGUgTGF1bmNoClNpbmdsZSBTb3VyY2UgRmlsZSBMYXVuY2ggaXMgYSB0b29s" +
            "aW5nIGNoYW5nZSBpbiBKYXZhIDExLCBzcGVjaWZpZWQgYnkgSkVQIDMzMC4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxs" +
            "eSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMTEuCgojIyBGbGlnaHQgUmVjb3JkZXIKRmxpZ2h0IFJlY29yZGVyIGlzIGEg" +
            "dG9vbGluZyBjaGFuZ2UgaW4gSmF2YSAxMSwgc3BlY2lmaWVkIGJ5IEpFUCAzMjguCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwg" +
            "ZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDExLgoKIyMgUGFjazIwMCBkZXByZWNhdGVkClBhY2syMDAgZGVwcmVj" +
            "YXRlZCBpcyBhIHRvb2xpbmcgY2hhbmdlIGluIEphdmEgMTEsIHNwZWNpZmllZCBieSBKRVAgMzM2LgpJdCBzaGlwcGVkIGFz" +
            "IGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAxMS4KCiMjIE5vIG1vcmUgZnJhbWVzIGluIEphdmFE" +
            "b2MKTm8gbW9yZSBmcmFtZXMgaW4gSmF2YURvYyBpcyBhIHRvb2xpbmcgY2hhbmdlIGluIEphdmEgMTEsIHNwZWNpZmllZCBi" +
            "eSBpc3N1ZSBKREstODE5NjIwMi4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEph" +
            "dmEgMTEuCgo=",
        "java-11.md" to "LS0tCmlkOiBqYXZhLTExCnRpdGxlOiBKYXZhIDExCmNhdGVnb3J5OiByZWxlYXNlCmFsaWFzZXM6IFtKREsgMTEsIEphdmEg" +
            "U0UgMTFdCmxhc3RfcmV2aWV3ZWQ6IDIwMjYtMTAtMTAKc3RhdHVzOiBwdWJsaXNoZWQKLS0tCgpSZWxlYXNlIGZhY3RzIGZv" +
            "ciBKYXZhIDExLCBhIExUUyByZWxlYXNlIG9mIHRoZSBKYXZhIHBsYXRmb3JtLgoKIyMgUmVsZWFzZSBmYWN0cwpKYXZhIDEx" +
            "IHJlYWNoZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAxOC0wOS0yNSBhcyBhIGxvbmctdGVybSBzdXBwb3J0IChMVFMp" +
            "IHJlbGVhc2UuCkl0cyBzdXBwb3J0IHdpbmRvdyBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMjctMTAuClRoZSByZWxlYXNl" +
            "IGlzIGRlZmluZWQgYnkgdW1icmVsbGEgSlNSIDM4NC4KCiMjIENsYXNzIGZpbGVzIGFuZCBVbmljb2RlClRoZSBjb21waWxl" +
            "ciBpbiBKYXZhIDExIGVtaXRzIGNsYXNzIGZpbGVzIHdpdGggbWFqb3IgdmVyc2lvbiA1NS4wLgpKYXZhIDExIGJ1bmRsZXMg" +
            "VW5pY29kZSB2ZXJzaW9uIDEwLjAuMC4KCiMjIE1haW50ZW5hbmNlIHVwZGF0ZXMKSmF2YSAxMSBoYXMgcmVjZWl2ZWQgMzcg" +
            "bWFpbnRlbmFuY2UgdXBkYXRlcyBpbiB0aGlzIHNuYXBzaG90OyB0aGUgbW9zdCByZWNlbnQgaXMgMTEuMC4zMi4xLCByZWxl" +
            "YXNlZCAyMDI2LTA4LTE5LgpUaGUgcmVmZXJlbmNlIGltcGxlbWVudGF0aW9uIHNvdXJjZXMgYXJlIG1haW50YWluZWQgYXQg" +
            "aHR0cHM6Ly9naXRodWIuY29tL29wZW5qZGsvamRrMTF1Lgo=",
        "java-17-api.md" to "LS0tCmlkOiBqYXZhLTE3LWFwaQp0aXRsZTogSmF2YSAxNyBBUEkgY2hhbmdlcwpjYXRlZ29yeTogYXBpCmFsaWFzZXM6IFtK" +
            "RVAgMzU2LCBKRVAgMzk4LCBKRVAgNDA3LCBKRVAgNDExLCBKRVAgNDEyLCBKRVAgNDE0XQpsYXN0X3Jldmlld2VkOiAyMDI2" +
            "LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAxNyBzaGlwcGVkIDYgQVBJIGNoYW5nZXMuIEVhY2ggc2VjdGlv" +
            "biBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBFbmhhbmNlZCBQc2V1ZG8tUmFuZG9tIE51bWJlciBHZW5lcmF0b3JzCkVuaGFu" +
            "Y2VkIFBzZXVkby1SYW5kb20gTnVtYmVyIEdlbmVyYXRvcnMgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDE3LCBzcGVjaWZp" +
            "ZWQgYnkgSkVQIDM1Ni4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMTcu" +
            "CgojIyBEZXByZWNhdGUgdGhlIEFwcGxldCBBUEkgZm9yIFJlbW92YWwKRGVwcmVjYXRlIHRoZSBBcHBsZXQgQVBJIGZvciBS" +
            "ZW1vdmFsIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSAxNywgc3BlY2lmaWVkIGJ5IEpFUCAzOTguCkl0IHNoaXBwZWQgYXMg" +
            "YSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDE3LgoKIyMgUmVtb3ZlIFJNSSBBY3RpdmF0aW9uClJl" +
            "bW92ZSBSTUkgQWN0aXZhdGlvbiBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMTcsIHNwZWNpZmllZCBieSBKRVAgNDA3LgpJ" +
            "dCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAxNy4KCiMjIERlcHJlY2F0ZSB0" +
            "aGUgU2VjdXJpdHkgTWFuYWdlciBmb3IgUmVtb3ZhbApEZXByZWNhdGUgdGhlIFNlY3VyaXR5IE1hbmFnZXIgZm9yIFJlbW92" +
            "YWwgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDE3LCBzcGVjaWZpZWQgYnkgSkVQIDQxMS4KSXQgc2hpcHBlZCBhcyBhIGZp" +
            "bmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMTcuCgojIyBGb3JlaWduIEZ1bmN0aW9uICYgTWVtb3J5IEFQ" +
            "SQpGb3JlaWduIEZ1bmN0aW9uICYgTWVtb3J5IEFQSSBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMTcsIHNwZWNpZmllZCBi" +
            "eSBKRVAgNDEyLgpKYXZhIDE3IHNoaXBzIGl0IGFzIGFuIGluY3ViYXRpbmcgbW9kdWxlOiB0aGUgQVBJIGxpdmVzIGluIHRo" +
            "ZSBqZGsuaW5jdWJhdG9yIG5hbWVzcGFjZSBhbmQgaXMgbm90IHlldCBwYXJ0IG9mIHRoZSBKYXZhIFNFIHN0YW5kYXJkLgoK" +
            "IyMgVmVjdG9yIEFQSQpWZWN0b3IgQVBJIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSAxNywgc3BlY2lmaWVkIGJ5IEpFUCA0" +
            "MTQuCkphdmEgMTcgc2hpcHMgaXQgYXMgYW4gaW5jdWJhdGluZyBtb2R1bGU6IHRoZSBBUEkgbGl2ZXMgaW4gdGhlIGpkay5p" +
            "bmN1YmF0b3IgbmFtZXNwYWNlIGFuZCBpcyBub3QgeWV0IHBhcnQgb2YgdGhlIEphdmEgU0Ugc3RhbmRhcmQuCgo=",
        "java-17-jvm.md" to "LS0tCmlkOiBqYXZhLTE3LWp2bQp0aXRsZTogSmF2YSAxNyBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlcwpjYXRlZ29yeToganZt" +
            "CmFsaWFzZXM6IFtKRVAgMzgyLCBKRVAgMzkxLCBKRVAgNDAzXQpsYXN0X3Jldmlld2VkOiAyMDI2LTEwLTEwCnN0YXR1czog" +
            "cHVibGlzaGVkCi0tLQoKSmF2YSAxNyBzaGlwcGVkIDMgSlZNIGFuZCBydW50aW1lIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBj" +
            "b3ZlcnMgb25lIG9mIHRoZW0uCgojIyBOZXcgbWFjT1MgUmVuZGVyaW5nIFBpcGVsaW5lCk5ldyBtYWNPUyBSZW5kZXJpbmcg" +
            "UGlwZWxpbmUgaXMgYSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlIGluIEphdmEgMTcsIHNwZWNpZmllZCBieSBKRVAgMzgyLgpJ" +
            "dCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAxNy4KCiMjIG1hY09TL0FBcmNo" +
            "NjQgUG9ydAptYWNPUy9BQXJjaDY0IFBvcnQgaXMgYSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlIGluIEphdmEgMTcsIHNwZWNp" +
            "ZmllZCBieSBKRVAgMzkxLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAx" +
            "Ny4KCiMjIFN0cm9uZ2x5IEVuY2Fwc3VsYXRlIEpESyBJbnRlcm5hbHMKU3Ryb25nbHkgRW5jYXBzdWxhdGUgSkRLIEludGVy" +
            "bmFscyBpcyBhIEpWTSBhbmQgcnVudGltZSBjaGFuZ2UgaW4gSmF2YSAxNywgc3BlY2lmaWVkIGJ5IEpFUCA0MDMuCkl0IHNo" +
            "aXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDE3LgoK",
        "java-17-language.md" to "LS0tCmlkOiBqYXZhLTE3LWxhbmd1YWdlCnRpdGxlOiBKYXZhIDE3IGxhbmd1YWdlIGNoYW5nZXMKY2F0ZWdvcnk6IGxhbmd1" +
            "YWdlCmFsaWFzZXM6IFtKRVAgMzA2LCBKRVAgNDA2LCBKRVAgNDA5XQpsYXN0X3Jldmlld2VkOiAyMDI2LTEwLTEwCnN0YXR1" +
            "czogcHVibGlzaGVkCi0tLQoKSmF2YSAxNyBzaGlwcGVkIDMgbGFuZ3VhZ2UgY2hhbmdlcy4gRWFjaCBzZWN0aW9uIGNvdmVy" +
            "cyBvbmUgb2YgdGhlbS4KCiMjIFJlc3RvcmUgQWx3YXlzLVN0cmljdCBGbG9hdGluZy1Qb2ludCBTZW1hbnRpY3MKUmVzdG9y" +
            "ZSBBbHdheXMtU3RyaWN0IEZsb2F0aW5nLVBvaW50IFNlbWFudGljcyBpcyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZhIDE3" +
            "LCBzcGVjaWZpZWQgYnkgSkVQIDMwNi4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGlu" +
            "IEphdmEgMTcuCgojIyBQYXR0ZXJuIE1hdGNoaW5nIGZvciBzd2l0Y2gKUGF0dGVybiBNYXRjaGluZyBmb3Igc3dpdGNoIGlz" +
            "IGEgbGFuZ3VhZ2UgY2hhbmdlIGluIEphdmEgMTcsIHNwZWNpZmllZCBieSBKRVAgNDA2LgpKYXZhIDE3IHNoaXBzIGl0IGFz" +
            "IGEgcHJldmlldyBmZWF0dXJlOiBpdCBtdXN0IGJlIGVuYWJsZWQgd2l0aCAtLWVuYWJsZS1wcmV2aWV3IGFuZCBjYW4gc3Rp" +
            "bGwgY2hhbmdlIGJlZm9yZSBpdCBiZWNvbWVzIGZpbmFsLgoKIyMgU2VhbGVkIENsYXNzZXMKU2VhbGVkIENsYXNzZXMgaXMg" +
            "YSBsYW5ndWFnZSBjaGFuZ2UgaW4gSmF2YSAxNywgc3BlY2lmaWVkIGJ5IEpFUCA0MDkuCkl0IHNoaXBwZWQgYXMgYSBmaW5h" +
            "bCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDE3LgoK",
        "java-17-tools.md" to "LS0tCmlkOiBqYXZhLTE3LXRvb2xzCnRpdGxlOiBKYXZhIDE3IHRvb2xpbmcgY2hhbmdlcwpjYXRlZ29yeTogdG9vbHMKYWxp" +
            "YXNlczogW0pFUCA0MTBdCmxhc3RfcmV2aWV3ZWQ6IDIwMjYtMTAtMTAKc3RhdHVzOiBwdWJsaXNoZWQKLS0tCgpKYXZhIDE3" +
            "IHNoaXBwZWQgMSB0b29saW5nIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBSZW1vdmUg" +
            "dGhlIEV4cGVyaW1lbnRhbCBBT1QgYW5kIEpJVCBDb21waWxlcgpSZW1vdmUgdGhlIEV4cGVyaW1lbnRhbCBBT1QgYW5kIEpJ" +
            "VCBDb21waWxlciBpcyBhIHRvb2xpbmcgY2hhbmdlIGluIEphdmEgMTcsIHNwZWNpZmllZCBieSBKRVAgNDEwLgpJdCBzaGlw" +
            "cGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAxNy4KCg==",
        "java-17.md" to "LS0tCmlkOiBqYXZhLTE3CnRpdGxlOiBKYXZhIDE3CmNhdGVnb3J5OiByZWxlYXNlCmFsaWFzZXM6IFtKREsgMTcsIEphdmEg" +
            "U0UgMTddCmxhc3RfcmV2aWV3ZWQ6IDIwMjYtMTAtMTAKc3RhdHVzOiBwdWJsaXNoZWQKLS0tCgpSZWxlYXNlIGZhY3RzIGZv" +
            "ciBKYXZhIDE3LCBhIExUUyByZWxlYXNlIG9mIHRoZSBKYXZhIHBsYXRmb3JtLgoKIyMgUmVsZWFzZSBmYWN0cwpKYXZhIDE3" +
            "IHJlYWNoZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAyMS0wOS0xNCBhcyBhIGxvbmctdGVybSBzdXBwb3J0IChMVFMp" +
            "IHJlbGVhc2UuCkl0cyBzdXBwb3J0IHdpbmRvdyBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMjctMTAuClRoZSByZWxlYXNl" +
            "IGlzIGRlZmluZWQgYnkgdW1icmVsbGEgSlNSIDM5Mi4KCiMjIENsYXNzIGZpbGVzIGFuZCBVbmljb2RlClRoZSBjb21waWxl" +
            "ciBpbiBKYXZhIDE3IGVtaXRzIGNsYXNzIGZpbGVzIHdpdGggbWFqb3IgdmVyc2lvbiA2MS4wLgpKYXZhIDE3IGJ1bmRsZXMg" +
            "VW5pY29kZSB2ZXJzaW9uIDEzLjAuMC4KCiMjIE1haW50ZW5hbmNlIHVwZGF0ZXMKSmF2YSAxNyBoYXMgcmVjZWl2ZWQgMjMg" +
            "bWFpbnRlbmFuY2UgdXBkYXRlcyBpbiB0aGlzIHNuYXBzaG90OyB0aGUgbW9zdCByZWNlbnQgaXMgMTcuMC4yMC4xLCByZWxl" +
            "YXNlZCAyMDI2LTA4LTA3LgpUaGUgcmVmZXJlbmNlIGltcGxlbWVudGF0aW9uIHNvdXJjZXMgYXJlIG1haW50YWluZWQgYXQg" +
            "aHR0cHM6Ly9naXRodWIuY29tL29wZW5qZGsvamRrMTd1Lgo=",
        "java-21-api.md" to "LS0tCmlkOiBqYXZhLTIxLWFwaQp0aXRsZTogSmF2YSAyMSBBUEkgY2hhbmdlcwpjYXRlZ29yeTogYXBpCmFsaWFzZXM6IFtK" +
            "RVAgNDMxLCBKRVAgNDQyLCBKRVAgNDQ0LCBKRVAgNDQ2LCBKRVAgNDQ4LCBKRVAgNDUyLCBKRVAgNDUzXQpsYXN0X3Jldmll" +
            "d2VkOiAyMDI2LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAyMSBzaGlwcGVkIDcgQVBJIGNoYW5nZXMuIEVh" +
            "Y2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBTZXF1ZW5jZWQgQ29sbGVjdGlvbnMKU2VxdWVuY2VkIENvbGxl" +
            "Y3Rpb25zIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSAyMSwgc3BlY2lmaWVkIGJ5IEpFUCA0MzEuCkl0IHNoaXBwZWQgYXMg" +
            "YSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDIxLgoKIyMgRm9yZWlnbiBGdW5jdGlvbiAmIE1lbW9y" +
            "eSBBUEkKRm9yZWlnbiBGdW5jdGlvbiAmIE1lbW9yeSBBUEkgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDIxLCBzcGVjaWZp" +
            "ZWQgYnkgSkVQIDQ0Mi4KSmF2YSAyMSBzaGlwcyBpdCBhcyBhIHByZXZpZXcgZmVhdHVyZTogaXQgbXVzdCBiZSBlbmFibGVk" +
            "IHdpdGggLS1lbmFibGUtcHJldmlldyBhbmQgY2FuIHN0aWxsIGNoYW5nZSBiZWZvcmUgaXQgYmVjb21lcyBmaW5hbC4KCiMj" +
            "IFZpcnR1YWwgVGhyZWFkcwpWaXJ0dWFsIFRocmVhZHMgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDIxLCBzcGVjaWZpZWQg" +
            "YnkgSkVQIDQ0NC4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjEuCgoj" +
            "IyBTY29wZWQgVmFsdWVzClNjb3BlZCBWYWx1ZXMgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDIxLCBzcGVjaWZpZWQgYnkg" +
            "SkVQIDQ0Ni4KSmF2YSAyMSBzaGlwcyBpdCBhcyBhIHByZXZpZXcgZmVhdHVyZTogaXQgbXVzdCBiZSBlbmFibGVkIHdpdGgg" +
            "LS1lbmFibGUtcHJldmlldyBhbmQgY2FuIHN0aWxsIGNoYW5nZSBiZWZvcmUgaXQgYmVjb21lcyBmaW5hbC4KCiMjIFZlY3Rv" +
            "ciBBUEkKVmVjdG9yIEFQSSBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMjEsIHNwZWNpZmllZCBieSBKRVAgNDQ4LgpKYXZh" +
            "IDIxIHNoaXBzIGl0IGFzIGFuIGluY3ViYXRpbmcgbW9kdWxlOiB0aGUgQVBJIGxpdmVzIGluIHRoZSBqZGsuaW5jdWJhdG9y" +
            "IG5hbWVzcGFjZSBhbmQgaXMgbm90IHlldCBwYXJ0IG9mIHRoZSBKYXZhIFNFIHN0YW5kYXJkLgoKIyMgS2V5IEVuY2Fwc3Vs" +
            "YXRpb24gTWVjaGFuaXNtIEFQSQpLZXkgRW5jYXBzdWxhdGlvbiBNZWNoYW5pc20gQVBJIGlzIGFuIEFQSSBjaGFuZ2UgaW4g" +
            "SmF2YSAyMSwgc3BlY2lmaWVkIGJ5IEpFUCA0NTIuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNo" +
            "YW5nZSBpbiBKYXZhIDIxLgoKIyMgU3RydWN0dXJlZCBDb25jdXJyZW5jeQpTdHJ1Y3R1cmVkIENvbmN1cnJlbmN5IGlzIGFu" +
            "IEFQSSBjaGFuZ2UgaW4gSmF2YSAyMSwgc3BlY2lmaWVkIGJ5IEpFUCA0NTMuCkphdmEgMjEgc2hpcHMgaXQgYXMgYSBwcmV2" +
            "aWV3IGZlYXR1cmU6IGl0IG11c3QgYmUgZW5hYmxlZCB3aXRoIC0tZW5hYmxlLXByZXZpZXcgYW5kIGNhbiBzdGlsbCBjaGFu" +
            "Z2UgYmVmb3JlIGl0IGJlY29tZXMgZmluYWwuCgo=",
        "java-21-jvm.md" to "LS0tCmlkOiBqYXZhLTIxLWp2bQp0aXRsZTogSmF2YSAyMSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlcwpjYXRlZ29yeToganZt" +
            "CmFsaWFzZXM6IFtKRVAgNDM5LCBKRVAgNDQ5LCBKRVAgNDUxXQpsYXN0X3Jldmlld2VkOiAyMDI2LTEwLTEwCnN0YXR1czog" +
            "cHVibGlzaGVkCi0tLQoKSmF2YSAyMSBzaGlwcGVkIDMgSlZNIGFuZCBydW50aW1lIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBj" +
            "b3ZlcnMgb25lIG9mIHRoZW0uCgojIyBHZW5lcmF0aW9uYWwgWkdDCkdlbmVyYXRpb25hbCBaR0MgaXMgYSBKVk0gYW5kIHJ1" +
            "bnRpbWUgY2hhbmdlIGluIEphdmEgMjEsIHNwZWNpZmllZCBieSBKRVAgNDM5LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1" +
            "bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAyMS4KCiMjIERlcHJlY2F0ZSB0aGUgV2luZG93cyAzMi1iaXQgeDg2IFBv" +
            "cnQgZm9yIFJlbW92YWwKRGVwcmVjYXRlIHRoZSBXaW5kb3dzIDMyLWJpdCB4ODYgUG9ydCBmb3IgUmVtb3ZhbCBpcyBhIEpW" +
            "TSBhbmQgcnVudGltZSBjaGFuZ2UgaW4gSmF2YSAyMSwgc3BlY2lmaWVkIGJ5IEpFUCA0NDkuCkl0IHNoaXBwZWQgYXMgYSBm" +
            "aW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDIxLgoKIyMgUHJlcGFyZSB0byBEaXNhbGxvdyB0aGUgRHlu" +
            "YW1pYyBMb2FkaW5nIG9mIEFnZW50cwpQcmVwYXJlIHRvIERpc2FsbG93IHRoZSBEeW5hbWljIExvYWRpbmcgb2YgQWdlbnRz" +
            "IGlzIGEgSlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDIxLCBzcGVjaWZpZWQgYnkgSkVQIDQ1MS4KSXQgc2hpcHBl" +
            "ZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjEuCgo=",
        "java-21-language.md" to "LS0tCmlkOiBqYXZhLTIxLWxhbmd1YWdlCnRpdGxlOiBKYXZhIDIxIGxhbmd1YWdlIGNoYW5nZXMKY2F0ZWdvcnk6IGxhbmd1" +
            "YWdlCmFsaWFzZXM6IFtKRVAgNDMwLCBKRVAgNDQwLCBKRVAgNDQxLCBKRVAgNDQzLCBKRVAgNDQ1XQpsYXN0X3Jldmlld2Vk" +
            "OiAyMDI2LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAyMSBzaGlwcGVkIDUgbGFuZ3VhZ2UgY2hhbmdlcy4g" +
            "RWFjaCBzZWN0aW9uIGNvdmVycyBvbmUgb2YgdGhlbS4KCiMjIFN0cmluZyBUZW1wbGF0ZXMKU3RyaW5nIFRlbXBsYXRlcyBp" +
            "cyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZhIDIxLCBzcGVjaWZpZWQgYnkgSkVQIDQzMC4KSmF2YSAyMSBzaGlwcyBpdCBh" +
            "cyBhIHByZXZpZXcgZmVhdHVyZTogaXQgbXVzdCBiZSBlbmFibGVkIHdpdGggLS1lbmFibGUtcHJldmlldyBhbmQgY2FuIHN0" +
            "aWxsIGNoYW5nZSBiZWZvcmUgaXQgYmVjb21lcyBmaW5hbC4KCiMjIFJlY29yZCBQYXR0ZXJucwpSZWNvcmQgUGF0dGVybnMg" +
            "aXMgYSBsYW5ndWFnZSBjaGFuZ2UgaW4gSmF2YSAyMSwgc3BlY2lmaWVkIGJ5IEpFUCA0NDAuCkl0IHNoaXBwZWQgYXMgYSBm" +
            "aW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDIxLgoKIyMgUGF0dGVybiBNYXRjaGluZyBmb3Igc3dpdGNo" +
            "ClBhdHRlcm4gTWF0Y2hpbmcgZm9yIHN3aXRjaCBpcyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZhIDIxLCBzcGVjaWZpZWQg" +
            "YnkgSkVQIDQ0MS4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjEuCgoj" +
            "IyBVbm5hbWVkIFBhdHRlcm5zIGFuZCBWYXJpYWJsZXMKVW5uYW1lZCBQYXR0ZXJucyBhbmQgVmFyaWFibGVzIGlzIGEgbGFu" +
            "Z3VhZ2UgY2hhbmdlIGluIEphdmEgMjEsIHNwZWNpZmllZCBieSBKRVAgNDQzLgpKYXZhIDIxIHNoaXBzIGl0IGFzIGEgcHJl" +
            "dmlldyBmZWF0dXJlOiBpdCBtdXN0IGJlIGVuYWJsZWQgd2l0aCAtLWVuYWJsZS1wcmV2aWV3IGFuZCBjYW4gc3RpbGwgY2hh" +
            "bmdlIGJlZm9yZSBpdCBiZWNvbWVzIGZpbmFsLgoKIyMgVW5uYW1lZCBDbGFzc2VzIGFuZCBJbnN0YW5jZSBNYWluIE1ldGhv" +
            "ZHMKVW5uYW1lZCBDbGFzc2VzIGFuZCBJbnN0YW5jZSBNYWluIE1ldGhvZHMgaXMgYSBsYW5ndWFnZSBjaGFuZ2UgaW4gSmF2" +
            "YSAyMSwgc3BlY2lmaWVkIGJ5IEpFUCA0NDUuCkphdmEgMjEgc2hpcHMgaXQgYXMgYSBwcmV2aWV3IGZlYXR1cmU6IGl0IG11" +
            "c3QgYmUgZW5hYmxlZCB3aXRoIC0tZW5hYmxlLXByZXZpZXcgYW5kIGNhbiBzdGlsbCBjaGFuZ2UgYmVmb3JlIGl0IGJlY29t" +
            "ZXMgZmluYWwuCgo=",
        "java-21.md" to "LS0tCmlkOiBqYXZhLTIxCnRpdGxlOiBKYXZhIDIxCmNhdGVnb3J5OiByZWxlYXNlCmFsaWFzZXM6IFtKREsgMjEsIEphdmEg" +
            "U0UgMjFdCmxhc3RfcmV2aWV3ZWQ6IDIwMjYtMTAtMTAKc3RhdHVzOiBwdWJsaXNoZWQKLS0tCgpSZWxlYXNlIGZhY3RzIGZv" +
            "ciBKYXZhIDIxLCBhIExUUyByZWxlYXNlIG9mIHRoZSBKYXZhIHBsYXRmb3JtLgoKIyMgUmVsZWFzZSBmYWN0cwpKYXZhIDIx" +
            "IHJlYWNoZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAyMy0wOS0xOSBhcyBhIGxvbmctdGVybSBzdXBwb3J0IChMVFMp" +
            "IHJlbGVhc2UuCkl0cyBzdXBwb3J0IHdpbmRvdyBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMjktMTIuClRoZSByZWxlYXNl" +
            "IGlzIGRlZmluZWQgYnkgdW1icmVsbGEgSlNSIDM5Ni4KCiMjIENsYXNzIGZpbGVzIGFuZCBVbmljb2RlClRoZSBjb21waWxl" +
            "ciBpbiBKYXZhIDIxIGVtaXRzIGNsYXNzIGZpbGVzIHdpdGggbWFqb3IgdmVyc2lvbiA2NS4wLgpKYXZhIDIxIGJ1bmRsZXMg" +
            "VW5pY29kZSB2ZXJzaW9uIDE1LjAuMC4KCiMjIE1haW50ZW5hbmNlIHVwZGF0ZXMKSmF2YSAyMSBoYXMgcmVjZWl2ZWQgMTMg" +
            "bWFpbnRlbmFuY2UgdXBkYXRlcyBpbiB0aGlzIHNuYXBzaG90OyB0aGUgbW9zdCByZWNlbnQgaXMgMjEuMC4xMi4xLCByZWxl" +
            "YXNlZCAyMDI2LTA4LTA3LgpUaGUgcmVmZXJlbmNlIGltcGxlbWVudGF0aW9uIHNvdXJjZXMgYXJlIG1haW50YWluZWQgYXQg" +
            "aHR0cHM6Ly9naXRodWIuY29tL29wZW5qZGsvamRrMjF1Lgo=",
        "java-25-api.md" to "LS0tCmlkOiBqYXZhLTI1LWFwaQp0aXRsZTogSmF2YSAyNSBBUEkgY2hhbmdlcwpjYXRlZ29yeTogYXBpCmFsaWFzZXM6IFtK" +
            "RVAgNDcwLCBKRVAgNTAyLCBKRVAgNTA1LCBKRVAgNTA2LCBKRVAgNTA4LCBKRVAgNTEwXQpsYXN0X3Jldmlld2VkOiAyMDI2" +
            "LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAyNSBzaGlwcGVkIDYgQVBJIGNoYW5nZXMuIEVhY2ggc2VjdGlv" +
            "biBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBQRU0gRW5jb2RpbmdzIG9mIENyeXB0b2dyYXBoaWMgT2JqZWN0cwpQRU0gRW5j" +
            "b2RpbmdzIG9mIENyeXB0b2dyYXBoaWMgT2JqZWN0cyBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMjUsIHNwZWNpZmllZCBi" +
            "eSBKRVAgNDcwLgpKYXZhIDI1IHNoaXBzIGl0IGFzIGEgcHJldmlldyBmZWF0dXJlOiBpdCBtdXN0IGJlIGVuYWJsZWQgd2l0" +
            "aCAtLWVuYWJsZS1wcmV2aWV3IGFuZCBjYW4gc3RpbGwgY2hhbmdlIGJlZm9yZSBpdCBiZWNvbWVzIGZpbmFsLgoKIyMgU3Rh" +
            "YmxlIFZhbHVlcwpTdGFibGUgVmFsdWVzIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSAyNSwgc3BlY2lmaWVkIGJ5IEpFUCA1" +
            "MDIuCkphdmEgMjUgc2hpcHMgaXQgYXMgYSBwcmV2aWV3IGZlYXR1cmU6IGl0IG11c3QgYmUgZW5hYmxlZCB3aXRoIC0tZW5h" +
            "YmxlLXByZXZpZXcgYW5kIGNhbiBzdGlsbCBjaGFuZ2UgYmVmb3JlIGl0IGJlY29tZXMgZmluYWwuCgojIyBTdHJ1Y3R1cmVk" +
            "IENvbmN1cnJlbmN5ClN0cnVjdHVyZWQgQ29uY3VycmVuY3kgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDI1LCBzcGVjaWZp" +
            "ZWQgYnkgSkVQIDUwNS4KSmF2YSAyNSBzaGlwcyBpdCBhcyBhIHByZXZpZXcgZmVhdHVyZTogaXQgbXVzdCBiZSBlbmFibGVk" +
            "IHdpdGggLS1lbmFibGUtcHJldmlldyBhbmQgY2FuIHN0aWxsIGNoYW5nZSBiZWZvcmUgaXQgYmVjb21lcyBmaW5hbC4KCiMj" +
            "IFNjb3BlZCBWYWx1ZXMKU2NvcGVkIFZhbHVlcyBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMjUsIHNwZWNpZmllZCBieSBK" +
            "RVAgNTA2LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAyNS4KCiMjIFZl" +
            "Y3RvciBBUEkKVmVjdG9yIEFQSSBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgMjUsIHNwZWNpZmllZCBieSBKRVAgNTA4LgpK" +
            "YXZhIDI1IHNoaXBzIGl0IGFzIGFuIGluY3ViYXRpbmcgbW9kdWxlOiB0aGUgQVBJIGxpdmVzIGluIHRoZSBqZGsuaW5jdWJh" +
            "dG9yIG5hbWVzcGFjZSBhbmQgaXMgbm90IHlldCBwYXJ0IG9mIHRoZSBKYXZhIFNFIHN0YW5kYXJkLgoKIyMgS2V5IERlcml2" +
            "YXRpb24gRnVuY3Rpb24gQVBJCktleSBEZXJpdmF0aW9uIEZ1bmN0aW9uIEFQSSBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEg" +
            "MjUsIHNwZWNpZmllZCBieSBKRVAgNTEwLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2Ug" +
            "aW4gSmF2YSAyNS4KCg==",
        "java-25-jvm.md" to "LS0tCmlkOiBqYXZhLTI1LWp2bQp0aXRsZTogSmF2YSAyNSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlcwpjYXRlZ29yeToganZt" +
            "CmFsaWFzZXM6IFtKRVAgNTAzLCBKRVAgNTE0LCBKRVAgNTE1LCBKRVAgNTE4LCBKRVAgNTE5LCBKRVAgNTIwLCBKRVAgNTIx" +
            "XQpsYXN0X3Jldmlld2VkOiAyMDI2LTEwLTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAyNSBzaGlwcGVkIDcgSlZN" +
            "IGFuZCBydW50aW1lIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9mIHRoZW0uCgojIyBSZW1vdmUgdGhlIDMy" +
            "LWJpdCB4ODYgUG9ydApSZW1vdmUgdGhlIDMyLWJpdCB4ODYgUG9ydCBpcyBhIEpWTSBhbmQgcnVudGltZSBjaGFuZ2UgaW4g" +
            "SmF2YSAyNSwgc3BlY2lmaWVkIGJ5IEpFUCA1MDMuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNo" +
            "YW5nZSBpbiBKYXZhIDI1LgoKIyMgQWhlYWQtb2YtVGltZSBDb21tYW5kLUxpbmUgRXJnb25vbWljcwpBaGVhZC1vZi1UaW1l" +
            "IENvbW1hbmQtTGluZSBFcmdvbm9taWNzIGlzIGEgSlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDI1LCBzcGVjaWZp" +
            "ZWQgYnkgSkVQIDUxNC4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjUu" +
            "CgojIyBBaGVhZC1vZi1UaW1lIE1ldGhvZCBQcm9maWxpbmcKQWhlYWQtb2YtVGltZSBNZXRob2QgUHJvZmlsaW5nIGlzIGEg" +
            "SlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDI1LCBzcGVjaWZpZWQgYnkgSkVQIDUxNS4KSXQgc2hpcHBlZCBhcyBh" +
            "IGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjUuCgojIyBKRlIgQ29vcGVyYXRpdmUgU2FtcGxpbmcK" +
            "SkZSIENvb3BlcmF0aXZlIFNhbXBsaW5nIGlzIGEgSlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDI1LCBzcGVjaWZp" +
            "ZWQgYnkgSkVQIDUxOC4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjUu" +
            "CgojIyBDb21wYWN0IE9iamVjdCBIZWFkZXJzCkNvbXBhY3QgT2JqZWN0IEhlYWRlcnMgaXMgYSBKVk0gYW5kIHJ1bnRpbWUg" +
            "Y2hhbmdlIGluIEphdmEgMjUsIHNwZWNpZmllZCBieSBKRVAgNTE5LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1" +
            "cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAyNS4KCiMjIEpGUiBNZXRob2QgVGltaW5nICYgVHJhY2luZwpKRlIgTWV0aG9kIFRp" +
            "bWluZyAmIFRyYWNpbmcgaXMgYSBKVk0gYW5kIHJ1bnRpbWUgY2hhbmdlIGluIEphdmEgMjUsIHNwZWNpZmllZCBieSBKRVAg" +
            "NTIwLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSAyNS4KCiMjIEdlbmVy" +
            "YXRpb25hbCBTaGVuYW5kb2FoCkdlbmVyYXRpb25hbCBTaGVuYW5kb2FoIGlzIGEgSlZNIGFuZCBydW50aW1lIGNoYW5nZSBp" +
            "biBKYXZhIDI1LCBzcGVjaWZpZWQgYnkgSkVQIDUyMS4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQg" +
            "Y2hhbmdlIGluIEphdmEgMjUuCgo=",
        "java-25-language.md" to "LS0tCmlkOiBqYXZhLTI1LWxhbmd1YWdlCnRpdGxlOiBKYXZhIDI1IGxhbmd1YWdlIGNoYW5nZXMKY2F0ZWdvcnk6IGxhbmd1" +
            "YWdlCmFsaWFzZXM6IFtKRVAgNTA3LCBKRVAgNTExLCBKRVAgNTEyLCBKRVAgNTEzXQpsYXN0X3Jldmlld2VkOiAyMDI2LTEw" +
            "LTEwCnN0YXR1czogcHVibGlzaGVkCi0tLQoKSmF2YSAyNSBzaGlwcGVkIDQgbGFuZ3VhZ2UgY2hhbmdlcy4gRWFjaCBzZWN0" +
            "aW9uIGNvdmVycyBvbmUgb2YgdGhlbS4KCiMjIFByaW1pdGl2ZSBUeXBlcyBpbiBQYXR0ZXJucywgaW5zdGFuY2VvZiwgYW5k" +
            "IHN3aXRjaApQcmltaXRpdmUgVHlwZXMgaW4gUGF0dGVybnMsIGluc3RhbmNlb2YsIGFuZCBzd2l0Y2ggaXMgYSBsYW5ndWFn" +
            "ZSBjaGFuZ2UgaW4gSmF2YSAyNSwgc3BlY2lmaWVkIGJ5IEpFUCA1MDcuCkphdmEgMjUgc2hpcHMgaXQgYXMgYSBwcmV2aWV3" +
            "IGZlYXR1cmU6IGl0IG11c3QgYmUgZW5hYmxlZCB3aXRoIC0tZW5hYmxlLXByZXZpZXcgYW5kIGNhbiBzdGlsbCBjaGFuZ2Ug" +
            "YmVmb3JlIGl0IGJlY29tZXMgZmluYWwuCgojIyBNb2R1bGUgSW1wb3J0IERlY2xhcmF0aW9ucwpNb2R1bGUgSW1wb3J0IERl" +
            "Y2xhcmF0aW9ucyBpcyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZhIDI1LCBzcGVjaWZpZWQgYnkgSkVQIDUxMS4KSXQgc2hp" +
            "cHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgMjUuCgojIyBDb21wYWN0IFNvdXJjZSBG" +
            "aWxlcyBhbmQgSW5zdGFuY2UgTWFpbiBNZXRob2RzCkNvbXBhY3QgU291cmNlIEZpbGVzIGFuZCBJbnN0YW5jZSBNYWluIE1l" +
            "dGhvZHMgaXMgYSBsYW5ndWFnZSBjaGFuZ2UgaW4gSmF2YSAyNSwgc3BlY2lmaWVkIGJ5IEpFUCA1MTIuCkl0IHNoaXBwZWQg" +
            "YXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDI1LgoKIyMgRmxleGlibGUgQ29uc3RydWN0b3Ig" +
            "Qm9kaWVzCkZsZXhpYmxlIENvbnN0cnVjdG9yIEJvZGllcyBpcyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZhIDI1LCBzcGVj" +
            "aWZpZWQgYnkgSkVQIDUxMy4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEg" +
            "MjUuCgo=",
        "java-25.md" to "LS0tCmlkOiBqYXZhLTI1CnRpdGxlOiBKYXZhIDI1CmNhdGVnb3J5OiByZWxlYXNlCmFsaWFzZXM6IFtKREsgMjUsIEphdmEg" +
            "U0UgMjVdCmxhc3RfcmV2aWV3ZWQ6IDIwMjYtMTAtMTAKc3RhdHVzOiBwdWJsaXNoZWQKLS0tCgpSZWxlYXNlIGZhY3RzIGZv" +
            "ciBKYXZhIDI1LCBhIExUUyByZWxlYXNlIG9mIHRoZSBKYXZhIHBsYXRmb3JtLgoKIyMgUmVsZWFzZSBmYWN0cwpKYXZhIDI1" +
            "IHJlYWNoZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAyNS0wOS0xNiBhcyBhIGxvbmctdGVybSBzdXBwb3J0IChMVFMp" +
            "IHJlbGVhc2UuCkl0cyBzdXBwb3J0IHdpbmRvdyBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMzAtMDkuClRoZSByZWxlYXNl" +
            "IGlzIGRlZmluZWQgYnkgdW1icmVsbGEgSlNSIDQwMC4KCiMjIENsYXNzIGZpbGVzIGFuZCBVbmljb2RlClRoZSBjb21waWxl" +
            "ciBpbiBKYXZhIDI1IGVtaXRzIGNsYXNzIGZpbGVzIHdpdGggbWFqb3IgdmVyc2lvbiA2OS4wLgpKYXZhIDI1IGJ1bmRsZXMg" +
            "VW5pY29kZSB2ZXJzaW9uIDE2LjAuMC4KCiMjIE1haW50ZW5hbmNlIHVwZGF0ZXMKSmF2YSAyNSBoYXMgcmVjZWl2ZWQgNSBt" +
            "YWludGVuYW5jZSB1cGRhdGVzIGluIHRoaXMgc25hcHNob3Q7IHRoZSBtb3N0IHJlY2VudCBpcyAyNS4wLjQuMSwgcmVsZWFz" +
            "ZWQgMjAyNi0wOC0wNy4KVGhlIHJlZmVyZW5jZSBpbXBsZW1lbnRhdGlvbiBzb3VyY2VzIGFyZSBtYWludGFpbmVkIGF0IGh0" +
            "dHBzOi8vZ2l0aHViLmNvbS9vcGVuamRrL2pkazI1dS4K",
        "java-8-api.md" to "LS0tCmlkOiBqYXZhLTgtYXBpCnRpdGxlOiBKYXZhIDggQVBJIGNoYW5nZXMKY2F0ZWdvcnk6IGFwaQphbGlhc2VzOiBbSkVQ" +
            "IDEwNywgSkVQIDEwOSwgSkVQIDExNCwgSkVQIDExNSwgSkVQIDEyMSwgSkVQIDEyMywgSkVQIDEyNCwgSkVQIDEzMF0KbGFz" +
            "dF9yZXZpZXdlZDogMjAyNi0xMC0xMApzdGF0dXM6IHB1Ymxpc2hlZAotLS0KCkphdmEgOCBzaGlwcGVkIDExIEFQSSBjaGFu" +
            "Z2VzLiBFYWNoIHNlY3Rpb24gY292ZXJzIG9uZSBvZiB0aGVtLgoKIyMgU3RyZWFtcyAoamF2YS51dGlsLnN0cmVhbSkKU3Ry" +
            "ZWFtcyAoamF2YS51dGlsLnN0cmVhbSkgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTA3" +
            "LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoKIyMgTGFtYmRhIEFQ" +
            "SXMgKGphdmEudXRpbC5mdW5jdGlvbikKTGFtYmRhIEFQSXMgKGphdmEudXRpbC5mdW5jdGlvbikgaXMgYW4gQVBJIGNoYW5n" +
            "ZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTA5LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRl" +
            "ZCBjaGFuZ2UgaW4gSmF2YSA4LgoKIyMgVExTIFNlcnZlciBOYW1lIEluZGljYXRpb24gKFNOSSkgRXh0ZW5zaW9uClRMUyBT" +
            "ZXJ2ZXIgTmFtZSBJbmRpY2F0aW9uIChTTkkpIEV4dGVuc2lvbiBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgOCwgc3BlY2lm" +
            "aWVkIGJ5IEpFUCAxMTQuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDgu" +
            "CgojIyBBRUFEIENpcGhlclN1aXRlcwpBRUFEIENpcGhlclN1aXRlcyBpcyBhbiBBUEkgY2hhbmdlIGluIEphdmEgOCwgc3Bl" +
            "Y2lmaWVkIGJ5IEpFUCAxMTUuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZh" +
            "IDguCgojIyBTdHJvbmdlciBBbGdvcml0aG1zIGZvciBQYXNzd29yZC1CYXNlZCBFbmNyeXB0aW9uClN0cm9uZ2VyIEFsZ29y" +
            "aXRobXMgZm9yIFBhc3N3b3JkLUJhc2VkIEVuY3J5cHRpb24gaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmll" +
            "ZCBieSBKRVAgMTIxLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoK" +
            "IyMgQ29uZmlndXJhYmxlIFNlY3VyZSBSYW5kb20tTnVtYmVyIEdlbmVyYXRpb24KQ29uZmlndXJhYmxlIFNlY3VyZSBSYW5k" +
            "b20tTnVtYmVyIEdlbmVyYXRpb24gaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTIzLgpJ" +
            "dCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoKIyMgRW5oYW5jZSB0aGUg" +
            "Q2VydGlmaWNhdGUgUmV2b2NhdGlvbi1DaGVja2luZyBBUEkKRW5oYW5jZSB0aGUgQ2VydGlmaWNhdGUgUmV2b2NhdGlvbi1D" +
            "aGVja2luZyBBUEkgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTI0LgpJdCBzaGlwcGVk" +
            "IGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoKIyMgU0hBLTIyNCBNZXNzYWdlIERpZ2Vz" +
            "dHMKU0hBLTIyNCBNZXNzYWdlIERpZ2VzdHMgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAg" +
            "MTMwLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoKIyMgQ29uY3Vy" +
            "cmVuY3kgVXBkYXRlcwpDb25jdXJyZW5jeSBVcGRhdGVzIGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSA4LCBzcGVjaWZpZWQg" +
            "YnkgSkVQIDE1NS4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgOC4KCiMj" +
            "IE92ZXJoYXVsIEpLUy1KQ0VLUy1QS0NTMTIgS2V5c3RvcmVzCk92ZXJoYXVsIEpLUy1KQ0VLUy1QS0NTMTIgS2V5c3RvcmVz" +
            "IGlzIGFuIEFQSSBjaGFuZ2UgaW4gSmF2YSA4LCBzcGVjaWZpZWQgYnkgSkVQIDE2Ni4KSXQgc2hpcHBlZCBhcyBhIGZpbmFs" +
            "LCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdlIGluIEphdmEgOC4KCiMjIERhdGUgVGltZSAoamF2YS50aW1lKQpEYXRlIFRpbWUg" +
            "KGphdmEudGltZSkgaXMgYW4gQVBJIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKU1IgMzEwIGFuZCBKRVAgMTUw" +
            "LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoK",
        "java-8-jvm.md" to "LS0tCmlkOiBqYXZhLTgtanZtCnRpdGxlOiBKYXZhIDggSlZNIGFuZCBydW50aW1lIGNoYW5nZXMKY2F0ZWdvcnk6IGp2bQph" +
            "bGlhc2VzOiBbSkVQIDEyMiwgSkVQIDE2NF0KbGFzdF9yZXZpZXdlZDogMjAyNi0xMC0xMApzdGF0dXM6IHB1Ymxpc2hlZAot" +
            "LS0KCkphdmEgOCBzaGlwcGVkIDIgSlZNIGFuZCBydW50aW1lIGNoYW5nZXMuIEVhY2ggc2VjdGlvbiBjb3ZlcnMgb25lIG9m" +
            "IHRoZW0uCgojIyBSZW1vdmUgUGVybWFuZW50IEdlbmVyYXRpb24KUmVtb3ZlIFBlcm1hbmVudCBHZW5lcmF0aW9uIGlzIGEg" +
            "SlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTIyLgpJdCBzaGlwcGVkIGFzIGEg" +
            "ZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoKIyMgTGV2ZXJhZ2UgQ1BVIEluc3RydWN0aW9ucyBm" +
            "b3IgQUVTIENyeXB0b2dyYXBoeQpMZXZlcmFnZSBDUFUgSW5zdHJ1Y3Rpb25zIGZvciBBRVMgQ3J5cHRvZ3JhcGh5IGlzIGEg" +
            "SlZNIGFuZCBydW50aW1lIGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTY0LgpJdCBzaGlwcGVkIGFzIGEg" +
            "ZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoK",
        "java-8-language.md" to "LS0tCmlkOiBqYXZhLTgtbGFuZ3VhZ2UKdGl0bGU6IEphdmEgOCBsYW5ndWFnZSBjaGFuZ2VzCmNhdGVnb3J5OiBsYW5ndWFn" +
            "ZQphbGlhc2VzOiBbSkVQIDEwNCwgSkVQIDEyMF0KbGFzdF9yZXZpZXdlZDogMjAyNi0xMC0xMApzdGF0dXM6IHB1Ymxpc2hl" +
            "ZAotLS0KCkphdmEgOCBzaGlwcGVkIDUgbGFuZ3VhZ2UgY2hhbmdlcy4gRWFjaCBzZWN0aW9uIGNvdmVycyBvbmUgb2YgdGhl" +
            "bS4KCiMjIExhbWJkYSBFeHByZXNzaW9ucwpMYW1iZGEgRXhwcmVzc2lvbnMgaXMgYSBsYW5ndWFnZSBjaGFuZ2UgaW4gSmF2" +
            "YSA4LCBzcGVjaWZpZWQgYnkgSlNSIDMzNS4KSXQgc2hpcHBlZCBhcyBhIGZpbmFsLCBmdWxseSBzdXBwb3J0ZWQgY2hhbmdl" +
            "IGluIEphdmEgOC4KCiMjIERlZmF1bHQgTWV0aG9kcyBpbiBJbnRlcmZhY2VzCkRlZmF1bHQgTWV0aG9kcyBpbiBJbnRlcmZh" +
            "Y2VzIGlzIGEgbGFuZ3VhZ2UgY2hhbmdlIGluIEphdmEgOCwgc3BlY2lmaWVkIGJ5IEpTUiAzMzUuCkl0IHNoaXBwZWQgYXMg" +
            "YSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDguCgojIyBFZmZlY3RpdmVseSBGaW5hbCBWYXJpYWJs" +
            "ZXMKRWZmZWN0aXZlbHkgRmluYWwgVmFyaWFibGVzIGlzIGEgbGFuZ3VhZ2UgY2hhbmdlIGluIEphdmEgOCwgc3BlY2lmaWVk" +
            "IGJ5IEpTUiAzMzUuCkl0IHNoaXBwZWQgYXMgYSBmaW5hbCwgZnVsbHkgc3VwcG9ydGVkIGNoYW5nZSBpbiBKYXZhIDguCgoj" +
            "IyBUeXBlIFVzZSBBbm5vdGF0aW9ucwpUeXBlIFVzZSBBbm5vdGF0aW9ucyBpcyBhIGxhbmd1YWdlIGNoYW5nZSBpbiBKYXZh" +
            "IDgsIHNwZWNpZmllZCBieSBKRVAgMTA0LgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1cHBvcnRlZCBjaGFuZ2Ug" +
            "aW4gSmF2YSA4LgoKIyMgUmVwZWF0aW5nIEFubm90YXRpb25zClJlcGVhdGluZyBBbm5vdGF0aW9ucyBpcyBhIGxhbmd1YWdl" +
            "IGNoYW5nZSBpbiBKYXZhIDgsIHNwZWNpZmllZCBieSBKRVAgMTIwLgpJdCBzaGlwcGVkIGFzIGEgZmluYWwsIGZ1bGx5IHN1" +
            "cHBvcnRlZCBjaGFuZ2UgaW4gSmF2YSA4LgoK",
        "java-8.md" to "LS0tCmlkOiBqYXZhLTgKdGl0bGU6IEphdmEgOApjYXRlZ29yeTogcmVsZWFzZQphbGlhc2VzOiBbSkRLIDgsIEphdmEgU0Ug" +
            "OF0KbGFzdF9yZXZpZXdlZDogMjAyNi0xMC0xMApzdGF0dXM6IHB1Ymxpc2hlZAotLS0KClJlbGVhc2UgZmFjdHMgZm9yIEph" +
            "dmEgOCwgYSBMVFMgcmVsZWFzZSBvZiB0aGUgSmF2YSBwbGF0Zm9ybS4KCiMjIFJlbGVhc2UgZmFjdHMKSmF2YSA4IHJlYWNo" +
            "ZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAxNC0wMy0xOCBhcyBhIGxvbmctdGVybSBzdXBwb3J0IChMVFMpIHJlbGVh" +
            "c2UuCkl0cyBzdXBwb3J0IHdpbmRvdyBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMjctMDcuClRoZSByZWxlYXNlIGlzIGRl" +
            "ZmluZWQgYnkgdW1icmVsbGEgSlNSIDMzNy4KCiMjIENsYXNzIGZpbGVzIGFuZCBVbmljb2RlClRoZSBjb21waWxlciBpbiBK" +
            "YXZhIDggZW1pdHMgY2xhc3MgZmlsZXMgd2l0aCBtYWpvciB2ZXJzaW9uIDUyLjAuCkphdmEgOCBidW5kbGVzIFVuaWNvZGUg" +
            "dmVyc2lvbiA2LjIuMC4KCiMjIE1haW50ZW5hbmNlIHVwZGF0ZXMKSmF2YSA4IGhhcyByZWNlaXZlZCAzMyBtYWludGVuYW5j" +
            "ZSB1cGRhdGVzIGluIHRoaXMgc25hcHNob3Q7IHRoZSBtb3N0IHJlY2VudCBpcyA4dTUwNCwgcmVsZWFzZWQgMjAyNi0wOC0x" +
            "OS4KVGhlIHJlZmVyZW5jZSBpbXBsZW1lbnRhdGlvbiBzb3VyY2VzIGFyZSBtYWludGFpbmVkIGF0IGh0dHBzOi8vZ2l0aHVi" +
            "LmNvbS9vcGVuamRrL2pkazh1Lgo=",
        "java-lts-overview.md" to "LS0tCmlkOiBqYXZhLWx0cy1vdmVydmlldwp0aXRsZTogSmF2YSBMVFMgcmVsZWFzZXMgb3ZlcnZpZXcKY2F0ZWdvcnk6IHJl" +
            "bGVhc2UKYWxpYXNlczogW0xUUywgbG9uZy10ZXJtIHN1cHBvcnQsIGNsYXNzIGZpbGUgdmVyc2lvbiwgcmVsZWFzZSBkYXRl" +
            "c10KbGFzdF9yZXZpZXdlZDogMjAyNi0xMC0xMApzdGF0dXM6IHB1Ymxpc2hlZAotLS0KClRoaXMgY29ycHVzIGNvdmVycyB0" +
            "aGUgbG9uZy10ZXJtIHN1cHBvcnQgKExUUykgcmVsZWFzZXMgb2YgSmF2YTogSmF2YSA4LCBKYXZhIDExLCBKYXZhIDE3LCBK" +
            "YXZhIDIxLCBKYXZhIDI1LgoKIyMgUmVsZWFzZSB0aW1lbGluZQpKYXZhIDggcmVhY2hlZCBnZW5lcmFsIGF2YWlsYWJpbGl0" +
            "eSBvbiAyMDE0LTAzLTE4LgpKYXZhIDExIHJlYWNoZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAxOC0wOS0yNS4KSmF2" +
            "YSAxNyByZWFjaGVkIGdlbmVyYWwgYXZhaWxhYmlsaXR5IG9uIDIwMjEtMDktMTQuCkphdmEgMjEgcmVhY2hlZCBnZW5lcmFs" +
            "IGF2YWlsYWJpbGl0eSBvbiAyMDIzLTA5LTE5LgpKYXZhIDI1IHJlYWNoZWQgZ2VuZXJhbCBhdmFpbGFiaWxpdHkgb24gMjAy" +
            "NS0wOS0xNi4KCiMjIENsYXNzLWZpbGUgdmVyc2lvbnMKQ2xhc3MgZmlsZXMgY29tcGlsZWQgZm9yIEphdmEgOCBjYXJyeSBt" +
            "YWpvciB2ZXJzaW9uIDUyLjAuCkNsYXNzIGZpbGVzIGNvbXBpbGVkIGZvciBKYXZhIDExIGNhcnJ5IG1ham9yIHZlcnNpb24g" +
            "NTUuMC4KQ2xhc3MgZmlsZXMgY29tcGlsZWQgZm9yIEphdmEgMTcgY2FycnkgbWFqb3IgdmVyc2lvbiA2MS4wLgpDbGFzcyBm" +
            "aWxlcyBjb21waWxlZCBmb3IgSmF2YSAyMSBjYXJyeSBtYWpvciB2ZXJzaW9uIDY1LjAuCkNsYXNzIGZpbGVzIGNvbXBpbGVk" +
            "IGZvciBKYXZhIDI1IGNhcnJ5IG1ham9yIHZlcnNpb24gNjkuMC4KVG8gZmluZCBvdXQgd2hpY2ggSmF2YSByZWxlYXNlIGEg" +
            "Y2xhc3MgZmlsZSBuZWVkcywgcmVhZCBpdHMgbWFqb3IgdmVyc2lvbjogNTIgbWVhbnMgSmF2YSA4LCA1NSBtZWFucyBKYXZh" +
            "IDExLCA2MSBtZWFucyBKYXZhIDE3LCA2NSBtZWFucyBKYXZhIDIxLCA2OSBtZWFucyBKYXZhIDI1LgoKIyMgU3VwcG9ydCB3" +
            "aW5kb3dzClN1cHBvcnQgZm9yIEphdmEgOCBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMjctMDcuClN1cHBvcnQgZm9yIEph" +
            "dmEgMTEgaXMgc2NoZWR1bGVkIHRvIGVuZCBpbiAyMDI3LTEwLgpTdXBwb3J0IGZvciBKYXZhIDE3IGlzIHNjaGVkdWxlZCB0" +
            "byBlbmQgaW4gMjAyNy0xMC4KU3VwcG9ydCBmb3IgSmF2YSAyMSBpcyBzY2hlZHVsZWQgdG8gZW5kIGluIDIwMjktMTIuClN1" +
            "cHBvcnQgZm9yIEphdmEgMjUgaXMgc2NoZWR1bGVkIHRvIGVuZCBpbiAyMDMwLTA5Lgo=",
    )
}
