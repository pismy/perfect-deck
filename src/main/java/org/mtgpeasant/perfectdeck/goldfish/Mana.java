package org.mtgpeasant.perfectdeck.goldfish;

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

    static Mana of(String mana) {
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

    static Mana of(int b, int u, int g, int r, int w, int x) {
        return new Mana(b, u, g, r, w, x);
    }

    int ccm() {
        return B + U + G + R + W + X;
    }

    boolean contains(Mana other) {
        return B >= other.B
                && U >= other.U
                && G >= other.G
                && R >= other.R
                && W >= other.W
                && X >= other.X + other.B - B + other.U - U + other.G - G + other.R - R + other.W - W;
    }

    Mana plus(Mana other) {
        return new Mana(B + other.B, U + other.U, G + other.G, R + other.R, W + other.W, X + other.X);
    }

    Mana minus(Mana other) {
        return new Mana(
                Math.max(0, B - other.B),
                Math.max(0, U - other.U),
                Math.max(0, G - other.G),
                Math.max(0, R - other.R),
                Math.max(0, W - other.W),
                X - other.X);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(X == 0 ? "" : X)
                .append(Strings.repeat("B", B))
                .append(Strings.repeat("U", U))
                .append(Strings.repeat("G", G))
                .append(Strings.repeat("R", R))
                .append(Strings.repeat("W", W))
//                .append("(")
//                .append(ccm())
//                .append(")")
                .toString();
    }
}
