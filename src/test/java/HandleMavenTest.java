import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class HandleMavenTest {

    @Test
    void handleyml() throws FileNotFoundException {
        HandleMaven handleMaven=new HandleMaven();
        String path="src\\main\\java\\.travis.yml";
        boolean res=handleMaven.handleyml(path);
        System.out.println(res);
    }

    @Test
    void handlepom() {
        HandleMaven handleMaven=new HandleMaven();
        String path="D:\\Data\\common\\workingProject\\pz\\searching\\bashparser-master\\pom.xml";
        boolean res=handleMaven.handlepom(path);
    }
    @Test
    void handlebash() {
    }
}