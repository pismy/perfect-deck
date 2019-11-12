package org.mtgpeasant.perfectdeck.common.mana;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Value;

import java.util.Objects;

@Value
public class Mana {
    private static final Mana B = of(1, 0, 0, 0, 0, 0);
    private static final Mana U = of(0, 1, 0, 0, 0, 0);
    private static final Mana G = of(0, 0, 1, 0, 0, 0);
    private static final Mana R = of(0, 0, 0, 1, 0, 0);
    private static final Mana W = of(0, 0, 0, 0, 1, 0);
    private static final Mana ONE = of(0, 0, 0, 0, 0, 1);
    private static final Mana ZERO = of(0, 0, 0, 0, 0, 0);

    /**
     * ⓿
     */
    public static Mana zero() {
        return ZERO;
    }

    /**
     * ➊
     */
    public static Mana one() {
        return ONE;
    }

    /**
     * 🅑
     */
    public static Mana B() {
        return B;
    }

    /**
     * 🅡
     */
    public static Mana R() {
        return R;
    }

    /**
     * 🅖
     */
    public static Mana G() {
        return G;
    }

    /**
     * 🅤
     */
    public static Mana U() {
        return U;
    }

    /**
     * 🅦
     */
    public static Mana W() {
        return W;
    }

    final int b;
    final int u;
    final int g;
    final int r;
    final int w;
    final int x;

    private Mana(int b, int u, int g, int r, int w, int x) {
        Preconditions.checkArgument(b >= 0, "Cannot have negative amount of B");
        Preconditions.checkArgument(u >= 0, "Cannot have negative amount of U");
        Preconditions.checkArgument(g >= 0, "Cannot have negative amount of G");
        Preconditions.checkArgument(r >= 0, "Cannot have negative amount of R");
        Preconditions.checkArgument(w >= 0, "Cannot have negative amount of W");
        Preconditions.checkArgument(x >= 0, "Cannot have negative amount of X");
        this.b = b;
        this.u = u;
        this.g = g;
        this.r = r;
        this.w = w;
        this.x = x;
    }

    public static Mana of(String mana) {
        int b = 0, u = 0, g = 0, r = 0, w = 0;
        StringBuilder colorless = new StringBuilder();
        for (char c : mana.toCharArray()) {
            switch (c) {
                case 'B':
                case 'b':
                    b++;
                    break;
                case 'U':
                case 'u':
                    u++;
                    break;
                case 'G':
                case 'g':
                    g++;
                    break;
                case 'R':
                case 'r':
                    r++;
                    break;
                case 'W':
                case 'w':
                    w++;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    colorless.append(c);
                    break;
                default:
                    // ignore
            }
        }

        return of(b, u, g, r, w, colorless.length() == 0 ? 0 : Integer.parseInt(colorless.toString()));
    }

    public static Mana of(int b, int u, int g, int r, int w, int x) {
        return new Mana(b, u, g, r, w, x);
    }

    public int ccm() {
        return b + u + g + r + w + x;
    }

    public boolean contains(Mana other) {
        return b >= other.b
                && u >= other.u
                && g >= other.g
                && r >= other.r
                && w >= other.w
                && x >= other.x + other.b - b + other.u - u + other.g - g + other.r - r + other.w - w;
    }

    public Mana plus(Mana other) {
        return of(b + other.b, u + other.u, g + other.g, r + other.r, w + other.w, x + other.x);
    }

    public Mana minus(Mana other) {
        Extraction pull = extract(other);
        Preconditions.checkArgument(pull.notExtracted.isEmpty(), "Can't remove " + other + " mana from " + this);
        return pull.rest;
    }

    /**
     * Removes the given amount of mana from this pool
     * <p>
     * Example:
     * <pre>
     *     2B.remove(1BR)
     *     gives:
     *     - removed: 1B
     *     - notRemoved: R
     *     - rest: B
     * </pre>
     */
    public Extraction extract(Mana other) {
        // 1: remove colors
        Mana removed = of(
                Math.min(b, other.b),
                Math.min(u, other.u),
                Math.min(g, other.g),
                Math.min(r, other.r),
                Math.min(w, other.w),
                Math.min(x, other.x)
        );
        Mana rest = of(
                b - removed.b,
                u - removed.u,
                g - removed.g,
                r - removed.r,
                w - removed.w,
                x - removed.x
        );
        Mana notRemoved = of(
                other.b - removed.b,
                other.u - removed.u,
                other.g - removed.g,
                other.r - removed.r,
                other.w - removed.w,
                other.x - removed.x
        );

        // 2: pay remaining uncolors with coloured
        if (notRemoved.getX() > 0 && rest.ccm() > 0) {
            if (notRemoved.getX() >= rest.ccm()) {
                // we can consume all coloured from the rest
                removed = removed.plus(rest);
                notRemoved = notRemoved.minus(of(0, 0, 0, 0, 0, rest.ccm()));
                rest = zero();
            } else {
                // we consume partly
                while (notRemoved.getX() > 0 && rest.ccm() > 0) {
                    // TODO: which color to choose ?
                    if (rest.getB() > 0) {
                        rest = rest.minus(B);
                        removed = removed.plus(B);
                    } else if (rest.getU() > 0) {
                        rest = rest.minus(U);
                        removed = removed.plus(U);
                    } else if (rest.getG() > 0) {
                        rest = rest.minus(G);
                        removed = removed.plus(G);
                    } else if (rest.getR() > 0) {
                        rest = rest.minus(R);
                        removed = removed.plus(R);
                    } else if (rest.getW() > 0) {
                        rest = rest.minus(W);
                        removed = removed.plus(W);
                    }
                    notRemoved = notRemoved.minus(ONE);
                }
            }
        }

        return new Extraction(removed, notRemoved, rest);
    }

    @Value
    public static class Extraction {
        final Mana extracted;
        final Mana notExtracted;
        final Mana rest;
    }

    public boolean isEmpty() {
        return ccm() == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Mana mana = (Mana) other;
        return b == mana.b &&
                u == mana.u &&
                g == mana.g &&
                r == mana.r &&
                w == mana.w &&
                x == mana.x;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), b, u, g, r, w, x);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "0";
        }
        return (x > 0 ? x : "") // Character.toString((char)(x+9311))
                + Strings.repeat("B", b)
                + Strings.repeat("U", u)
                + Strings.repeat("G", g)
                + Strings.repeat("R", r)
                + Strings.repeat("W", w)
                + (x < 0 ? x : "");
//        if (isEmpty()) {
//            return "⓿";
//        }
//        return (x > 0 ? roundedNumber(x) : "")
//                + Strings.repeat("\uD83C\uDD51", b)
//                + Strings.repeat("\uD83C\uDD64", u)
//                + Strings.repeat("\uD83C\uDD56", g)
//                + Strings.repeat("\uD83C\uDD61", r)
//                + Strings.repeat("\uD83C\uDD66", w)
//                + (x < 0 ? x : "");
    }

    private static String roundedNumber(int nb) {
        switch (nb) {
            case 0:
                return "⓿";
            case 1:
                return "❶";
            case 2:
                return "❷";
            case 3:
                return "❸";
            case 4:
                return "❹";
            case 5:
                return "❺";
            case 6:
                return "❻";
            case 7:
                return "❼";
            case 8:
                return "❽";
            case 9:
                return "❾";
            case 10:
                return "❿";
            case 11:
                return "⓫";
            case 12:
                return "⓬";
            case 13:
                return "⓭";
            case 14:
                return "⓮";
            case 15:
                return "⓯";
            case 16:
                return "⓰";
            case 17:
                return "⓱";
            case 18:
                return "⓲";
            case 19:
                return "⓳";
            case 20:
                return "⓴";
            default:
                return String.valueOf(nb);
        }
    }
}
