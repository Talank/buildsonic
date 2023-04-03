import org.junit.jupiter.api.Test;

/**
* HandleYaml Tester. 
* 
* @author <Authors name> 
* @since <pre>5�� 25, 2021</pre> 
* @version 1.0 
*/ 
public class HandleCacheTest {

@Test
public void testGetYamlFile() throws Exception {
    HandleCache handleCache =new HandleCache();
    String path="src\\main\\java\\.travis.yml";
    boolean result= handleCache.handleYamlFile(path);
    if (result) System.out.println("yes");
    else System.out.println("no");
}
} 
