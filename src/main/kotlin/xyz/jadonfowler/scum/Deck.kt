package xyz.jadonfowler.scum

import java.util.*

class Deck {
    val random = Random()

    val cards: MutableList<Card>
    val pulledCards: MutableList<Card>

    init {
        cards = mutableListOf()
        pulledCards = mutableListOf()
        reset()
    }

    fun reset() {
        cards.clear()
        pulledCards.clear()
        Suite.values().forEach {
            val suite = it
            Kind.values().forEach {
                cards.add(Card(it, suite))
            }
        }
    }

    private fun randInt(min: Int, max: Int): Int {
        return random.nextInt(max - min + 1) + min
    }

    fun pullRandom(): Card {
        val card = cards.removeAt(randInt(0, cards.size - 1))
        pulledCards.add(card)
        return card
    }
}

inline fun List<Card>?.value(): Int {
    return this!![0].kind.int
}

class Card(val kind: Kind, val suite: Suite) : Comparable<Card> {
    override fun toString(): String = "$kind of $suite"

    override fun compareTo(other: Card): Int {
        return other.kind.int - kind.int
    }
}

enum class Kind(val int: Int) {
    THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10),
    JACK(11), QUEEN(12), KING(13), ACE(14), TWO(15);

    fun fromInt(n: Int): Kind {
        return Kind.values()[n - 1]
    }

    override fun toString(): String {
        return when (this) {
            ACE, JACK, QUEEN, KING -> name[0].toString()
            TWO -> "2"
            else -> int.toString()
        }
    }
}

enum class Suite(val string: String) {
    SPADES("Spades"),
    HEARTS("Hearts"),
    DIAMONDS("Diamonds"),
    CLUBS("Clubs");

    override fun toString(): String = string
}
