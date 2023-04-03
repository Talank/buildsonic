package smell.fixer.Maven

class MavenDependency {
    String groupId
    String artifactId
    int offset
    int len

    MavenDependency(String groupId, String artifactId, int offset, int len) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.offset = offset
        this.len = len
    }

    @Override
    public String toString() {
        return "MavenDependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", offset=" + offset +
                ", len=" + len +
                '}';
    }
}
