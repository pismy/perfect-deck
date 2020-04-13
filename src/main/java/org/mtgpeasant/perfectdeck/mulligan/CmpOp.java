package org.mtgpeasant.perfectdeck.mulligan;

public enum CmpOp {
    LT {
        @Override
        public boolean compare(int value, int to) {
            return value < to;
        }
    },
    LE {
        @Override
        public boolean compare(int value, int to) {
            return value <= to;
        }
    },
    EQ {
        @Override
        public boolean compare(int value, int to) {
            return value == to;
        }
    },
    GE {
        @Override
        public boolean compare(int value, int to) {
            return value >= to;
        }
    },
    GT {
        @Override
        public boolean compare(int value, int to) {
            return value > to;
        }
    };

    public abstract boolean compare(int value, int to);

    public static CmpOp parse(String str) {
        switch (str.toLowerCase()) {
            case "lt":
            case "<":
                return LT;
            case "le":
            case "<=":
                return LE;
            case "eq":
            case "==":
            case "=":
                return EQ;
            case "ge":
            case ">=":
                return GE;
            case "gt":
            case ">":
                return GT;
            default:
                throw new IllegalArgumentException("Unrecognized comarision operator: '" + str + "'");
        }
    }
}
