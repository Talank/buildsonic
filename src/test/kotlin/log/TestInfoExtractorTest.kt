package log

import java.nio.file.Paths


internal class TestInfoExtractorTest {

    @org.junit.jupiter.api.Test
    fun extractSection() {
        println(LOG_DIRECTORY_PATH)
        val filePath = Paths.get(LOG_DIRECTORY_PATH, "caelum@vraptor4", "42@1.log").toString()
        println(filePath)
        val extractor = TestInfoExtractor()
        val section = extractor.extractSection(filePath)
        section.forEach { println(it) }
        val infos = extractor.extractTestClassInfo(section)
        infos.forEach { println(it) }
    }
}