package parser

class GradleDependency {
    String groupId;
    String artifactId;
    String version;

    GradleDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    public GradleDependency( Map<String, String> dep )
    {
        this.groupId = dep.get( "group" )
        this.artifactId = dep.get( "name" )
        this.version =  dep.get( "version" )
    }


    @Override
    public String toString() {
        return "GradleDependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
