package org.mtgpeasant.decks.burn;

import org.mtgpeasant.perfectdeck.goldfish.Game;

public interface BurnCards {
    String DEFENDER_SUBTYPE = "defender";

    String PERM_BOOST = "+1/+1";
    String TURN_BOOST = "*+1/+1";

    // LANDS
    String MOUNTAIN = "mountain";
    String FORGOTTEN_CAVE = "forgotten cave";
    String FORGOTTEN_CAVE_CYCLE = "forgotten cave (cycle)";

    // CREATURES
    String MONASTERY_SWIFTSPEAR = "monastery swiftspear"; // R: haste; prowess +1/+1; 1/2
    String THERMO_ALCHEMIST = "thermo-alchemist";
    String KELDON_MARAUDERS = "keldon marauders";
    String GHITU_LAVARUNNER = "ghitu lavarunner"; // R: 1/2; if 2 or more instant/sorcery in GY: haste and +1/+0
    String ORCISH_HELLRAISER = "orcish hellraiser"; // 1R: 3/2; echo R; when dies: 2 damage to target player
    String VIASHINO_PYROMANCER = "viashino pyromancer"; // 1R: 2/1; 2 damage when enters the battlefield
    String FIREBRAND_ARCHER = "firebrand archer"; // 1R: 2/1; 1 damage for each non-crea spell
    String ELECTROSTATIC_FIELD = "electrostatic field"; // 1R: 0/4 wall; 1 damage for each instant or sorcery spell
    String KILN_FIEND = "kiln fiend";
    String FURNACE_SCAMP = "furnace scamp"; // R: 1/1 sacrifice when it deals combat damage to deal 3 more damages

    // BURN
    String RIFT_BOLT = "rift bolt"; // 2R: 3 damage; suspend 1: R
    String RIFT_BOLT_SP = "rift bolt (suspend)"; // 2R: 3 damage; suspend 1: R
    String FIREBLAST = "fireblast"; // sac 2 moutains: 4 damage
    String LAVA_SPIKE = "lava spike"; // R: 3 damage
    String LIGHTNING_BOLT = "lightning bolt"; // R: 3 damage
    String SKEWER_THE_CRITICS = "skewer the critics"; // 2R: 3 damage; spectacle: R
    String LAVA_DART = "lava dart"; // R: 1 damage; flashback: sac mountain
    String LAVA_DART_FB = "lava dart (flashback)"; // R: 1 damage; flashback: sac mountain
    String NEEDLE_DROP = "needle drop"; // R: 1 damage + 1 draw
    String CHAIN_LIGHTNING = "chain lightning"; // R: 3 damage
    String FORKED_BOLT = "forked bolt"; // R: 2 damage
    String SEARING_BLAZE = "searing blaze"; // RR: 1 damage to creature and controller; landfall: 3 damage instead
    String MAGMA_JET = "magma jet"; // 1R: 2 damage, scry 2
    String VOLCANIC_FALLOUT = "volcanic fallout"; // 1RR: 2 damage to each creature and player
    String FLAME_RIFT = "flame rift"; // 1R: 4 damage to each player
    String THUNDEROUS_WRATH = "thunderous wrath"; // 4RR: 5 damage; miracle: R
    String SEAL_OF_FIRE = "seal of fire";
    String RECKLESS_ABANDON = "reckless abandon";

    // OTHERS
    String CURSE_OF_THE_PIERCED_HEART = "curse of the pierced heart";
    String GITAXIAN_PROBE = "gitaxian probe";
    String LIGHT_UP_THE_STAGE = "light up the stage"; // 2R: exile 2 top card from library, can play until end of my next turn; spectacle: R

    default Game.CardType typeof(String cardName) {
        switch (cardName) {
            case MOUNTAIN:
            case FORGOTTEN_CAVE:
                return Game.CardType.land;

            case MONASTERY_SWIFTSPEAR:
            case THERMO_ALCHEMIST:
            case KELDON_MARAUDERS:
            case GHITU_LAVARUNNER:
            case ORCISH_HELLRAISER:
            case VIASHINO_PYROMANCER:
            case FIREBRAND_ARCHER:
            case KILN_FIEND:
            case FURNACE_SCAMP:
                return Game.CardType.creature;

            case FIREBLAST:
            case LIGHTNING_BOLT:
            case NEEDLE_DROP:
            case SEARING_BLAZE:
            case MAGMA_JET:
            case VOLCANIC_FALLOUT:
            case THUNDEROUS_WRATH:
            case LAVA_DART:
            case LAVA_DART_FB:
                return Game.CardType.instant;

            case RIFT_BOLT:
            case LAVA_SPIKE:
            case SKEWER_THE_CRITICS:
            case CHAIN_LIGHTNING:
            case FORKED_BOLT:
            case FLAME_RIFT:
            case GITAXIAN_PROBE:
            case LIGHT_UP_THE_STAGE:
            case RECKLESS_ABANDON:
                return Game.CardType.sorcery;

            case CURSE_OF_THE_PIERCED_HEART:
            case SEAL_OF_FIRE:
                return Game.CardType.enchantment;
        }
        return null;
    }
}
