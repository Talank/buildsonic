package maven;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Dependency {

    private String groupID;
    private String artifactID;
    private String version;
    private VersionSpecifier versionSpecifier;

    public Dependency(String groupID, String artifactID, String version, VersionSpecifier versionSpecifier) {
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.version = version;
        this.versionSpecifier = versionSpecifier;
    }

    public String getName() {
        return groupID + "/" + artifactID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return groupID.equals(that.groupID) && artifactID.equals(that.artifactID) &&
                versionSpecifier.equals(that.versionSpecifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupID,artifactID,versionSpecifier);
    }

    @Override
    public String toString() {
        return "groupID: " + groupID + " artifactID: " + artifactID + " version: " + version;
    }
}
