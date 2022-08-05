package MainComComponents;

public enum CPUFlags {
    CARRY((byte)(1 << 0)),
    ZERO((byte)(1 << 1)),
    D_INTERRUPT((byte)(1 << 2)),
    DECIMAL((byte)(1 << 3)),
    BREAK((byte)(1 << 4)),
    UNUSED((byte)(1 << 5)),
    OVERFLOW((byte)(1 << 6)),
    NEGATIVE((byte)(1 << 7));

    private final byte position;

    CPUFlags(byte _position){position = _position;}

    byte getPosition() { return position; }
}
