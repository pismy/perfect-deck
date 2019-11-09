package org.mtgpeasant.perfectdeck.common;

import com.google.common.base.Strings;
import lombok.Value;

import java.util.Objects;

@Value
public class Mana {
    private static final Mana _B = Mana.of("B");
    private static final Mana _R = Mana.of("R");
    private static final Mana _G = Mana.of("G");
    private static final Mana _U = Mana.of("U");
    private static final Mana _W = Mana.of("W");
    private static final Mana ONE = Mana.of("1");
    private static final Mana ZERO = new Mana(0, 0, 0, 0, 0, 0);

    public static Mana zero() {
        return ZERO;
    }

    public static Mana one() {
        return ONE;
    }

    public static Mana b() {
        return _B;
    }

    public static Mana r() {
        return _R;
    }

    public static Mana g() {
        return _G;
    }

    public static Mana u() {
        return _U;
    }

    public static Mana w() {
        return _W;
    }

    final int B;
    final int U;
    final int G;
    final int R;
    final int W;
    final int X;

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
        return B + U + G + R + W + X;
    }

    public boolean contains(Mana other) {
        return B >= other.B
                && U >= other.U
                && G >= other.G
                && R >= other.R
                && W >= other.W
                && X >= other.X + other.B - B + other.U - U + other.G - G + other.R - R + other.W - W;
    }

    public Mana plus(Mana other) {
        return new Mana(B + other.B, U + other.U, G + other.G, R + other.R, W + other.W, X + other.X);
    }

    public Mana minus(Mana other) {
        if (!contains(other)) {
            throw new IllegalArgumentException("Can't remove " + other + " mana from " + this);
        }
        Mana result = new Mana(
                B - other.B,
                U - other.U,
                G - other.G,
                R - other.R,
                W - other.W,
                X - other.X);
        return result.ccm() == 0 ? zero() : result;
    }

    /**
     * Removes the given amount of mana from this
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
                Math.min(B, other.B),
                Math.min(U, other.U),
                Math.min(G, other.G),
                Math.min(R, other.R),
                Math.min(W, other.W),
                Math.min(X, other.X));
        Mana rest = this.minus(removed);
        Mana notRemoved = other.minus(removed);

        // 2: pay remaining uncolors with coloured
        while (notRemoved.getX() > 0 && rest.ccm() > 0) {
            // TODO: which color to choose ?
            if (rest.getB() > 0) {
                rest = rest.minus(_B);
                removed = removed.plus(_B);
            } else if (rest.getU() > 0) {
                rest = rest.minus(_U);
                removed = removed.plus(_U);
            } else if (rest.getG() > 0) {
                rest = rest.minus(_G);
                removed = removed.plus(_G);
            } else if (rest.getR() > 0) {
                rest = rest.minus(_R);
                removed = removed.plus(_R);
            } else if (rest.getW() > 0) {
                rest = rest.minus(_W);
                removed = removed.plus(_W);
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
        return B == mana.B &&
                U == mana.U &&
                G == mana.G &&
                R == mana.R &&
                W == mana.W &&
                X == mana.X;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), B, U, G, R, W, X);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "0";
        }
        return new StringBuilder()
                .append(X > 0 ? X : "")
                .append(Strings.repeat("B", B))
                .append(Strings.repeat("U", U))
                .append(Strings.repeat("G", G))
                .append(Strings.repeat("R", R))
                .append(Strings.repeat("W", W))
                .append(X < 0 ? X : "")
//                .append("(")
//                .append(ccm())
//                .append(")")
                .toString();
    }
}
