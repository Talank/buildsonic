package smell

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.junit.jupiter.api.Test
import static util.GradleUtil.GradleStrategy

import java.nio.file.Paths

class GradleFixerTest {
    @Test
    void testGradlePropertiesFixer1() {
        String path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "cloudinary@cloudinary_android.properties").normalize().toString()
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "out1.properties").normalize().toString()
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties()
                                .setFileName(path));

        Configuration config = builder.getConfiguration();
        config.setProperty("zc", "lemon");
        config.setProperty("test", "test")
        builder.save()
//        FileHandler handler = new FileHandler(config);
//        handler.save(outPath)
    }

    @Test
    void testGradleFileFixer1() {
        String path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "1.gradle").normalize().toString()
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "out1.gradle").normalize().toString()
        GradleFixer.gradleFileFixer(path, GradleStrategy.GRADLE_PARALLEL_TEST, outPath)
    }

}
