package chess

import kotlin.math.abs

fun main() {
    val game = Game()
    game.run()
}

class FinishGameException : RuntimeException()
class EnPassantException : RuntimeException()

class GameOption {
    companion object {
        const val NUMBER_OF_FILES = 8
        const val NUMBER_OF_RANKS = 8
        const val INITIAL_RANK_OF_WHITE = 2 - 1
        const val INITIAL_RANK_OF_BLACK = 7 - 1
        const val SPACE_SYMBOL = ' '
        const val NOTICE_REGEX = "###"
        const val TERMINATE_COMMAND = "exit"
        const val COMMAND_REGEX = """([a-zA-Z]\d){2}"""
        const val COLOR_FIRST_PLAYER = "white"
        const val LETTER_FIRST_PLAYER = 'W'
        const val COLOR_SECOND_PLAYER = "black"
        const val LETTER_SECOND_PLAYER = 'B'
    }
}

enum class GameNotice(val msg: String) {
    GAME_CAPTION("Pawns-Only Chess"),
    ASK_FIRST_PLAYER_NAME("First Player's name:"),
    ASK_SECOND_PLAYER_NAME("Second Player's name:"),
    ASK_PLAYER_TURN("${GameOption.NOTICE_REGEX}'s turn:"),
    BYE_NOTICE("Bye!"),
    INVALID_INPUT("Invalid Input"),
    NO_PAWN_AT_POSITION("No ${GameOption.NOTICE_REGEX} pawn at ${GameOption.NOTICE_REGEX}"),
    PLAYER_WIN("${GameOption.NOTICE_REGEX} Wins!"),
    PLAYER_DRAW("Stalemate!"),
}

class Game {
    private val board =
        Array(GameOption.NUMBER_OF_RANKS) { CharArray(GameOption.NUMBER_OF_FILES) { GameOption.SPACE_SYMBOL } }
    private val gamePlay = GamePlay()
    private var firstPlayer = ""
    private var secondPlayer = ""
    private var currentPlayer = 1
    private var previsionCommand = ""
    private var currentCommand = ""

    fun run() {
        showCaption()
        firstPlayer = askPlayerName(1)
        secondPlayer = askPlayerName(2)
        boardInitialise()
        showBoard()
        while (true) {
            previsionCommand = currentCommand
            if (isCheckStalemate(currentPlayer)) {
                showDrawMessage()
                showByeMessage()
                return
            }
            try {
                askPlayerTurn(currentPlayer)
                moveFigure(currentCommand)
            } catch (ex: FinishGameException) {
                showByeMessage()
                return
            } catch (ex: EnPassantException) {
                moveEnPassant(currentCommand)
            }
            showBoard()
            if (isReachFinalRank(currentPlayer, currentCommand)
                || isOpponentPawnsIsEmpty(currentPlayer)
            ) {
                showWinMessage(currentPlayer)
                showByeMessage()
                return
            }
            currentPlayer = if (currentPlayer != 1) 1 else 2
        }
    }

    private fun isCheckStalemate(player: Int): Boolean {
        val letter = if (player == 1) GameOption.LETTER_FIRST_PLAYER else GameOption.LETTER_SECOND_PLAYER
        val oppositeLetter = if (player == 1) GameOption.LETTER_SECOND_PLAYER else GameOption.LETTER_FIRST_PLAYER
        val diff = if (player == 1) +1 else -1
        for (row in board.indices) {
            for (col in board[row].indices) {
                val content = board[row][col]
                if (content != letter) {
                    continue
                }
                val newRow = row + diff
                if (newRow in 0..board.lastIndex && board[newRow][col] == GameOption.SPACE_SYMBOL) {
                    return false
                }
                for (newCol in listOf(col + 1, col - 1)) {
                    if (newRow in 0..board.lastIndex
                        && newCol in 0..board[0].lastIndex
                        && board [newRow][newCol] == oppositeLetter
                    ) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun isReachFinalRank(player: Int, command: String): Boolean {
        val number = gamePlay.getPosition(command[2], command[3]).second
        return if (player == 1) {
            number == (GameOption.NUMBER_OF_RANKS - 1)
        } else {
            number == 0
        }
    }

    private fun isOpponentPawnsIsEmpty(player: Int): Boolean {
        val oppositePlayer = if (player == 1) 2 else 1
        return gamePlay.getCountOfPawns(board, oppositePlayer) == 0
    }

    private fun askPlayerTurn(player: Int) {
        val playerName = if (player == 1) firstPlayer else secondPlayer
        val question = GameNotice.ASK_PLAYER_TURN.msg.replace(GameOption.NOTICE_REGEX, playerName)
        while (true) {
            writeln(question)
            currentCommand = readln()
            if (currentCommand == GameOption.TERMINATE_COMMAND) {
                throw FinishGameException()
            }

            if (!gamePlay.isValidCommand(currentCommand)) {
                writeln(GameNotice.INVALID_INPUT.msg)
                continue
            }

            if (!gamePlay.isValidInitialPosition(board, player, currentCommand)) {
                val color = if (player == 1) GameOption.COLOR_FIRST_PLAYER else GameOption.COLOR_SECOND_PLAYER
                val msg = GameNotice.NO_PAWN_AT_POSITION.msg
                    .replaceFirst(GameOption.NOTICE_REGEX, color)
                    .replaceFirst(GameOption.NOTICE_REGEX, "${currentCommand[0]}${currentCommand[1]}")
                writeln(msg)
                continue
            }

            if (!gamePlay.isValidDestinationPositionMoveForward(board, player, currentCommand)
                && !gamePlay.isCapture(board, player, currentCommand)
                && !gamePlay.isCaptureEnPassant(board, player, currentCommand, previsionCommand)
            ) {
                writeln(GameNotice.INVALID_INPUT.msg)
                continue
            }
            break
        }
    }

    private fun moveFigure(command: String) {
        val symbol = gamePlay.getContentAtPosition(board, command[0], command[1])
        gamePlay.setContentAtPosition(board, command[0], command[1], GameOption.SPACE_SYMBOL)
        gamePlay.setContentAtPosition(board, command[2], command[3], symbol)
    }

    private fun moveEnPassant(command: String) {
        moveFigure(command)
        gamePlay.setContentAtPosition(board, previsionCommand[2], previsionCommand[3], GameOption.SPACE_SYMBOL)
    }

    private fun showWinMessage(player: Int) {
        val color = (if (player == 1) GameOption.COLOR_FIRST_PLAYER else GameOption.COLOR_SECOND_PLAYER)
        val msg = GameNotice.PLAYER_WIN.msg.replace(
            GameOption.NOTICE_REGEX,
            color.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
        writeln(msg)
    }

    private fun showDrawMessage() {
        writeln(GameNotice.PLAYER_DRAW.msg)
    }

    private fun showByeMessage() = writeln(GameNotice.BYE_NOTICE.msg)

    private fun askPlayerName(player: Int): String {
        val question = if (player == 1) GameNotice.ASK_FIRST_PLAYER_NAME else GameNotice.ASK_SECOND_PLAYER_NAME
        writeln(question.msg)
        return readln()
    }

    private fun boardInitialise() {
        board[GameOption.INITIAL_RANK_OF_WHITE].fill(GameOption.LETTER_FIRST_PLAYER)
        board[GameOption.INITIAL_RANK_OF_BLACK].fill(GameOption.LETTER_SECOND_PLAYER)
    }

    private fun showCaption() = writeln(GameNotice.GAME_CAPTION.msg)

    private fun showBoard() {
        val rowSeparator = "  +---+---+---+---+---+---+---+---+"

        writeln(rowSeparator)
        for (i in board.size - 1 downTo 0) {
            val row = board[i].joinToString(" | ")
            writeln("${i + 1} | $row |")
            writeln(rowSeparator)
        }
        val letters = (0 until board.first().size).map { 'a' + it }.joinToString("   ")
        writeln("${" ".repeat(4)}$letters")
    }

    private fun writeln(msg: String) {
        write("$msg\n")
    }

    private fun write(msg: String) {
        print(msg)
    }

}

class GamePlay {

    fun isValidCommand(command: String): Boolean {
        if (!GameOption.COMMAND_REGEX.toRegex().matches(command)) {
            return false
        }
        for (i in listOf(0, 2)) {
            if (command[i] !in 'a' until 'a' + GameOption.NUMBER_OF_RANKS) {
                return false
            }
        }
        for (i in listOf(1, 3)) {
            if (command[i].digitToInt() !in 1..GameOption.NUMBER_OF_FILES) {
                return false
            }
        }
        return true
    }

    fun isValidInitialPosition(board: Array<CharArray>, player: Int, command: String): Boolean {
        val letter = if (player == 1) GameOption.LETTER_FIRST_PLAYER else GameOption.LETTER_SECOND_PLAYER
        val content = getContentAtPosition(board, command[0], command[1])
        return content == letter
    }

    fun isValidDestinationPositionMoveForward(board: Array<CharArray>, player: Int, command: String): Boolean {
        val (letter1, number1) = getPosition(command[0], command[1])
        val (letter2, number2) = getPosition(command[2], command[3])
        if (letter1 != letter2) {
            return false
        }

        val delta = getPositionDelta(player, number1, number2)
        if ((isInitialPosition(player, number1) && delta !in 1..2)
            || (!isInitialPosition(player, number1) && delta != 1)
        ) {
            return false
        }

        for (i in 1..delta) {
            val numberDestination = if (player == 1) {
                number1 + i
            } else {
                number1 - i
            }
            if (!isPositionFree(board, letter2, numberDestination)) {
                return false
            }
        }
        return true
    }

    @Suppress("DuplicatedCode")
    fun isCapture(board: Array<CharArray>, player: Int, command: String): Boolean {
        val (letter1, number1) = getPosition(command[0], command[1])
        val (letter2, number2) = getPosition(command[2], command[3])
        if (abs(number2 - number1) != 1 || abs(letter2 - letter1) != 1) {
            return false
        }

        val playerOppositeSymbol =
            if (player == 1) GameOption.LETTER_SECOND_PLAYER else GameOption.LETTER_FIRST_PLAYER
        if (playerOppositeSymbol != getContentAtPosition(board, letter2, number2)) {
            return false
        }
        return true
    }

    @Suppress("DuplicatedCode")
    fun isCaptureEnPassant(board: Array<CharArray>, player: Int, command: String, previsionCommand: String): Boolean {
        if (previsionCommand.isEmpty()) {
            return false
        }
        val numberPrevision1 = getPosition(previsionCommand[0], previsionCommand[1]).second
        val numberPrevision2 = getPosition(previsionCommand[2], previsionCommand[3]).second
        val playerOpposite = if (player == 1) 2 else 1
        if (!isInitialPosition(playerOpposite, numberPrevision1)
            || getPositionDelta(playerOpposite, numberPrevision1, numberPrevision2) != 2
        ) {
            return false
        }

        val (letter1, number1) = getPosition(command[0], command[1])
        val (letter2, number2) = getPosition(command[2], command[3])
        if (abs(number2 - number1) != 1 || abs(letter2 - letter1) != 1) {
            return false
        }

        val playerOppositeSymbol =
            if (player == 1) GameOption.LETTER_SECOND_PLAYER else GameOption.LETTER_FIRST_PLAYER
        val number = if (player == 1) {
            number2 - 1
        } else {
            number2 + 1
        }
        if (playerOppositeSymbol != getContentAtPosition(board, letter2, number)) {
            return false
        }
        throw EnPassantException()
    }

    fun setContentAtPosition(board: Array<CharArray>, letter: Char, number: Char, symbol: Char) {
        val (x, y) = getPosition(letter, number)
        board[y][x] = symbol
    }

    fun getContentAtPosition(board: Array<CharArray>, letter: Char, number: Char): Char {
        val (x, y) = getPosition(letter, number)
        return getContentAtPosition(board, x, y)
    }

    fun getPosition(letter: Char, number: Char): Pair<Int, Int> {
        return Pair(letter - 'a', number.digitToInt() - 1)
    }

    fun getCountOfPawns(board: Array<CharArray>, player: Int): Int {
        val symbol = if (player == 1) GameOption.LETTER_FIRST_PLAYER else GameOption.LETTER_SECOND_PLAYER
        var count = 0
        for (i in board.indices) {
            count += board[i].count { it == symbol }
        }
        return count
    }

    fun getContentAtPosition(board: Array<CharArray>, x: Int, y: Int): Char {
        return board[y][x]
    }

    private fun isInitialPosition(player: Int, numberSource: Int): Boolean {
        return (player == 1 && numberSource == GameOption.INITIAL_RANK_OF_WHITE)
                || (player == 2 && numberSource == GameOption.INITIAL_RANK_OF_BLACK)
    }

    private fun isPositionFree(board: Array<CharArray>, letterDestination: Int, numberDestination: Int): Boolean {
        val content = getContentAtPosition(board, letterDestination, numberDestination)
        return content == GameOption.SPACE_SYMBOL
    }

    private fun getPositionDelta(player: Int, numberSource: Int, numberDestination: Int): Int {
        return if (player == 1) {
            numberDestination - numberSource
        } else {
            numberSource - numberDestination
        }
    }

}
