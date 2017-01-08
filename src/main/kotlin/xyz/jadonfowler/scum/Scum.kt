package xyz.jadonfowler.scum

fun main(args: Array<String>) {
    val players = mutableListOf<Player>()

    // Make player
    print("Please choose a name: ")
    var name = readLine()
    if (name.isNullOrEmpty()) name = "Chuck"
    players.add(ConsolePlayer(name!!))

    // Add bots
    print("How many bots? ")
    val amount = readLine()!!.toInt()
    (1..amount).forEach { players.add(BotPlayer(it)) }

    val game = Game(players)
    game.start()
}

class Game(val players: MutableList<Player>) {
    var top: List<Card>? = null

    fun handOutCards() {
        val deck = Deck()
        var i = 0
        while (deck.cards.size > 0) {
            players[i].hand.add(deck.pullRandom())
            if (++i > players.size - 1) i = 0
        }
    }

    fun start() {
        handOutCards()

        val totalPlayers = players.size
        var i = 0
        var lastPlayed = -1
        while (true) {
            if (players.size <= i)
                break

            if (lastPlayed == i) {
                top = null
                players.forEach {
                    it.sendMessage("Top card swiped.")
                }
            }

            val player = players[i]
            var played = player.play(top)

            if (top != null && played != null && played[0].kind != Kind.TWO) {
                while (top?.size != played?.size) {
                    player.sendMessage("Choose the right amount of cards!")
                    played = player.play(top)
                }

                val topValue = top?.value()!!
                val playedValue = played?.value()!!

                while (topValue > playedValue && played != null) {
                    player.sendMessage("You need to choose a higher card!")
                    played = player.play(top)
                }
            }

            // Remove cards from player's hand
            played?.forEach {
                player.hand.remove(it)
            }

            val playerFinished = player.hand.size < 1

            players.forEach {
                if (played == null)
                    it.sendMessage("$player didn't play anything.")
                else if (played!!.size == 1)
                    it.sendMessage("$player played a ${played!![0]}.")
                else
                    it.sendMessage("$player played $played.")

                if (playerFinished)
                    it.sendMessage("$player finished #${totalPlayers - players.size + 1}!")
            }

            // Swipe if 2
            if (played?.get(0)?.kind == Kind.TWO) {
                top = null
                players.forEach {
                    it.sendMessage("Top card swiped.")
                }
                // Reset turn
                if (player.hand.size > 0) i--
            }

            // If the card is the same, skip the next player
            val skip = top?.value() == played?.value()

            if (played != null && played[0].kind != Kind.TWO) {
                top = played
                lastPlayed = i
            }
            if (player.hand.size < 1)
                players.removeAt(i)

            if (++i > players.size - 1) i = 0
            if (skip) if (++i > players.size - 1) i = 0
        }
    }

}

abstract class Player(val name: String) {
    val hand = mutableListOf<Card>()

    override fun toString(): String = name

    abstract fun play(top: List<Card>?): List<Card>?
    abstract fun sendMessage(message: String)
}

class ConsolePlayer(name: String) : Player(name) {

    override fun sendMessage(message: String) {
        println(message)
    }

    override fun play(top: List<Card>?): List<Card>? {
        println()
        if (top != null)
            println("The top card is: ${if (top.size > 1) top.toString()
            else top[0].toString()}.")

        println("Your hand:")
        hand.sort()
        hand.forEachIndexed { i, card ->
            println("  [$i] $card")
        }

        print("Please choose a card: ")
        var input = readLine()
        while (input == null) input = readLine()

        // Get one card
        if (input.matches(Regex("[0-9]+"))) {
            val index = input.toInt()
            if (index >= hand.size) return play(top)
            val card = hand[index]
            return listOf(card)
        }
        // Get multiple cards
        else if (input.matches(Regex("[0-9]+, [0-9]+"))
                || input.matches(Regex("[0-9]+, [0-9]+, [0-9]+"))
                || input.matches(Regex("[0-9]+, [0-9]+, [0-9]+, [0-9]+"))) {
            val indices = input.split(", ").map { it.toInt() }

            // Check if there are multiple indices in the input
            val searchIndices = mutableListOf<Int>()
            indices.forEach {
                if (searchIndices.contains(it)) return play(top)
                else searchIndices.add(it)
            }

            val cards = indices.map { hand[it] }
            return cards
        } else if (input.contains("none")) return null
        else return play(top)
    }

}

class BotPlayer(i: Int) : Player("Bot $i") {
    override fun sendMessage(message: String) {
//        println(" Message to $name: $message")
    }

    override fun play(top: List<Card>?): List<Card>? {

        if (top == null) return listOf(hand.last())
        val validCards = hand.filter { it.kind.int >= top.value() }.toMutableList().sorted().reversed()
        if (validCards.isEmpty()) return null
//        println("  $name's hand: $validCards.")

        val amountNeeded = top.size
        var amount = 0
        val indices = mutableListOf<Int>()
        var lastKind: Kind? = null

        validCards.forEachIndexed { i, card ->
            if (amount == amountNeeded) {
                return indices.map { validCards[it] }
            }

            if (lastKind == null) {
                lastKind = card.kind
                indices.add(i)
                amount++
            } else {
                if (lastKind == card.kind) {
                    indices.add(i)
                    amount++
                } else {
                    amount = 0
                    indices.clear()
                }
            }
        }
        return null
    }
}
