package smell

enum StateFlag {
    OPEN(1), DEFAULT(2), CLOSE(0)

    private final int value
    private StateFlag(int value) {
        this.value = value
    }

    int getValue() {
        return value
    }
}
