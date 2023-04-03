import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

public class HandleCache {
    public boolean handleYamlFile(String filepath) throws FileNotFoundException {
        File f = new File(filepath);
        InputStream in = new FileInputStream(f);
        Yaml yaml = new Yaml();
        Map<String, Object> ret = (Map<String, Object>) yaml.load(in);
        if (ret.containsKey("cache")) return true;
        return  false;
    }
}
