package org.mtgpeasant.perfectdeck.common;

import com.google.common.base.Strings;
import lombok.Value;

import java.util.Objects;

@Value
public class Mana {
    private static final Mana B = Mana.of("B");
    private static final Mana R = Mana.of("R");
    private static final Mana G = Mana.of("G");
    private static final Mana U = Mana.of("U");
    private static final Mana W = Mana.of("W");
    private static final Mana ONE = Mana.of("1");
    private static final Mana ZERO = new Mana(0, 0, 0, 0, 0, 0);

    public static Mana zero() {
        return ZERO;
    }

    public static Mana one() {
        return ONE;
    }

    public static Mana b() {
        return B;
    }

    public static Mana r() {
        return R;
    }

    public static Mana g() {
        return G;
    }

    public static Mana u() {
        return U;
    }

    public static Mana w() {
        return W;
    }

    final int b;
    final int u;
    final int g;
    final int r;
    final int w;
    final int x;

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
        return new Mana(b + other.b, u + other.u, g + other.g, r + other.r, w + other.w, x + other.x);
    }

    public Mana minus(Mana other) {
        if (!contains(other)) {
            throw new IllegalArgumentException("Can't remove " + other + " mana from " + this);
        }
        Mana result = new Mana(
                b - other.b,
                u - other.u,
                g - other.g,
                r - other.r,
                w - other.w,
                x - other.x);
        return result.ccm() == 0 ? zero() : result;
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
    public RemoveResult remove(Mana other) {
        // 1: remove colors
        Mana removed = new Mana(
                Math.min(b, other.b),
                Math.min(u, other.u),
                Math.min(g, other.g),
                Math.min(r, other.r),
                Math.min(w, other.w),
                Math.min(x, other.x));
        Mana rest = this.minus(removed);
        Mana notRemoved = other.minus(removed);

        // 2: pay remaining uncolors with coloured
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

        return new RemoveResult(removed, notRemoved, rest);
    }

    @Value
    public static class RemoveResult {
        final Mana removed;
        final Mana notRemoved;
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
        return new StringBuilder()
                .append(x > 0 ? x : "")
                .append(Strings.repeat("B", b))
                .append(Strings.repeat("U", u))
                .append(Strings.repeat("G", g))
                .append(Strings.repeat("R", r))
                .append(Strings.repeat("W", w))
                .append(x < 0 ? x : "")
//                .append("(")
//                .append(ccm())
//                .append(")")
                .toString();
    }
}
