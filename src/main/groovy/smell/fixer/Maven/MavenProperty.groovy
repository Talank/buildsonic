package smell.fixer.Maven

class MavenProperty {
    String tag
    String content
    int offset
    int len

    MavenProperty(String tag, String content, int offset, int len) {
        this.tag = tag
        this.content = content
        this.offset = offset
        this.len = len
    }

    @Override
    public String toString() {
        return "MavenProperty{" +
                "tag='" + tag + '\'' +
                ", content='" + content + '\'' +
                ", offset=" + offset +
                ", len=" + len +
                '}';
    }
}
