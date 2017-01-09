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
    val finishedPlayers = mutableListOf<Player>()

    fun handOutCards() {
        val deck = Deck()
        var i = 0
        while (deck.cards.size > 0) {
            players[i].hand.add(deck.pullRandom())
            if (++i > players.size - 1) i = 0
        }
    }

    fun reset() {
        players.addAll(finishedPlayers)
        finishedPlayers.clear()
        top = null
    }

    fun takeCards() {
        /*
        7 3 1   8 3 1   9 4 1   10 4 1   6 2 1
        6 2 2   7 2 2   8 3 2    9 3 2   5 1 2
        5 1 3   6 1 3   7 2 3    8 2 3    4 3
          4      5 4    6 1 4    7 1 4
                          5       6 5
         */
        val topMiddle =
                if (players.size % 2 == 0)
                    players.size / 2 + 1
                else Math.ceil(players.size / 2.0).toInt()
        val mostToTake = players.size - topMiddle
        var amount = mostToTake
        var index = 0
        while (amount > 0) {
            val highPlayer = players[index]
            val lowPlayer = players[players.size - index - 1]
            var take = highPlayer.take(amount, lowPlayer.hand)

            if (take != null) {
                val takeSize = take.size.div(2)
                // weird null shit in while loop
                val takeMod2 = take.size % 2
                while (!(takeSize <= amount && takeMod2 == 0)) {
                    take = highPlayer.take(amount, lowPlayer.hand)
                }
            }

            if (take != null) {
                val cardsToGive = take.subList(0, take.size / 2)
                val cardsToTake = take.subList(take.size / 2, take.size)

                if (highPlayer.hand.containsAll(cardsToGive)
                        && lowPlayer.hand.containsAll(cardsToTake)) {
                    highPlayer.hand.removeAll(cardsToGive)
                    highPlayer.hand.addAll(cardsToTake)
                    lowPlayer.hand.removeAll(cardsToTake)
                    lowPlayer.hand.addAll(cardsToGive)
                }
            }
            amount--
            index++
        }

    }

    fun start(takeCards: Boolean = false) {
        players.forEach {
            it.sendMessage("Passing out cards...")
        }
        handOutCards()

        if (takeCards) takeCards()

        val totalPlayers = players.size
        var i = 0
        var lastPlayed = -1
        while (true) {
            if (players.size <= i)
                break

//            println("   $lastPlayed ${players.size} $i")
            if (lastPlayed == i || (lastPlayed > (players.size - 1) && (i == players.size - 1))) {
                top = null
                val swipeMessage = { p: Player ->
                    p.sendMessage("Top card swiped.")
                }
                players.forEach(swipeMessage)
                finishedPlayers.forEach(swipeMessage)
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

            val message = { p: Player ->
                if (played == null)
                    p.sendMessage("$player didn't play anything.")
                else if (played!!.size == 1)
                    p.sendMessage("$player played a ${played!![0]}.")
                else
                    p.sendMessage("$player played $played.")

                if (playerFinished)
                    p.sendMessage("$player finished #${totalPlayers - players.size + 1}!")
            }
            players.forEach(message)
            finishedPlayers.forEach(message)

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
            val skip = top != null && played != null && top?.value() == played.value()

            if (played != null && played[0].kind != Kind.TWO) {
                top = played
                lastPlayed = i
            }
            if (playerFinished) {
                val p = players.removeAt(i)
                finishedPlayers.add(p)
            }

            if (++i > players.size - 1) i = 0
            if (skip) {
                if (players.size >= i) {
                    val skippedMessage = { p: Player ->
                        p.sendMessage("${players[i]} was skipped.")
                    }
                    players.forEach(skippedMessage)
                    finishedPlayers.forEach(skippedMessage)
                }

                if (++i > players.size - 1) i = 0

            }
        }
        reset()
        players.forEach {
            it.sendMessage("The game has finished!")
        }
        start(true)
    }

}

abstract class Player(val name: String) {
    val hand = mutableListOf<Card>()

    override fun toString(): String = name

    abstract fun play(top: List<Card>?): List<Card>?
    abstract fun take(amount: Int, theirHand: List<Card>): List<Card>?
    abstract fun sendMessage(message: String)
}

class ConsolePlayer(name: String) : Player(name) {

    override fun sendMessage(message: String) {
        println(message)
    }

    override fun take(amount: Int, theirHand: List<Card>): List<Card>? {

        println("Your hand:")
        hand.sort()
        hand.forEachIndexed { i, card ->
            // Put an X by the cards we can't use.
            println("  [${i + 1}] $card")
        }

        println("Their hand:")
        val theirHandSorted = theirHand.sorted()
        theirHandSorted.forEachIndexed { i, card ->
            // Put an X by the cards we can't use.
            println("  [${i + 1}] $card")
        }

        fun getIndices(): List<Int>? {
            val input = readLine()
            if (input == null || input.isNullOrEmpty()) return null

            if (input.matches(Regex("[0-9]+")))
                return listOf(input.toInt() - 1)
            else if (input.matches(Regex("[0-9]+, [0-9]+"))
                    || input.matches(Regex("[0-9]+, [0-9]+, [0-9]+"))
                    || input.matches(Regex("[0-9]+, [0-9]+, [0-9]+, [0-9]+"))) {
                val indices = input.split(", ").map { it.toInt() - 1 }

                // Check if there are multiple indices in the input
                val searchIndices = mutableListOf<Int>()
                indices.forEach {
                    if (searchIndices.contains(it)) return getIndices()
                    else searchIndices.add(it)
                }
                return indices
            } else return null
        }

        print("Please choose $amount cards to take: ")
        val takeIndices = getIndices()

        takeIndices ?: return null
        takeIndices.forEach {
            if (it < 0 || it > theirHandSorted.size)
                return null
        }

        print("Please choose $amount cards to give: ")
        val giveIndices = getIndices()

        giveIndices ?: return null
        giveIndices.forEach {
            if (it < 0 || it > hand.size)
                return null
        }

        val cardsToGive = giveIndices.map { hand[it] }.toMutableList()
        val cardsToTake = takeIndices.map { theirHandSorted[it] }
        println("$cardsToGive -> $cardsToTake (amount: $amount)")
        cardsToGive.addAll(cardsToTake)
        return cardsToGive
    }

    override fun play(top: List<Card>?): List<Card>? {
        println()

        val validCards = hand.filter {
            if (top != null) it.kind.int >= top.value()
            else true
        }

        if (top != null) {
            println("The top card is: ${if (top.size > 1) top.toString()
            else top[0].toString()}.")

            // If we can't play anything, don't play anything.
            if (validCards.isEmpty()) {
                println("You couldn't play anything.")
                return null
            }

            // If we can only play our final card, play that card.
            if (validCards.size == 1 && hand.size == 1) {
                return listOf(validCards[0])
            }
        }

        // If we can play any card and we only have one card, play that card.
        if (top == null && hand.size == 1) return listOf(hand[0])

        println("Your hand:")
        hand.sort()
        hand.forEachIndexed { i, card ->
            // Put an X by the cards we can't use.
            println("${if (validCards.contains(card)) " " else "X"} [${i + 1}] $card")
        }

        print("Please choose a card: ")
        val input = readLine()
        if (input == null || input.isNullOrEmpty()) return null

        // Get one card
        if (input.matches(Regex("[0-9]+"))) {
            val index = input.toInt() - 1
            if (index >= hand.size) return play(top)
            if (index < 0 || index > hand.size)
                return play(top)
            val card = hand[index]
            return listOf(card)
        }
        // Get multiple cards
        else if (input.matches(Regex("[0-9]+, [0-9]+"))
                || input.matches(Regex("[0-9]+, [0-9]+, [0-9]+"))
                || input.matches(Regex("[0-9]+, [0-9]+, [0-9]+, [0-9]+"))) {
            val indices = input.split(", ").map { it.toInt() - 1 }

            // Check if there are multiple indices in the input
            val searchIndices = mutableListOf<Int>()
            indices.forEach {
                if (searchIndices.contains(it)) return play(top)
                else searchIndices.add(it)
            }

            // Make sure the indices are within the bounds of the hand
            indices.forEach {
                if (it < 0 || it > hand.size)
                    return play(top)
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

    override fun take(amount: Int, theirHand: List<Card>): List<Card>? {
        val cardsToTake = theirHand.sorted().subList(0, amount)
        val cardsToGive = hand.sorted().asReversed().subList(0, amount).toMutableList()
//        println("$name $cardsToGive -> $cardsToTake (amount: $amount)")
        cardsToGive.addAll(cardsToTake)
        return cardsToGive
    }

    override fun play(top: List<Card>?): List<Card>? {
        Thread.sleep(500) // Thinking takes a while
        if (top == null) return listOf(hand.last())
        val validCards = hand.filter { it.kind.int >= top.value() }.toMutableList().sorted().reversed()
//        println("  Top Card: $top, $name's hand: $validCards from $hand.")
        if (validCards.isEmpty()) return null

        val amountNeeded = top.size
        var amount = 0
        val indices = mutableListOf<Int>()
        var lastKind: Kind? = null

        validCards.forEachIndexed { i, card ->
            if (amount == amountNeeded)
                return indices.map { validCards[it] }

            if (lastKind == null) {
                lastKind = card.kind
                indices.add(i)
                amount++
            } else {
                if (lastKind == card.kind) {
                    indices.add(i)
                    amount++
                } else {
                    indices.clear()
                    amount = 0
                    lastKind = null
                }
            }
        }
        if (amount == amountNeeded)
            return indices.map { validCards[it] }
        else
            return null
    }
}
