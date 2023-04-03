package smell.fixer.Maven

class MavenPlugin {
    String groupId
    String artifactId
    String version
    Map<String, String> configurationMap = new LinkedHashMap<>()

    MavenPlugin(String groupId, String artifactId, String version, Map<String, String> configurationMap) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
        this.configurationMap = configurationMap
    }

    @Override
    public String toString() {
        return "MavenPlugin{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", configurationMap=" + configurationMap +
                '}';
    }
}
