package smell.fixer.Gradle


import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import static util.GradleUtil.GradleStrategy

import java.nio.file.Paths

class GradlePropertiesFixer {
    static void modifyProperties(String repoPath, List<GradleStrategy> strategies){
        if(strategies.size()==0){
            return
        }
        String path = Paths.get(repoPath,"gradle.properties").normalize().toString()
        File file = new File(path)
        if (!file.exists()) {
            try {
                file.createNewFile()
                println("不存在gradle.properties，已经创建新文件")
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties()
                                .setFileName(path));
        HashMap<String,String> properties = getProperties(strategies)
        Configuration config = builder.getConfiguration();
        for(String key: properties.keySet()){
            String value = properties.get(key)
            config.setProperty(key,value)
        }
        builder.save()
    }

    static HashMap<String, String> getProperties(List<GradleStrategy> strategies){
        HashMap<String, String> properties = new HashMap<>()
        for(GradleStrategy strategy : strategies){
            if(strategy == GradleStrategy.PARALLEL_BUILDS){
                properties.put("org.gradle.parallel", "true")
            } else if(strategy  == GradleStrategy.FILE_SYSTEM_WATCHING){
                properties.put("org.gradle.vfs.watch", "true")
            } else if(strategy == GradleStrategy.CONFIGURATION_ON_DEMAND){
                properties.put("org.gradle.configureondemand", "true")
            } else if(strategy == GradleStrategy.CACHING){
                properties.put("org.gradle.caching", "true")
            } else if(strategy == GradleStrategy.GRADLE_DAEMON){
                properties.put("org.gradle.daemon", "true")
            }
        }
        return properties
    }
}
