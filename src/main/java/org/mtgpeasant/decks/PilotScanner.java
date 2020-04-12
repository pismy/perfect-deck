package org.mtgpeasant.decks;

import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PilotScanner {
    public static Collection<PilotMetadata> scan() throws IOException {
        return scan(PilotScanner.class.getPackage().getName());
    }

    public static List<PilotMetadata> scan(String basePackage) throws IOException {
        ClassPath cp = ClassPath.from(PilotScanner.class.getClassLoader());
        return cp.getTopLevelClassesRecursive(basePackage).stream()
                .map(classInfo -> classInfo.load())
                .filter(clazz -> DeckPilot.class.isAssignableFrom(clazz))
                .filter(clazz -> Modifier.isPublic(clazz.getModifiers()))
                .map(clazz -> PilotMetadata.load((Class<? extends DeckPilot>) clazz))
                .sorted(Comparator.comparing(PilotMetadata::getName))
                .collect(Collectors.toList());
    }

    @Value
    public static class PilotMetadata {
        String name;
        String description;
        Cards managedCards;
        Class<? extends DeckPilot> pilotClass;

        public static PilotMetadata load(Class<? extends DeckPilot> pilotClass) {
            return new PilotMetadata(name(pilotClass), loadDescription(pilotClass), DeckPilot.loadManagedCards(pilotClass), pilotClass);
        }

        private static String name(Class<? extends DeckPilot> pilotClass) {
            String name = pilotClass.getSimpleName();
            if (name.endsWith("DeckPilot")) {
                return name.substring(0, name.length() - 9);
            }
            if (name.endsWith("Pilot")) {
                return name.substring(0, name.length() - 5);
            }
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static String loadDescription(Class<? extends DeckPilot> pilotClass) {
        return loadDescription(pilotClass.getResourceAsStream(pilotClass.getSimpleName() + "-desc.md"));
    }

    private static String loadDescription(InputStream in) {
        if (in == null) {
            return null;
        }
        try {
            return CharStreams.toString(new InputStreamReader(in, Charset.forName("utf-8")));
        } catch (IOException e) {
            return null;
        }
    }

}
