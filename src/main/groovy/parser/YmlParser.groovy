package parser

import org.yaml.snakeyaml.Yaml

class YmlParser {
    // 解析yaml文件并返回一个map
    static LinkedHashMap<String, Object> parse(String filePath) {
        File file = new File(filePath)
        if (file.exists() == false) {
            return null
        }
        String content = file.text
        LinkedHashMap<String, Object> map = null
        try {
            map = new Yaml().load(content)
        } catch(Exception e) {
            //println(filePath)
            if (e.message.contains("TAB")) {
                content = content.replace("\t", "  ")
                map = new Yaml().load(content)
            }
            //e.printStackTrace()
        }
        return map
    }

    // 是都是maven gradle运行指令
    static boolean isCommand(String content) {
        if (content == null)
            return false
        if (content.contains("mvn") || content.contains("gradle")) {
            return true
        }
        return false
    }

    static List<String> getCommands(Object o) {
        return []
    }

    //TODO
    //可能需要利用shell parser
    static List<String> getCommands(String s) {
        return [s]
    }

    static List<String> getCommands(ArrayList<Object> arrayList) {
        List<String> list = []
        arrayList.each {
            list.addAll(getCommands(it))
        }
        return list
    }

    // 返回所有string类型命令
    static List<String> getCommands(LinkedHashMap<String, Object> map) {
        List<String> list = []
        map.each {k, v ->
            list.addAll(getCommands(v))
        }
        return list
    }

    // 返回map 的key
    static void getCacheValues(def o, List<Object> list) {
        if (o instanceof Map) {
            o.each {k, v ->
                if (k.equals("cache"))
                list << v
                getCacheValues(v, list)
            }
        } else if (o instanceof List) {
            o.each {
                getCacheValues(it, list)
            }
        }
    }

    static void getKeyValues(def o, List<Object> list, String key) {
        if (o instanceof Map) {
            o.each {k, v ->
                if (k.equals(key))
                    list << v
                getKeyValues(v, list, key)
            }
        } else if (o instanceof List) {
            o.each {
                getKeyValues(it, list, key)
            }
        }
    }

    static void getKeys(def o, List<String> list) {
        if (o instanceof Map) {
            o.each {k, v ->
                list << k
                getKeys(v, list)
            }
        } else if (o instanceof List) {
            o.each {
                getKeys(it, list)
            }
        }
    }
}
