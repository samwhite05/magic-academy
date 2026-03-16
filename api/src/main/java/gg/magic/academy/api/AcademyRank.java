package gg.magic.academy.api;

public enum AcademyRank {
    STUDENT(0),
    APPRENTICE(1),
    MAGE(2),
    MASTER_MAGE(3),
    ARCHMAGE(4);

    private final int level;

    AcademyRank(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public boolean isHigherThan(AcademyRank other) {
        return this.level > other.level;
    }

    public AcademyRank next() {
        AcademyRank[] values = values();
        int next = this.level + 1;
        return next < values.length ? values[next] : this;
    }
}
