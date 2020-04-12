package org.mtgpeasant.decks;

import com.google.common.reflect.ClassPath;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PilotScanner {
    public static Collection<PilotDescription> scan() throws IOException {
        return scan(PilotScanner.class.getPackage().getName());
    }

    public static List<PilotDescription> scan(String basePackage) throws IOException {
        ClassPath cp = ClassPath.from(PilotScanner.class.getClassLoader());
        return cp.getTopLevelClassesRecursive(basePackage).stream()
                .map(classInfo -> classInfo.load())
                .filter(clazz -> DeckPilot.class.isAssignableFrom(clazz))
                .filter(clazz -> Modifier.isPublic(clazz.getModifiers()))
                .map(clazz -> PilotDescription.load((Class<? extends DeckPilot>) clazz))
                .collect(Collectors.toList());
    }

    @Value
    public static class PilotDescription {
        String name;
        String description;
        Cards managedCards;
        Class<? extends DeckPilot> pilotClass;

        public static PilotDescription load(Class<? extends DeckPilot> pilotClass) {
            return new PilotDescription(name(pilotClass), desc(pilotClass), DeckPilot.loadManagedCards(pilotClass), pilotClass);
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

        private static String desc(Class<? extends DeckPilot> pilotClass) {
            // TODO
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
