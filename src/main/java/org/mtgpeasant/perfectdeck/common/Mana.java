package org.mtgpeasant.perfectdeck.common;

import com.google.common.base.Strings;
import lombok.Value;

@Value
public class Mana {
    final int B;
    final int U;
    final int G;
    final int R;
    final int W;
    final int X;

    public static Mana zero() {
        return new Mana(0, 0, 0, 0, 0, 0);
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

    public boolean isEmpty() {
        return ccm() == 0;
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
