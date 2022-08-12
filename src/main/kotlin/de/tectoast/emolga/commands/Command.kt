package de.tectoast.emolga.commands

import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.RowData
import com.google.common.reflect.ClassPath
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.bot.EmolgaMain.flegmonjda
import de.tectoast.emolga.buttons.ButtonListener
import de.tectoast.emolga.buttons.buttonsaves.MonData
import de.tectoast.emolga.buttons.buttonsaves.Nominate
import de.tectoast.emolga.buttons.buttonsaves.PrismaTeam
import de.tectoast.emolga.buttons.buttonsaves.TrainerData
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.commands.Command.Companion.sendToMe
import de.tectoast.emolga.commands.CommandCategory.Companion.order
import de.tectoast.emolga.database.Database.Companion.incrementPredictionCounter
import de.tectoast.emolga.modals.ModalListener
import de.tectoast.emolga.selectmenus.MenuListener
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.Constants.ASLID
import de.tectoast.emolga.utils.Constants.CALENDAR_MSGID
import de.tectoast.emolga.utils.Constants.CALENDAR_TCID
import de.tectoast.emolga.utils.Constants.CULTID
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.MYSERVER
import de.tectoast.emolga.utils.Constants.MYTAG
import de.tectoast.emolga.utils.Constants.NDSID
import de.tectoast.emolga.utils.Constants.SOULLINK_MSGID
import de.tectoast.emolga.utils.Constants.SOULLINK_TCID
import de.tectoast.emolga.utils.annotations.ToTest
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.Tierlist.Companion.getByGuild
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.draft.*
import de.tectoast.emolga.utils.music.GuildMusicManager
import de.tectoast.emolga.utils.records.CalendarEntry
import de.tectoast.emolga.utils.records.DeferredSlashResponse
import de.tectoast.emolga.utils.records.TypicalSets
import de.tectoast.emolga.utils.showdown.Analysis
import de.tectoast.emolga.utils.showdown.Player
import de.tectoast.emolga.utils.sql.managers.*
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import de.tectoast.jsolf.JSONTokener
import de.tectoast.toastilities.repeat.RepeatTask
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.internal.utils.Helpers
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.*
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.random.Random

@Suppress("unused", "SameParameterValue")
abstract class Command(
    /**
     * The name of the command, used to check if the command was used in [.check]
     */
    val name: String,
    /**
     * The help string of the command, shown in the help messages by the bot
     */
    val help: String,
    /**
     * The [CommandCategory] which this command is in
     */
    val category: CommandCategory?, vararg guilds: Long
) {
    /**
     * List containing guild ids where this command is enabled, empty if it is enabled in all guilds
     */
    private val allowedGuilds: List<Long>

    /**
     * Set containing all aliases of this command
     */
    val aliases: MutableSet<String> = mutableSetOf()

    /**
     * HashMap containing a help for a guild id which should be shown for this command on that guild instead of the [.help]
     */
    private val overrideHelp: Map<Long, String> = HashMap()

    /**
     * HashMap containing a channel list which should be used for this command instead of [.emolgaChannel]
     */
    protected val overrideChannel: MutableMap<Long, MutableList<Long>> = HashMap()

    /**
     * True if this command should use the `e!` instead of `!`
     */
    protected var otherPrefix = false

    /**
     * The ArgumentManagerTemplate of this command, used for checking arguments
     */
    lateinit var argumentTemplate: ArgumentManagerTemplate

    /**
     * If true, this command is only allowed for me because I'm working on it
     */
    var wip = false

    /**
     * If true, sends a beta information when using this command
     */
    protected var beta = false

    /**
     * True if this command should bypass all channel restrictions
     */
    protected var everywhere = false

    /**
     * Predicate which checks if a member is allowed to use this command, ignored if [.customPermissions] is false
     */
    private var allowsMember: Predicate<Member> = Predicate { false }

    /**
     * True if this command should not use the permissions of the CommandCategory but [.allowsMember] to test if a user is allowed to use the command
     */
    private var customPermissions = false

    /**
     * If true, this command is disabled and cannot be used
     */
    var disabled = false
    protected var allowedBotId: Long = -1
    var isSlash = false
    protected var onlySlash = false
    val slashGuilds: MutableSet<Long> = HashSet()
    val childCommands: MutableMap<String, Command> = LinkedHashMap()

    init {
        allowedGuilds = guilds.toList()
    }

    constructor(name: String, help: String) : this(name, help, null)

    private fun addToMap() {
        val prefix = if (otherPrefix) "e!" else "!"
        commands[prefix + name] = this
        aliases.forEach { commands[prefix + it] = this }
        if (!this::argumentTemplate.isInitialized) {
            logger.error("{} has no argument manager!", this::class.qualifiedName)
            uninitializedCommands.add(this::class.qualifiedName ?: "ERROR")
        }
    }

    private fun addToChildren(c: Command) {
        c.childCommands[name] = this
    }

    protected fun setCustomPermissions(predicate: Predicate<Member>) {
        allowsMember = predicate.or { it.idLong == FLOID }
        customPermissions = true
    }

    protected fun addCustomChannel(guildId: Long, vararg channelIds: Long) {
        val l = overrideChannel.computeIfAbsent(guildId) { mutableListOf() }
        for (channelId in channelIds) {
            l.add(channelId)
        }
    }

    protected fun disable() {
        disabled = true
    }

    protected fun slash(onlySlash: Boolean, vararg guilds: Long) {
        this.onlySlash = onlySlash
        isSlash = true
        slashGuilds.addAll(guilds.toList())
        slashGuilds.add(MYSERVER)
    }

    protected fun slash() {
        this.slash(false)
    }

    protected fun wip() {
        wip = true
    }

    protected fun beta() {
        beta = true
    }

    fun allowsMember(mem: Member): Boolean {
        return category!!.allowsMember(mem) && (!customPermissions || allowsMember.test(mem))
    }

    fun allowsGuild(g: Guild): Boolean {
        return allowsGuild(g.idLong)
    }

    private fun allowsGuild(gid: Long): Boolean {
        if (gid == MYSERVER) return true
        return if (allowedGuilds.isEmpty() && category!!.allowsGuild(gid)) true else allowedGuilds.isNotEmpty() && allowedGuilds.contains(
            gid
        )
    }

    fun checkPermissions(gid: Long, mem: Member): PermissionCheck {
        if (!allowsGuild(gid)) return PermissionCheck.GUILD_NOT_ALLOWED
        return if (!allowsMember(mem)) PermissionCheck.PERMISSION_DENIED else PermissionCheck.GRANTED
    }

    fun checkBot(jda: JDA, guildid: Long): Boolean {
        return allowedBotId == -1L || allowedBotId == jda.selfUser.idLong || guildid == CULTID
    }

    val prefix: String
        get() = if (otherPrefix) "e!" else "!"

    /**
     * Abstract method, which is called on the subclass with the corresponding command when the command was received
     *
     * @param e A GuildCommandEvent containing the information about the command
     * @throws Exception Every exception that can be thrown in any command
     */
    @Throws(Exception::class)
    abstract suspend fun process(e: GuildCommandEvent)


    fun getHelp(g: Guild?): String {
        val args = argumentTemplate //?: return "`" + prefix + name + "` " + overrideHelp.getOrDefault(g.idLong, help)
        return buildString {
            append("`")
            append(
                (if (args.hasSyntax()) args.syntax else prefix + name + (if (args.arguments.isNotEmpty()) " " else "") + args.arguments.joinToString(
                    " "
                ) { "${if (it.isOptional) "[" else "<"}${it.name}${if (it.isOptional) "]" else ">"}" })
            )
            append("` ")
            append(
                (overrideHelp[g?.idLong ?: -1] ?: help)
            )
            append(if (wip) " (**W.I.P.**)" else "")
        }
    }

    fun getHelpWithoutCmd(g: Guild): String {
        return overrideHelp.getOrDefault(g.idLong, help) + if (wip) " (**W.I.P.**)" else ""
    }

    enum class Bot(val jDA: JDA) {
        EMOLGA(emolgajda), FLEGMON(flegmonjda);

        companion object {
            fun byJDA(jda: JDA): Bot {
                for (value in values()) {
                    if (jda.selfUser.idLong == value.jDA.selfUser.idLong) {
                        return value
                    }
                }
                throw IllegalStateException("Bot not found by user ${jda.selfUser.name}")
            }
        }
    }

    enum class PermissionCheck {
        GRANTED, PERMISSION_DENIED, GUILD_NOT_ALLOWED
    }

    interface ArgumentType {
        fun validate(str: String, vararg params: Any): Any?
        fun getName(): String
        val customHelp: String?
            get() = null

        fun asOptionType(): OptionType
        fun needsValidate(): Boolean
        fun hasAutoComplete(): Boolean {
            return false
        }

        fun autoCompleteList(arg: String, event: CommandAutoCompleteInteractionEvent): List<String>? {
            return null
        }
    }

    object PermissionPreset {
        val CULT = fromRole(781457314846343208L)
        val EMOLGAMOD = fromIDs(Constants.DASORID)
        fun fromRole(roleId: Long): Predicate<Member> {
            return Predicate { it.roles.any { r -> r.idLong == roleId } }
        }

        fun fromIDs(vararg ids: Long): Predicate<Member> {
            return Predicate { ids.any { l: Long -> it.idLong == l } }
        }
    }

    fun hasChildren(): Boolean {
        return childCommands.isNotEmpty()
    }

    open class ArgumentException : Exception {
        var argument: ArgumentManagerTemplate.Argument? = null
        var subcommands: Set<String>? = null

        constructor(argument: ArgumentManagerTemplate.Argument) {
            this.argument = argument
        }

        constructor(subcommands: Set<String>) {
            this.subcommands = subcommands
        }

        val isSubCmdMissing: Boolean
            get() = subcommands != null
    }

    class MissingArgumentException : ArgumentException {
        constructor(argument: ArgumentManagerTemplate.Argument) : super(argument)
        constructor(subcommands: Set<String>) : super(subcommands)
    }

    class ArgumentManager(val map: MutableMap<String, Any>, val executor: Command) {

        inline operator fun <reified T> get(key: String): T {
            return map[key] as T
        }

        fun getMember(key: String): Member {
            return map[key] as Member
        }

        fun getTranslation(key: String): Translation {
            return map[key] as Translation
        }

        fun getText(key: String): String {
            return map[key] as String
        }

        fun isText(key: String, text: String): Boolean {
            return map.getOrDefault(key, "") == text
        }

        fun isTextIgnoreCase(key: String, text: String): Boolean {
            return (map.getOrDefault(key, "") as String).equals(text, ignoreCase = true)
        }

        fun has(key: String): Boolean {
            return map.containsKey(key)
        }

        fun <T> getOrDefault(key: String, defvalue: T): T {
            @Suppress("UNCHECKED_CAST") return map.getOrDefault(key, defvalue) as T
        }

        fun getID(key: String): Long {
            return map[key] as Long
        }

        fun getInt(key: String): Int {
            return map[key] as Int
        }

        fun getLong(key: String): Long {
            val o: Any = map[key]!!
            return if (o is Int) o.toLong() else o as Long
        }

        fun getChannel(key: String): TextChannel {
            return map[key] as TextChannel
        }

        fun getAttachment(key: String): Attachment {
            return map[key] as Attachment
        }
    }

    class SubCommand(val name: String, val help: String) {

        companion object {
            @JvmOverloads
            fun of(name: String, help: String? = null): SubCommand {
                return SubCommand(name, help ?: "")
            }
        }
    }

    class ArgumentManagerTemplate {
        private val noCheck: Boolean
        val arguments: List<Argument>
        var example: String? = null
            private set
        var syntax: String? = null
            private set

        private constructor(arguments: List<Argument>, noCheck: Boolean, example: String?, syntax: String?) {
            this.arguments = arguments
            this.noCheck = noCheck
            this.example = example
            this.syntax = syntax
        }

        private constructor() {
            noCheck = true
            arguments = mutableListOf()
        }

        fun find(name: String): Argument? {
            return arguments.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

        fun hasExample(): Boolean {
            return example != null
        }

        fun hasSyntax(): Boolean {
            return syntax != null
        }

        @Throws(ArgumentException::class)
        fun construct(e: SlashCommandInteractionEvent, c: Command): ArgumentManager {
            val map: MutableMap<String, Any> = HashMap()
            if (c.hasChildren()) {
                val childCmd = c.childCommands[e.subcommandName]
                return childCmd!!.argumentTemplate.construct(e, childCmd)
            } else if (arguments.isNotEmpty() && arguments[0].type.asOptionType() == OptionType.SUB_COMMAND) {
                map[arguments[0].id] = e.subcommandName!!
            } else {
                for (arg in arguments) {
                    if (e.getOption(arg.name.lowercase()) == null && !arg.isOptional) {
                        throw MissingArgumentException(arg)
                    }
                    val type = arg.type
                    val o = e.getOption(arg.name.lowercase())
                    if (o != null) {
                        map[arg.id] =
                            if (type.needsValidate()) {
                                type.validate(o.asString, arg.language) ?: throw MissingArgumentException(arg)
                            } else {
                                when (o.type) {
                                    OptionType.ROLE -> o.asRole
                                    OptionType.CHANNEL -> o.asChannel
                                    OptionType.USER -> o.asUser
                                    OptionType.INTEGER -> o.asLong
                                    OptionType.BOOLEAN -> o.asBoolean
                                    else -> o.asString
                                }
                            }
                    }
                }
            }
            return ArgumentManager(map, c)
        }

        @Throws(ArgumentException::class)
        fun construct(e: MessageReceivedEvent, c: Command): ArgumentManager {
            val m = e.message
            val mid = m.idLong
            val raw = m.contentRaw
            val split = WHITESPACES_SPLITTER.split(raw).toMutableList()
            split.removeAt(0)
            val map: MutableMap<String, Any> = HashMap()
            if (c.hasChildren()) {
                if (split.isEmpty()) {
                    throw MissingArgumentException(c.childCommands.keys)
                }
                val sc = c.childCommands[split.removeAt(0)] ?: throw MissingArgumentException(c.childCommands.keys)
                return sc.argumentTemplate.construct(e, sc)
            } else if (split.size > 0 && arguments.isNotEmpty() && arguments[0].type.asOptionType() == OptionType.SUB_COMMAND) {
                map[arguments[0].id] = split[0]
                return ArgumentManager(map, c)
            }
            if (noCheck) return ArgumentManager(mutableMapOf(), c)
            val asFar: MutableMap<ArgumentType, Int> = HashMap()
            var argumentI = 0
            var i = 0
            while (i < split.size) {
                if (i >= arguments.size) {
                    break
                }
                if (argumentI >= arguments.size) break
                val a = arguments[argumentI]
                if (a.disabled.contains(mid)) break
                val str =
                    if (argumentI + 1 == arguments.size) split.subList(i, split.size).joinToString(" ") else split[i]
                val type = a.type
                var o: Any?
                if (type is DiscordType || type is DiscordFile) {
                    o = type.validate(str, m, asFar.getOrDefault(type, 0))
                    if (o != null) asFar[type] = asFar.getOrDefault(type, 0) + 1
                } else {
                    o = type.validate(str, a.language)
                    if (o == null) {
                        var b = true
                        for (j in argumentI + 1 until arguments.size) {
                            if (!arguments[j].isOptional) {
                                b = false
                                break
                            }
                        }
                        if (b && arguments.size > argumentI + 1) {
                            arguments[argumentI + 1].disabled.add(mid)
                        }
                        if (b) {
                            o = type.validate(split.subList(i, split.size).joinToString(" "), a.language)
                        }
                    }
                }
                if (o == null) {
                    if (!a.isOptional) {
                        clearDisable(mid)
                        throw MissingArgumentException(a)
                    }
                } else {
                    map[a.id] = o
                    i++
                }
                argumentI++
            }
            clearDisable(mid)
            if (map.isEmpty()) {
                arguments.firstOrNull { !it.isOptional }?.let { throw MissingArgumentException(it) }
            }
            return ArgumentManager(map, c)
        }

        private fun clearDisable(l: Long) {
            arguments.forEach { it.disabled.remove(l) }
        }

        enum class DiscordType(
            private val pattern: Pattern,
            private val typeName: String,
            private val female: Boolean,
            private val optionType: OptionType
        ) : ArgumentType {
            USER(
                Pattern.compile("<@!*\\d{18,22}>"), "User", false, OptionType.USER
            ),
            CHANNEL(Pattern.compile("<#*\\d{18,22}>"), "Channel", false, OptionType.CHANNEL), ROLE(
                Pattern.compile("<@&*\\d{18,22}>"), "Rolle", true, OptionType.ROLE
            ),
            ID(Pattern.compile("\\d{18,22}"), "ID", true, OptionType.STRING), INTEGER(
                Pattern.compile("\\d{1,9}"), "Zahl", true, OptionType.INTEGER
            );

            override fun validate(str: String, vararg params: Any): Any? {
                if (pattern.matcher(str).find()) {
                    if (mentionable()) {
                        val m = params[0] as Message
                        val soFar = params[1] as Int
                        val mentions = m.mentions
                        return if (this == USER) mentions.members[soFar] else if (this == CHANNEL) mentions.channels[soFar] else mentions.roles[soFar]
                    }
                    if (this == ID) return str.toLong()
                    if (this == INTEGER) return str.toInt()
                }
                return null
            }

            private fun mentionable(): Boolean {
                return this == USER || this == CHANNEL || this == ROLE
            }

            override fun getName(): String {
                return "ein" + (if (female) "e" else "") + " **" + typeName + "**"
            }

            override fun asOptionType(): OptionType {
                return optionType
            }

            override fun needsValidate(): Boolean {
                return false
            }
        }

        object ArgumentBoolean : ArgumentType {
            override fun validate(str: String, vararg params: Any) = str.toBoolean()

            override fun getName() = "Wahrheitswert"

            override fun asOptionType() = OptionType.BOOLEAN

            override fun needsValidate() = false

        }

        class Text : ArgumentType {
            private val texts: MutableList<SubCommand> = mutableListOf()
            private val any: Boolean
            private val slashSubCmd: Boolean
            private var mapper = Function { s: String -> s }

            private constructor(possible: List<SubCommand>, slashSubCmd: Boolean) {
                texts.addAll(possible)
                any = false
                this.slashSubCmd = slashSubCmd
            }

            private constructor() {
                any = true
                slashSubCmd = false
            }

            fun setMapper(mapper: Function<String, String>): Text {
                this.mapper = mapper
                return this
            }

            override fun validate(str: String, vararg params: Any): Any? {
                return if (any) str else texts.asSequence().map { obj: SubCommand -> obj.name }
                    .filter { scName: String -> scName.equals(str, ignoreCase = true) }.map { mapper.apply(it) }
                    .firstOrNull()
            }

            override fun getName(): String {
                return "ein Text"
            }

            override val customHelp: String?
                get() = if (any) null else texts.joinToString("\n") { sc: SubCommand -> "`" + sc.name + "`" + sc.help }

            override fun asOptionType(): OptionType {
                return if (slashSubCmd) OptionType.SUB_COMMAND else OptionType.STRING
            }

            override fun needsValidate(): Boolean {
                return true
            }

            override fun hasAutoComplete(): Boolean {
                return !(slashSubCmd || texts.isEmpty())
            }

            override fun autoCompleteList(arg: String, event: CommandAutoCompleteInteractionEvent): List<String> {
                return texts.asSequence().filter { c: SubCommand -> c.name.lowercase().startsWith(arg) }
                    .map { obj: SubCommand -> obj.name }.toList()
            }

            fun asSubCommandData(): List<SubcommandData> {
                check(slashSubCmd) { "Cannot call asSubCommandData on no SubCommand" }
                return texts.map { SubcommandData(it.name.lowercase(), it.help) }
            }

            companion object {
                fun of(vararg possible: SubCommand): Text {
                    return of(possible.toList(), false)
                }

                fun of(possible: List<SubCommand>, slashSubCmd: Boolean): Text {
                    return Text(possible, slashSubCmd)
                }

                fun any(): Text {
                    return Text()
                }
            }
        }

        class Number private constructor(possible: Array<Int>) : ArgumentType {
            private val numbers = mutableListOf<Int>()
            private val any: Boolean
            private var from = 0
            private var to = 0
            private var hasRange: Boolean

            init {
                if (possible.isEmpty()) {
                    any = true
                    hasRange = false
                } else {
                    numbers.addAll(listOf(*possible))
                    any = false
                    hasRange = false
                }
            }

            private fun setRange(from: Int, to: Int): Number {
                this.from = from
                this.to = to
                hasRange = true
                return this
            }

            private fun hasRange(): Boolean {
                return hasRange
            }

            override fun validate(str: String, vararg params: Any): Any? {
                if (Helpers.isNumeric(str)) {
                    val num = str.toInt()
                    if (any) return num
                    return if (numbers.contains(num)) num else null
                }
                return null
            }

            override fun getName(): String {
                return "ein Text"
            }

            override val customHelp: String
                get() = if (hasRange()) {
                    "$from-$to"
                } else numbers.joinToString()

            override fun asOptionType(): OptionType {
                return OptionType.INTEGER
            }

            override fun needsValidate(): Boolean {
                return true
            }

            companion object {
                fun range(from: Int, to: Int): Number {
                    val l = mutableListOf<Int>()
                    for (i in from..to) {
                        l.add(i)
                    }
                    return of(*l.toTypedArray().toIntArray()).setRange(from, to)
                }

                fun of(vararg possible: Int): Number {
                    return Number(possible.toTypedArray())
                }

                fun any(): Number = Number(emptyArray())
            }
        }

        class DiscordFile(private val fileType: String) : ArgumentType {
            override fun validate(str: String, vararg params: Any): Any? {
                val att = (params[0] as Message).attachments
                if (att.size == 0) return null
                val a = att[(params[1] as Int)]
                return if (a.fileName.endsWith(".$fileType") || fileType == "*") {
                    a
                } else null
            }

            override fun getName(): String {
                return "eine " + (if (fileType == "*") "" else "$fileType-") + "Datei"
            }

            override fun asOptionType(): OptionType {
                return OptionType.ATTACHMENT
            }

            override fun needsValidate(): Boolean {
                return false
            }

            companion object {
                fun of(fileType: String): DiscordFile {
                    return DiscordFile(fileType)
                }
            }
        }

        class Argument(
            val id: String,
            val name: String,
            val helpmsg: String,
            val type: ArgumentType,
            val isOptional: Boolean,
            val language: Translation.Language,
            val customErrorMessage: String?
        ) {
            val disabled: MutableList<Long> = mutableListOf()

            fun buildHelp(): String {
                val b = StringBuilder(helpmsg)
                if (type.customHelp == null) return b.toString()
                if (helpmsg.isNotEmpty()) {
                    b.append("\n")
                }
                return b.append("Möglichkeiten:\n").append(type.customHelp).toString()
            }

            fun hasCustomErrorMessage(): Boolean {
                return customErrorMessage != null
            }
        }

        class Builder {
            private val arguments: MutableList<Argument> = mutableListOf()
            var noCheck = false
            var example: String? = null
            var customDescription: String? = null

            @JvmOverloads
            fun add(
                id: String,
                name: String,
                help: String,
                type: ArgumentType,
                optional: Boolean = false,
                customErrorMessage: String? = null
            ): Builder {
                arguments.add(Argument(id, name, help, type, optional, Translation.Language.GERMAN, customErrorMessage))
                return this
            }

            @JvmOverloads
            fun addEngl(
                id: String,
                name: String,
                help: String,
                type: ArgumentType,
                optional: Boolean = false,
                customErrorMessage: String? = null
            ): Builder {
                arguments.add(
                    Argument(
                        id, name, help, type, optional, Translation.Language.ENGLISH, customErrorMessage
                    )
                )
                return this
            }

            fun setNoCheck(noCheck: Boolean): Builder {
                this.noCheck = noCheck
                return this
            }

            fun setExample(example: String?): Builder {
                this.example = example
                return this
            }

            fun setCustomDescription(customDescription: String?): Builder {
                this.customDescription = customDescription
                return this
            }

            fun build(): ArgumentManagerTemplate {
                return ArgumentManagerTemplate(arguments, noCheck, example, customDescription)
            }
        }

        companion object {
            private val noCheckTemplate: ArgumentManagerTemplate = ArgumentManagerTemplate()

            fun builder(): Builder {
                return Builder()
            }

            fun create(b: Builder.() -> Unit) = Builder().apply(b).build()

            fun noArgs(): ArgumentManagerTemplate {
                return noCheckTemplate
            }

            fun noSpecifiedArgs(syntax: String, example: String): ArgumentManagerTemplate {
                return ArgumentManagerTemplate(mutableListOf(), true, example, syntax)
            }

            fun draft(): ArgumentType {
                return withPredicate(
                    "Draftname", { it in Emolga.get.drafts }, false
                )
            }

            @JvmOverloads
            fun draftPokemon(autoComplete: BiFunction<String, CommandAutoCompleteInteractionEvent, List<String>?>? = null): ArgumentType {
                return withPredicate("Pokemon", { s: String ->
                    getDraftGerName(s).isFromType(Translation.Type.POKEMON)
                }, false, draftnamemapper, autoComplete)
            }

            fun withPredicate(name: String, check: Predicate<String>, female: Boolean): ArgumentType {
                return object : ArgumentType {
                    override fun validate(str: String, vararg params: Any): Any? {
                        return if (check.test(str)) str else null
                    }

                    override fun getName(): String {
                        return "ein" + (if (female) "e" else "") + " **" + name + "**"
                    }

                    override fun asOptionType(): OptionType {
                        return OptionType.STRING
                    }

                    override fun needsValidate(): Boolean {
                        return true
                    }
                }
            }

            private fun withPredicate(
                name: String,
                check: Predicate<String>,
                female: Boolean,
                mapper: Function<String, String>,
                autoComplete: BiFunction<String, CommandAutoCompleteInteractionEvent, List<String>?>?
            ): ArgumentType {
                return object : ArgumentType {
                    override fun validate(str: String, vararg params: Any): Any? {
                        return if (check.test(str)) {
                            mapper.apply(str)
                        } else null
                    }

                    override fun getName(): String {
                        return "ein" + (if (female) "e" else "") + " **" + name + "**"
                    }

                    override fun asOptionType(): OptionType {
                        return OptionType.STRING
                    }

                    override fun needsValidate(): Boolean {
                        return true
                    }

                    override fun hasAutoComplete(): Boolean {
                        return autoComplete != null
                    }

                    override fun autoCompleteList(
                        arg: String, event: CommandAutoCompleteInteractionEvent
                    ): List<String>? {
                        return autoComplete!!.apply(arg, event)
                    }
                }
            }
        }
    }

    class Translation {
        val type: Type
        val language: Language
        val translation: String
        val isEmpty: Boolean
        val otherLang: String
        val forme: String?

        constructor(translation: String, type: Type, language: Language) {
            this.translation = translation
            this.type = type
            this.language = language
            isEmpty = type == Type.UNKNOWN
            otherLang = ""
            forme = null
        }

        constructor(translation: String, type: Type, language: Language, otherLang: String) {
            this.translation = translation
            this.type = type
            this.language = language
            this.otherLang = otherLang
            isEmpty = false
            forme = null
        }

        constructor(translation: String, type: Type, language: Language, otherLang: String, forme: String?) {
            this.translation = translation
            this.type = type
            this.language = language
            this.otherLang = otherLang
            this.forme = forme
            isEmpty = false
        }

        override fun toString(): String {
            val sb = StringBuilder("Translation{")
            sb.append("type=").append(type)
            sb.append(", language=").append(language)
            sb.append(", translation='").append(translation).append('\'')
            sb.append(", empty=").append(isEmpty)
            sb.append(", otherLang='").append(otherLang).append('\'')
            sb.append('}')
            logger.info("ToStringCheck {}", sb)
            return sb.toString()
        }

        fun print() {
            logger.info(
                "Translation{type=$type, language=$language, translation='$translation', empty=$isEmpty, otherLang='$otherLang'}"
            )
        }

        fun append(str: String): Translation {
            return Translation(translation + str, type, language, otherLang, forme)
        }

        fun before(str: String): Translation {
            return Translation(str + translation, type, language, otherLang, forme)
        }

        fun isFromType(type: Type): Boolean {
            return this.type == type
        }

        val isSuccess: Boolean
            get() = !isEmpty

        enum class Language {
            GERMAN, ENGLISH, UNKNOWN
        }

        enum class Type(val id: String, private val typeName: String, private val female: Boolean) : ArgumentType {
            ABILITY("abi", "Fähigkeit", true), EGGGROUP("egg", "Eigruppe", true), ITEM(
                "item", "Item", false
            ),
            MOVE("atk", "Attacke", true), NATURE("nat", "Wesen", false), POKEMON("pkmn", "Pokémon", false), TYPE(
                "type", "Typ", false
            ),
            TRAINER("trainer", "Trainer", false), UNKNOWN("unknown", "Undefiniert", false);

            fun or(name: String): ArgumentType {
                return object : ArgumentType {
                    override fun validate(str: String, vararg params: Any): Any? {
                        return if (str == name) Translation(
                            "Tom", TRAINER, Language.GERMAN, "Tom"
                        ) else this@Type.validate(str, *params)
                    }

                    override fun getName(): String {
                        return this@Type.getName()
                    }

                    override val customHelp: String?
                        get() = this@Type.customHelp

                    override fun asOptionType(): OptionType {
                        return this@Type.asOptionType()
                    }

                    override fun needsValidate(): Boolean {
                        return this@Type.needsValidate()
                    }
                }
            }

            override fun needsValidate(): Boolean {
                return true
            }

            override fun validate(str: String, vararg params: Any): Any? {
                val t = if (params.isEmpty() || params[0] === Language.GERMAN) getGerName(
                    str,
                    false
                ) else getEnglNameWithType(str)
                if (t.isEmpty) return null
                if (t.translation == "Psychic" || t.otherLang == "Psychic") {
                    if (this == TYPE) {
                        return Translation(if (t.language == Language.GERMAN) "Psycho" else "Psychic", TYPE, t.language)
                    }
                }
                return if (t.type != this) null else t
            }

            override fun getName(): String {
                return "ein" + (if (female) "e" else "") + " **" + typeName + "**"
            }

            override fun asOptionType(): OptionType {
                return OptionType.STRING
            }

            companion object {
                fun fromId(id: String): Type {
                    return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UNKNOWN
                }

                fun of(vararg types: Type): ArgumentType {
                    return object : ArgumentType {
                        override fun validate(str: String, vararg params: Any): Any? {
                            val t = if (params[0] === Language.GERMAN) getGerName(str) else getEnglNameWithType(str)
                            if (t.isEmpty) return null
                            return if (!listOf(*types).contains(t.type)) null else t
                        }

                        override fun getName(): String {
                            //return "ein **Pokémon**, eine **Attacke**, ein **Item** oder eine **Fähigkeit**";
                            return buildEnumeration(*types)
                        }

                        override fun asOptionType(): OptionType {
                            return OptionType.STRING
                        }

                        override fun needsValidate(): Boolean {
                            return true
                        }
                    }
                }

                fun all(): ArgumentType {
                    return of(*values().filter { t: Type -> t != UNKNOWN }.toTypedArray())
                }
            }
        }

        companion object {
            private val emptyTranslation: Translation = Translation("", Type.UNKNOWN, Language.UNKNOWN)

            fun empty(): Translation {
                return emptyTranslation
            }
        }
    }

    companion object {
        /**
         * NO PERMISSION Message
         */
        const val NOPERM = "Dafür hast du keine Berechtigung!"

        /**
         * List of all commands of the bot
         */
        val commands: MutableMap<String, Command> = HashMap()

        /**
         * List of guilds where the chill playlist is playing
         */
        val chill: MutableList<Guild> = ArrayList()

        /**
         * List of guilds where the deep playlist is playing
         */
        val deep: MutableList<Guild> = ArrayList()

        /**
         * List of guilds where the music playlist is playing
         */
        val music: MutableList<Guild> = ArrayList()

        /**
         * Saves the channel ids per server where emolgas commands work
         */
        val emolgaChannel: MutableMap<Long, MutableList<Long>> = HashMap()

        /**
         * Some pokemon extensions for serebii
         */
        private val serebiiex: MutableMap<String, String> = HashMap()

        /**
         * Some pokemon extensions for showdown
         */
        val sdex: MutableMap<String, String> = HashMap()


        /**
         * saves all channels where emoteSteal is enabled
         */
        val emoteSteal: MutableList<Long> = ArrayList()

        /**
         * saves all Muted Roles per guild
         */
        private val mutedRoles: MutableMap<Long, Long> = HashMap()

        /**
         * saves all Moderator Roles per guild
         */
        val moderatorRoles: MutableMap<Long, Long> = HashMap()

        /**
         * saves all replay channel with their result channel
         */
        val replayAnalysis: MutableMap<Long, Long> = HashMap()

        /**
         * saves all guilds where spoiler tags should be used in the showdown results
         */
        @JvmField
        val spoilerTags: MutableList<Long> = ArrayList()

        /**
         * Cache for german translations
         */
        val translationsCacheGerman: MutableMap<String, Translation> = HashMap()

        /**
         * Order of the cached items, used to delete the oldest after enough caching
         */
        val translationsCacheOrderGerman = mutableListOf<String>()

        /**
         * Cache for english translations
         */
        val translationsCacheEnglish: MutableMap<String, Translation> = HashMap()

        /**
         * Order of the cached items, used to delete the oldest after enough caching
         */
        val translationsCacheOrderEnglish = mutableListOf<String>()

        /**
         * MusicManagers for Lavaplayer
         */
        val musicManagers: MutableMap<Long, GuildMusicManager> = HashMap()
        private val playerManagers: MutableMap<Long, AudioPlayerManager> = HashMap()
        val trainerDataButtons: MutableMap<Long, TrainerData> = HashMap()
        val monDataButtons: MutableMap<Long, MonData> = HashMap()
        val nominateButtons: MutableMap<Long, Nominate> = HashMap()
        val smogonMenu: MutableMap<Long, SmogonSet> = HashMap()
        val prismaTeam: MutableMap<Long, PrismaTeam> = HashMap()
        private val customResult = emptyList<Long>()
        val clips: MutableMap<Long, CircularFifoQueue<ByteArray>> = HashMap()
        val uninitializedCommands: MutableList<String> = mutableListOf()


        @JvmStatic
        protected val soullinkIds = mapOf(
            448542640850599947 to "Pascal",
            726495601021157426 to "David",
            867869302808248341 to "Jesse",
            541214204926099477 to "Felix"
        )
        protected val soullinkNames = listOf("Pascal", "David", "Jesse", "Felix")
        private val logger = LoggerFactory.getLogger(Command::class.java)
        private val otherFormatRegex = Regex("(\\S+)-(Mega|Alola|Galar)")

        /**
         * Mapper for the DraftGerName
         */
        val draftnamemapper = Function { s: String -> getDraftGerName(s).translation }
        val typeIcons = load("typeicons.json")
        private val SD_NAME_PATTERN = Regex("[^a-zA-Z\\däöüÄÖÜß♂♀é+]+")
        private val DURATION_PATTERN = Regex("\\d{1,8}[smhd]?")
        private val DURATION_SPLITTER = Regex("[.|:]")
        val WHITESPACES_SPLITTER = Regex("\\s+")
        private val EMPTY_PATTERN = Regex("")
        private val HYPHEN = Regex("-")
        private val USERNAME_PATTERN = Regex("[^a-zA-Z\\d]+")
        private val CUSTOM_GUILD_PATTERN = Regex("(\\d{18,})")
        val TRIPLE_HASHTAG = Regex("###")
        const val DEXQUIZ_BUDGET = 10


        /**
         * Used for a shiny counter
         */
        lateinit var shinycountjson: JSONObject

        /**
         * JSONObject containing all credentials (Discord Token, Google OAuth Token)
         */
        lateinit var tokens: JSONObject
        lateinit var catchrates: JSONObject
        val replayCount = AtomicInteger()
        protected var lastClipUsed: Long = -1

        @JvmStatic
        protected var calendarService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
        protected val moderationService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
        protected val birthdayService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        const val BOT_DISABLED = false
        const val DISABLED_TEXT = "Ich befinde mich derzeit im Wartungsmodus, versuche es später noch einmal :)"

        @JvmStatic
        fun newCalendarService() {
            calendarService = Executors.newScheduledThreadPool(5)
        }


        @JvmStatic
        fun registerCommands() {
            val loader = Thread.currentThread().contextClassLoader
            try {
                for (classInfo in ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.commands")) {
                    val cl = classInfo.load()
                    if (cl.isInterface) continue
                    val name = cl.superclass.simpleName
                    if (name.endsWith("Command") && !Modifier.isAbstract(cl.modifiers)) {
                        val cmd = cl.constructors[0].newInstance()
                        if (cl.isAnnotationPresent(ToTest::class.java)) {
                            logger.warn("{} has to be tested!", cl.name)
                        }
                        if (cmd is Command) {
                            cmd.addToMap()
                            for (dc in cl.declaredClasses) {
                                if (dc.superclass.simpleName.endsWith("Command") && !Modifier.isAbstract(dc.modifiers)) {
                                    val ccmd = dc.constructors[0].newInstance()
                                    if (ccmd is Command) {
                                        ccmd.addToChildren(cmd)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getFirst(str: String): String {
            return str.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        fun getDataName(s: String): String {
            logger.info("s = $s")
            if (s == "Wie-Shu") return "mienshao"
            if (s == "Lin-Fu") return "mienfoo"
            if (s == "Porygon-Z") return "porygonz"
            if (s == "Sen-Long") return "drampa"
            if (s == "Ho-Oh") return "hooh"
            if (s.startsWith("Furnifra")) return "heatmor"
            if (s.startsWith("Kapu-")) return getSDName(s)
            val split = s.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size == 1) return getSDName(s)
            if (s.startsWith("M-")) {
                return if (split.size == 3) {
                    getSDName(split[1]) + "mega" + split[2].lowercase()
                } else getSDName(split[1]) + "mega"
            }
            if (s.startsWith("A-")) return getSDName(split[1]) + "alola"
            return if (s.startsWith("G-")) getSDName(split[1]) + "galar" else getSDName(
                split[0]
            ) + toSDName(
                sdex.getOrDefault(
                    s, ""
                )
            )
        }

        fun getFirstAfterUppercase(s: String): String {
            return if (!s.contains("-")) s else s[0].toString() + s.substring(1, 2).uppercase() + s.substring(2)
        }

        fun doMatchUps(gameday: Int) {
            val nds = Emolga.get.nds()
            val teamnames = nds.teamnames
            val battleorder = nds.battleorder[gameday]!!
            val b = RequestBuilder(nds.sid)
            for (battle in battleorder.split(";").dropLastWhile { it.isEmpty() }) {
                logger.info("battle = {}", battle)
                val users = battle.split(":")
                for (index in 0..1) {
                    println(users)
                    val u1 = users[index]
                    val u2 = users[1 - index]
                    val team = teamnames[u1.toLong()]!!
                    val oppo = teamnames[u2.toLong()]!!
                    b.addSingle("$team!B18", "={'$oppo'!B16:AE16}")
                    b.addSingle("$team!B19", "={'$oppo'!B15:AE15}")
                    b.addSingle("$team!B21", "={'$oppo'!B14:AF14}")
                    b.addColumn(
                        "$team!A18", listOf(
                            "='$oppo'!Y2", "='$oppo'!B2"
                        )
                    )
                }
            }
            b.withRunnable {
                emolgajda.getTextChannelById(837425690844201000L)!!.sendMessage(
                    "Jo, kurzer Reminder, die Matchups des nächsten Spieltages sind im Doc, vergesst das Nominieren nicht :)\n<@&856205147754201108>"
                ).queue()
            }.execute()
        }

        @JvmStatic
        @JvmOverloads
        fun doNDSNominate(prevDay: Boolean = false) {
            val nds = Emolga.get.nds()
            val nom = nds.nominations
            val teamnames = nds.teamnames
            val table = nds.teamtable
            var cday = nom.currentDay
            if (prevDay) cday--
            val o = nom.nominated[cday]!!
            val picks = nds.picks
            val sid = nds.sid
            val b = RequestBuilder(sid)
            val tiers = listOf("S", "A", "B")
            for (u in picks.keys) {
                //String u = "297010892678234114";
                if (u !in o) {
                    if (cday == 1) {
                        val mons = picks[u]!!
                        val comp = compareBy<DraftPokemon>({ it.tier.indexedBy(tiers) }, { it.name })
                        o[u] = (buildString {
                            append(mons.sortedWith(comp).take(11).joinToString(";") { it.name })
                            append("###")
                            append(mons.sortedWith(comp).drop(11).joinToString(";") { it.name })
                        })
                    } else {
                        o[u] = nom.nominated[cday - 1]!![u]!!
                    }
                }
                //logger.info("o.get(u) = " + o.get(u));
                val str = o[u]!!
                val mons: List<String> = str.replace("###", ";").split(";")

                logger.info("mons = $mons")
                logger.info("u = $u")
                val index = table.indexOf(teamnames[u])
                b.addColumn("Data!F${index.y(17, 2)}", mons)
            }
            b.withRunnable {
                emolgajda.getTextChannelById(837425690844201000L)!!.sendMessage(
                    """
                Jo ihr alten Zipfelklatscher! Eure jämmerlichen Versuche eine Liga zu gewinnen werden scheitern ihr Arschgeigen, da ihr zu inkompetent seid euch zu merken, wann alles nach meiner Schwerstarbeit automatisch eingetragen wird. Daher erinnere ich euer Erbsenhirn mithilfe dieser noch nett formulierten Nachricht daran, dass ihr nun anfangen könnt zu builden. Dann bis nächste Woche Mittwoch!
                PS: Bannt Henny, der Typ ist broken! Und gebt ihm keinen Gehstocktänzer!

                _written by Henny_""".trimIndent()
                ).queue()
            }.execute()
            if (!prevDay) nom.currentDay++
            saveEmolgaJSON()
        }

        fun invertImage(mon: String, shiny: Boolean): File {
            logger.info("mon = $mon")
            val f = File(
                "../Showdown/sspclient/sprites/gen5" + (if (shiny) "-shiny" else "") + "/" + mon.lowercase() + ".png"
            )
            logger.info("f.getAbsolutePath() = " + f.absolutePath)
            val inputFile = ImageIO.read(f)
            val width = inputFile.width
            val height = inputFile.height
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val rgba = inputFile.getRGB(x, y)
                    var col = java.awt.Color(rgba, true)
                    col = java.awt.Color(
                        255 - col.red, 255 - col.green, 255 - col.blue
                    )
                    inputFile.setRGB(x, y, col.rgb)
                }
            }
            val outputFile = File("tempimages/invert-$mon.png")
            ImageIO.write(inputFile, "png", outputFile)
            return outputFile
        }

        @JvmOverloads
        fun loadPlaylist(channel: TextChannel, track: String, mem: Member, cm: String?, random: Boolean = false) {
            val musicManager = getGuildAudioPlayer(channel.guild)
            logger.info(track)
            getPlayerManager(channel.guild).loadItemOrdered(musicManager, track, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {}
                override fun playlistLoaded(playlist: AudioPlaylist) {
                    val toplay = mutableListOf<AudioTrack>()
                    if (random) {
                        val list = ArrayList(playlist.tracks)
                        list.shuffle()
                        toplay.addAll(list)
                    } else {
                        toplay.addAll(playlist.tracks)
                    }
                    for (playlistTrack in toplay) {
                        play(channel.guild, musicManager, playlistTrack, mem, channel)
                    }
                    channel.sendMessage(cm ?: "Loaded playlist!").queue()
                }

                override fun noMatches() {
                    channel.sendMessage("Es wurde unter `$track` nichts gefunden!").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    exception.printStackTrace()
                    channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.message).queue()
                }
            })
        }

        @JvmStatic
        fun byName(name: String): Command? {
            return commands.values.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.aliases.any { alias ->
                    name.equals(
                        alias, ignoreCase = true
                    )
                }
            }
        }

        @Throws(IllegalArgumentException::class)
        fun loadAndPlay(channel: TextChannel, track: String, mem: Member, cm: String?) {/*if (track.startsWith("https://www.youtube.com/playlist")) {
            loadPlaylist(channel, track, mem, cm);
            return;
        }*/
            val musicManager = getGuildAudioPlayer(channel.guild)
            logger.info(track)
            val loader = YTDataLoader.create(track)
            if (loader == null) {
                channel.sendMessage("Der Track wurde nicht gefunden!").queue()
                return
            }
            val url = loader.url
            logger.info("url = $url")
            getPlayerManager(channel.guild).loadItemOrdered(musicManager, url, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    //logger.info("LOADED!");
                    if (cm == null) {
                        channel.sendMessageEmbeds(loader.buildEmbed(track, mem, musicManager)).queue()
                    } else {
                        channel.sendMessage(cm).queue()
                    }
                    play(channel.guild, musicManager, track, mem, channel)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    if (cm == null) {
                        channel.sendMessageEmbeds(loader.buildEmbed(playlist, mem, musicManager)).queue()
                    } else {
                        channel.sendMessage(cm).queue()
                    }
                    for (playlistTrack in playlist.tracks) {
                        play(channel.guild, musicManager, playlistTrack, mem, channel)
                    }
                }

                override fun noMatches() {
                    channel.sendMessage("Es wurde unter `$track` nichts gefunden!").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    exception.printStackTrace()
                    channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.message).queue()
                }
            })
        }

        fun playSound(vc: AudioChannel?, path: String?, tc: MessageChannelUnion) {
            val flegmon = vc!!.jda.selfUser.idLong != 723829878755164202L
            if (System.currentTimeMillis() - lastClipUsed < 10000 && flegmon) {
                tc.sendMessage("Warte bitte noch kurz...").queue()
                return
            }
            val g = vc.guild
            val gmm = getGuildAudioPlayer(g)
            getPlayerManager(g).loadItemOrdered(gmm, path, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    if (flegmon) lastClipUsed = System.currentTimeMillis()
                    play(g, gmm, track, vc)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {}
                override fun noMatches() {}
                override fun loadFailed(exception: FriendlyException) {
                    exception.printStackTrace()
                }
            })
        }

        fun removeFromJSONArray(arr: JSONArray, value: Any): Boolean {
            var success = false
            var i = 0
            while (i < arr.length()) {
                if (arr[i] == value) {
                    arr.remove(i)
                    success = true
                } else i++
            }
            return success
        }

        fun parseShortTime(timestring: String): Int {
            var timestr = timestring
            timestr = timestr.lowercase()
            if (!DURATION_PATTERN.matches(timestr)) return -1
            var multiplier = 1
            when (timestr[timestr.length - 1]) {
                'd' -> {
                    multiplier *= 24
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'h' -> {
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'm' -> {
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                's' -> timestr = timestr.substring(0, timestr.length - 1)
                else -> {}
            }
            return multiplier * timestr.toInt()
        }

        @JvmStatic
        fun secondsToTime(timesec: Long): String {
            var timeseconds = timesec
            val builder = StringBuilder(20)
            val years = (timeseconds / (60 * 60 * 24 * 365)).toInt()
            if (years > 0) {
                builder.append("**").append(years).append("** ").append(pluralise(years.toLong(), "Jahr", "Jahre"))
                    .append(", ")
                timeseconds %= (60 * 60 * 24 * 365)
            }
            val weeks = (timeseconds / (60 * 60 * 24 * 7)).toInt()
            if (weeks > 0) {
                builder.append("**").append(weeks).append("** ").append(pluralise(weeks.toLong(), "Woche", "Wochen"))
                    .append(", ")
                timeseconds %= (60 * 60 * 24 * 7)
            }
            val days = (timeseconds / (60 * 60 * 24)).toInt()
            if (days > 0) {
                builder.append("**").append(days).append("** ").append(pluralise(days.toLong(), "Tag", "Tage"))
                    .append(", ")
                timeseconds %= (60 * 60 * 24)
            }
            val hours = (timeseconds / (60 * 60)).toInt()
            if (hours > 0) {
                builder.append("**").append(hours).append("** ").append(pluralise(hours.toLong(), "Stunde", "Stunden"))
                    .append(", ")
                timeseconds %= (60 * 60)
            }
            val minutes = (timeseconds / 60).toInt()
            if (minutes > 0) {
                builder.append("**").append(minutes).append("** ")
                    .append(pluralise(minutes.toLong(), "Minute", "Minuten")).append(", ")
                timeseconds %= 60
            }
            if (timeseconds > 0) {
                builder.append("**").append(timeseconds).append("** ")
                    .append(pluralise(timeseconds, "Sekunde", "Sekunden"))
            }
            var str = builder.toString()
            if (str.endsWith(", ")) str = str.substring(0, str.length - 2)
            if (str.isEmpty()) str = "**0** Sekunden"
            return str
        }

        private fun pluralise(x: Long, singular: String, plural: String): String {
            return if (x == 1L) singular else plural
        }

        fun getGameDay(league: League, uid1: String, uid2: String): Int {
            return getGameDay(league, uid1.toLong(), uid2.toLong())
        }

        fun getGameDay(league: League, uid1: Long, uid2: Long): Int = league.battleorder.asIterable()
            .firstNotNullOfOrNull { if (it.value.contains("$uid1:$uid2") || it.value.contains("$uid2:$uid1")) it.key else null }
            ?: -1

        @JvmStatic
        fun getPicksAsList(arr: JSONArray): List<String> {
            return arr.toJSONList().map { it.getString("name") }
        }

        fun tempMute(tco: TextChannel, mod: Member, mem: Member, time: Int, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht muten!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            if (mutedRoles.containsKey(g.idLong)) g.addRoleToMember(
                mem, g.getRoleById(
                    mutedRoles[g.idLong]!!
                )!!
            ).queue()
            val expires = (System.currentTimeMillis() + time * 1000L) / 1000 * 1000
            muteTimer(g, expires, mem.idLong)
            val builder = EmbedBuilder()
            builder.setAuthor(
                mem.effectiveName + " wurde für " + secondsToTime(time.toLong()).replace(
                    "*", ""
                ) + " gemutet", null, mem.user.effectiveAvatarUrl
            )
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            //Database.insert("mutes", "userid, modid, guildid, reason, expires", mem.getIdLong(), mod.getIdLong(), g.getIdLong(), reason, new Timestamp(expires));
            MuteManager.mute(mem.idLong, mod.idLong, g.idLong, reason, Timestamp(expires))
        }

        fun muteTimer(g: Guild, expires: Long, mem: Long) {
            if (expires == -1L) return
            moderationService.schedule({
                val gid = g.idLong
                if (MuteManager.unmute(mem, gid) != 0) {
                    g.removeRoleFromMember(UserSnowflake.fromId(mem), g.getRoleById(mutedRoles[gid]!!)!!).queue()
                }
            }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        fun kick(tco: TextChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht kicken!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            if (mem.idLong == FLOID) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Das lässt du lieber bleiben :3")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            g.kick(mem, reason).queue()
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde gekickt", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            //Database.insert("kicks", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
            KicksManager.kick(mem.idLong, mod.idLong, tco.guild.idLong, reason)
        }

        fun ban(tco: TextChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht bannen!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            if (mem.idLong == FLOID) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Das lässt du lieber bleiben :3")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            g.ban(mem, 0, reason).queue()
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde gebannt", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            BanManager.ban(mem.idLong, mem.user.name, mod.idLong, g.idLong, reason, null)
        }

        fun mute(tco: TextChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            val gid = g.idLong
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht muten!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            if (mutedRoles.containsKey(gid)) {
                g.addRoleToMember(mem, g.getRoleById(mutedRoles[gid]!!)!!).queue()
            }
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde gemutet", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            MuteManager.mute(mem.idLong, mod.idLong, tco.guild.idLong, reason, null)
        }

        fun unmute(tco: TextChannel, mem: Member) {
            val g = tco.guild
            val gid = g.idLong
            if (mutedRoles.containsKey(gid)) {
                g.removeRoleFromMember(mem, g.getRoleById(mutedRoles[gid]!!)!!).queue()
            }
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde entmutet", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            tco.sendMessageEmbeds(builder.build()).queue()
            MuteManager.unmute(mem.idLong, gid)
        }

        fun tempBan(tco: TextChannel, mod: Member, mem: Member, time: Int, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht bannen!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            g.ban(mem, 0, reason).queue()
            val expires = System.currentTimeMillis() + time * 1000L
            banTimer(g, expires, mem.idLong)
            val builder = EmbedBuilder()
            builder.setAuthor(
                mem.effectiveName + " wurde für " + secondsToTime(time.toLong()).replace(
                    "*", ""
                ) + " gebannt", null, mem.user.effectiveAvatarUrl
            )
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            BanManager.ban(mem.idLong, mem.user.name, mod.idLong, tco.guild.idLong, reason, Timestamp(expires))
        }

        fun banTimer(g: Guild, expires: Long, mem: Long) {
            if (expires == -1L) return
            moderationService.schedule({
                val gid = g.idLong
                if (BanManager.unban(mem, gid) != 0) {
                    g.unban(UserSnowflake.fromId(mem)).queue()
                }
            }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        fun warn(tco: TextChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht warnen!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde verwarnt", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            //Database.insert("warns", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
            WarnsManager.warn(mem.idLong, mod.idLong, tco.guild.idLong, reason)
        }

        @JvmStatic
        fun getNumber(map: Map<String, String>, pick: String): String {
            //logger.info(map);
            for ((s, value) in map) {
                if (s == pick || pick == "M-$s" || listOf("Amigento", "Wulaosu", "Arceus", "Deoxys", "Genesect").any {
                        s.contains(it) && pick.contains(it)
                    }) return value
            }
            return ""
        }

        @JvmStatic
        fun indexPick(picks: List<String>, mon: String): Int {
            for (pick in picks) {
                if (pick.equals(mon, ignoreCase = true) || pick.substring(2)
                        .equals(mon, ignoreCase = true)
                ) return picks.indexOf(pick)
                if (pick.equals("Amigento", ignoreCase = true) && mon.contains("Amigento")) return picks.indexOf(pick)
            }
            return -1
        }

        @JvmStatic
        fun formatToTime(toformat: Long): String {
            var l = toformat
            l /= 1000
            val hours = (l / 3600).toInt()
            val minutes = ((l - hours * 3600) / 60).toInt()
            val seconds = (l - hours * 3600 - minutes * 60).toInt()
            var str = ""
            if (hours > 0) str += getWithZeros(hours, 2) + ":"
            str += getWithZeros(minutes, 2) + ":"
            str += getWithZeros(seconds, 2)
            return str
        }

        fun getWithZeros(i: Int, lenght: Int): String {
            val str = StringBuilder(i.toString())
            while (str.length < lenght) str.insert(0, "0")
            return str.toString()
        }

        private fun <T> getXTimes(`object`: T, times: Int): List<T> {
            val list = ArrayList<T>()
            for (i in 0 until times) {
                list.add(`object`)
            }
            return list
        }

        fun getCellsAsRowData(`object`: CellData, x: Int, y: Int): List<RowData> {
            val list: MutableList<RowData> = mutableListOf()
            for (i in 0 until y) {
                list.add(RowData().setValues(getXTimes(`object`, x)))
            }
            return list
        }

        @JvmStatic
        fun compareColumns(o1: List<Any>, o2: List<Any>, vararg columns: Int): Int {
            for (column in columns) {
                val i1: Int = if (o1[column] is Int) o1[column] as Int else (o1[column] as String).toInt()
                val i2: Int = if (o2[column] is Int) o2[column] as Int else (o2[column] as String).toInt()
                if (i1 != i2) {
                    return i1.compareTo(i2)
                }
            }
            return 0
        }

        @JvmStatic
        protected fun buildCalendar(): String {
            val f = SimpleDateFormat("dd.MM. HH:mm")
            return CalendarManager.allEntries.sortedBy { it.expires.time }
                .joinToString("\n") { o: CalendarEntry -> "**${f.format(o.expires)}:** ${o.message}" }
                .ifEmpty { "_leer_" }
        }

        @JvmStatic
        protected fun scheduleCalendarEntry(expires: Long, message: String) {
            calendarService.schedule({
                val calendarTc: TextChannel = emolgajda.getTextChannelById(CALENDAR_TCID)!!
                CalendarManager.delete(Timestamp(expires / 1000 * 1000))
                calendarTc.sendMessage("(<@$FLOID>) $message")
                    .setActionRow(Button.primary("calendar;delete", "Löschen")).queue()
                calendarTc.editMessageById(CALENDAR_MSGID, buildCalendar()).queue()
            }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        fun scheduleCalendarEntry(e: CalendarEntry) {
            scheduleCalendarEntry(e.expires.time, e.message)
        }

        @JvmStatic
        @Throws(NumberFormatException::class)
        protected fun parseCalendarTime(str: String): Long {
            var timestr = str.lowercase()
            if (!DURATION_PATTERN.matches(timestr)) {
                val calendar = Calendar.getInstance()
                calendar[Calendar.SECOND] = 0
                var hoursSet = false
                for (s in str.split(";")) {
                    val split = DURATION_SPLITTER.split(s)
                    if (s.contains(".")) {
                        calendar[Calendar.DAY_OF_MONTH] = split[0].toInt()
                        calendar[Calendar.MONTH] = split[1].toInt() - 1
                    } else if (s.contains(":")) {
                        calendar[Calendar.HOUR_OF_DAY] = split[0].toInt()
                        calendar[Calendar.MINUTE] = split[1].toInt()
                        hoursSet = true
                    }
                }
                if (!hoursSet) {
                    calendar[Calendar.HOUR_OF_DAY] = 15
                    calendar[Calendar.MINUTE] = 0
                }
                return calendar.timeInMillis
            }
            var multiplier = 1000
            when (timestr[timestr.length - 1]) {
                'w' -> {
                    multiplier *= 7
                    multiplier *= 24
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'd' -> {
                    multiplier *= 24
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'h' -> {
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'm' -> {
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                's' -> timestr = timestr.substring(0, timestr.length - 1)
            }
            return System.currentTimeMillis() + multiplier.toLong() * timestr.toInt()
        }

        fun updateShinyCounts(id: Long) {
            emolgajda.getTextChannelById(id)?.run {
                editMessageById(
                    if (id == 778380440078647296L) 778380596413464676L else 925446888772239440L,
                    buildAndSaveShinyCounts()
                ).queue()
            }
        }

        fun updateSoullink() {
            emolgajda.getTextChannelById(SOULLINK_TCID)!!.editMessageById(SOULLINK_MSGID, buildSoullink()).queue()
        }

        private fun soullinkCols(): List<String> {
            return listOf(*soullinkNames.toTypedArray(), "Fundort", "Status")
        }

        private fun buildSoullink(): String {
            val statusOrder = listOf("Team", "Box", "RIP")
            val soullink = Emolga.get.soullink
            val mons = soullink.mons
            val order = soullink.order
            val maxlen = max(order.maxOfOrNull { it.length } ?: -1,
                max(mons.values.flatMap { o -> o.values }.maxOfOrNull { obj -> obj.length } ?: -1, 7)) + 1
            val b = StringBuilder("```")
            soullinkCols().map { ew(it, maxlen) }.forEach { b.append(it) }
            b.append("\n")
            for (s in order.sortedBy {
                statusOrder.indexOf(
                    //    mons.createOrGetJSON(it).optString("status", "Box")
                    mons.getOrPut(it) { mutableMapOf() }["status"] ?: "Box"
                )
            }) {
                val o = mons.getOrPut(s) { mutableMapOf() }
                val status = o["status"] ?: "Box"
                b.append(soullinkCols().joinToString("") {
                    ew(
                        when (it) {
                            "Fundort" -> s
                            "Status" -> status
                            else -> o[it] ?: ""
                        }, maxlen
                    )
                }).append("\n")
            }
            return b.append("```").toString()
        }

        /**
         * Expand whitespaces
         *
         * @param str the string
         * @param len the length
         * @return the whitespaced string
         */
        private fun ew(str: String, len: Int): String {
            return str + " ".repeat(max(0, len - str.length))
        }

        fun updateShinyCounts(e: ButtonInteractionEvent) {
            e.editMessage(buildAndSaveShinyCounts()).queue()
        }

        private fun buildAndSaveShinyCounts(): String {
            val b = StringBuilder(50)
            val counter = shinycountjson.getJSONObject("counter")
            val names = shinycountjson.getJSONObject("names")
            for (method in shinycountjson.getString("methodorder").split(",").dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                val m = counter.getJSONObject(method)
                b.append(method).append(": ").append(
                    emolgajda.getGuildById(745934535748747364)!!.getEmojiById(m.getString("emote"))!!.asMention
                ).append("\n")
                for (s in shinycountjson.getString("userorder").split(",")) {
                    b.append(names.getString(s)).append(": ").append(m.optInt(s, 0)).append("\n")
                }
                b.append("\n")
            }
            save(shinycountjson, "shinycount.json")
            return b.toString()
        }

        fun loadAndPlay(channel: TextChannel, trackUrl: String, vc: VoiceChannel) {
            val musicManager = getGuildAudioPlayer(vc.guild)
            getPlayerManager(vc.guild).loadItemOrdered(musicManager, trackUrl, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    channel.sendMessage("`" + track.info.title + "` wurde zur Warteschlange hinzugefügt!").queue()
                    play(vc.guild, musicManager, track, vc)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    var firstTrack = playlist.selectedTrack
                    if (firstTrack == null) {
                        firstTrack = playlist.tracks[0]
                    }
                    channel.sendMessage("Adding to queue " + firstTrack!!.info.title + " (first track of playlist " + playlist.name + ")")
                        .queue()
                    play(vc.guild, musicManager, firstTrack, vc)
                }

                override fun noMatches() {
                    channel.sendMessage("Es wurde unter `$trackUrl` nichts gefunden!").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.message).queue()
                    exception.printStackTrace()
                }
            })
        }

        fun play(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack, mem: Member, tc: TextChannel) {
            val audioManager = guild.audioManager
            if (!audioManager.isConnected) {
                if (mem.voiceState!!.inAudioChannel()) {
                    audioManager.openAudioConnection(mem.voiceState!!.channel)
                } else {
                    tc.sendMessage("Du musst dich in einem Voicechannel befinden!").queue()
                }
            }
            musicManager.scheduler.queue(track)
        }

        fun playFlo(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack, mem: Member) {
            val audioManager = guild.audioManager
            if (!audioManager.isConnected) {
                if (mem.voiceState!!.inAudioChannel()) {
                    audioManager.openAudioConnection(mem.voiceState!!.channel)
                } else {
                    sendToMe("Du musst dich in einem Voicechannel befinden!")
                }
            }
            musicManager.scheduler.queue(track)
        }

        fun play(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack, vc: AudioChannel) {
            val audioManager = guild.audioManager
            if (!audioManager.isConnected) {
                audioManager.openAudioConnection(vc)
            }
            musicManager.scheduler.queue(track)
        }

        fun skipTrack(channel: TextChannel) {
            val musicManager = getGuildAudioPlayer(channel.guild)
            musicManager.scheduler.nextTrack()
            channel.sendMessage("Skipped :)").queue()
        }

        @Synchronized
        fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
            val guildId = guild.idLong
            var musicManager = musicManagers[guildId]
            if (musicManager == null) {
                musicManager = GuildMusicManager(getPlayerManager(guild))
                musicManagers[guildId] = musicManager
            }
            guild.audioManager.sendingHandler = musicManager.sendHandler
            return musicManager
        }

        @Synchronized
        fun getPlayerManager(guild: Guild): AudioPlayerManager {
            val guildId = guild.idLong
            var playerManager = playerManagers[guildId]
            if (playerManager == null) {
                playerManager = DefaultAudioPlayerManager()
                playerManagers[guildId] = playerManager
            }
            AudioSourceManagers.registerRemoteSources(playerManager)
            AudioSourceManagers.registerLocalSource(playerManager)
            return playerManager
        }

        @JvmStatic
        fun trim(s: String, pokemon: String): String {
            return s.replace(pokemon, stars(pokemon.length)).replace(pokemon.uppercase(), stars(pokemon.length))
        }

        private fun stars(n: Int): String {
            return "+".repeat(0.coerceAtLeast(n))
        }

        fun load(filename: String): JSONObject {
            try {
                val f = File(filename)
                if (f.createNewFile()) {
                    val writer = BufferedWriter(FileWriter(f))
                    writer.write("{}")
                    writer.close()
                }
                return JSONObject(JSONTokener(FileReader(filename)))
            } catch (e: IOException) {
                throw e
            }
        }

        @JvmStatic
        fun loadSD(path: String, sub: Int): JSONObject {
            try {
                val f = File(path)
                logger.info("path = $path")
                val l = Files.readAllLines(f.toPath())
                val b = StringBuilder(1000)
                var func = false
                for ((i, s) in l.withIndex()) {
                    val str: String =
                        if (i == 0) s.substring(sub) else if (i == l.size - 1) s.substring(0, s.length - 1) else s
                    if (path.endsWith("moves.ts")) {/*if ((str.contains("{") && str.contains("(") && str.contains(")") && !str.contains(":")) || str.equals("\t\tcondition: {") || str.equals("\t\tsecondary: {")) {
                        func = true;
                    }*/
                        if (str.startsWith("\t\t") && str.isNotEmpty() && (str[str.length - 1] == '{' || str[str.length - 1] == '[')) func =
                            true
                        if (str == "\t\t}," && func) {
                            func = false
                        } else if (!str.startsWith("\t\t\t") && !func) b.append(str).append("\n")
                    } else {
                        b.append(str).append("\n")
                    }
                }
                if (path.endsWith("moves.ts")) {
                    //BufferedWriter writer = new BufferedWriter(new FileWriter("ichbineinwirklichtollertest.json"));
                    val writer2 = BufferedWriter(FileWriter("ichbineinbesserertest.txt"))
                    //writer.write(object.toString(4));
                    writer2.write(b.toString())
                    //writer.close();
                    writer2.close()
                }
                return JSONObject(b.toString())
            } catch (e: Exception) {
                logger.info("STACKTRACE $path")
                throw e
            }
        }

        val dataJSON: JSONObject
            get() = ModManager.default.dex
        val typeJSON: JSONObject
            get() = ModManager.default.typechart
        val learnsetJSON: JSONObject
            get() = ModManager.default.learnsets
        val movesJSON: JSONObject
            get() = ModManager.default.moves

        @JvmStatic
        fun save(json: JSONObject, filename: String) {
            try {
                Files.copy(Paths.get(filename), Paths.get("$filename.bak"), StandardCopyOption.REPLACE_EXISTING)
                val writer = BufferedWriter(FileWriter(filename))
                writer.write(json.toString(4))
                writer.close()
            } catch (ioException: IOException) {
                ioException.printStackTrace()
            }
        }

        fun awaitNextDay() {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_MONTH, 1)
            c[Calendar.HOUR_OF_DAY] = 0
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 0
            val tilnextday = c.timeInMillis - System.currentTimeMillis() + 1000
            logger.info("System.currentTimeMillis() = " + System.currentTimeMillis())
            logger.info("tilnextday = $tilnextday")
            logger.info("System.currentTimeMillis() + tilnextday = " + (System.currentTimeMillis() + tilnextday))
            logger.info("SELECT REQUEST: " + "SELECT * FROM birthdays WHERE month = " + (c[Calendar.MONTH] + 1) + " AND day = " + c[Calendar.DAY_OF_MONTH])
            birthdayService.schedule({
                BirthdayManager.checkBirthdays(c, flegmonjda.getTextChannelById(605650587329232896L)!!)
                awaitNextDay()
            }, tilnextday, TimeUnit.MILLISECONDS)
        }

        fun getTrainerDataActionRow(dt: TrainerData, withMoveset: Boolean): Collection<ActionRow> {
            return listOf(
                ActionRow.of(SelectMenu.create("trainerdata").addOptions(dt.monsList.map {
                    SelectOption.of(
                        it, it
                    ).withDefault(dt.isCurrent(it))
                }).build()), ActionRow.of(
                    if (withMoveset) Button.success(
                        "trainerdata;CHANGEMODE", "Mit Moveset"
                    ) else Button.secondary("trainerdata;CHANGEMODE", "Ohne Moveset")
                )
            )
        }

        fun getGenerationFromDexNumber(dexnumber: Int): Int {
            return if (dexnumber <= 151) 1 else if (dexnumber <= 251) 2 else if (dexnumber <= 386) 3 else if (dexnumber <= 493) 4 else if (dexnumber <= 649) 5 else if (dexnumber <= 721) 6 else if (dexnumber <= 809) 7 else 8
        }

        private fun setupRepeatTasks() {
            RepeatTask(
                Instant.ofEpochMilli(1661292000000), 5, Duration.ofDays(7L), { doNDSNominate() }, true
            )
            RepeatTask(
                Instant.ofEpochMilli(1661104800000),
                5,
                Duration.ofDays(7L),
                { doMatchUps(it) },
                true
            )
        }

        @JvmStatic
        fun init() {
            loadJSONFiles()
            ModManager("default", "./ShowdownData/")
            ModManager("nml", "../Showdown/sspserver/data/")
            Tierlist.setup()
            defaultScope.launch {
                ButtonListener.init()
                MenuListener.init()
                ModalListener.init()
                registerCommands()
                setupRepeatTasks()
                mutedRoles.putAll(Emolga.get.mutedroles)
                moderatorRoles.putAll(Emolga.get.moderatorroles)
                emolgaChannel.putAll(Emolga.get.emolgachannel)
                serebiiex["Barschuft-B"] = "b"
                serebiiex["Riffex-H"] = ""
                serebiiex["Riffex-T"] = "-l"
                serebiiex["Wolwerock-Tag"] = ""
                serebiiex["Wolwerock-Nacht"] = "-m"
                serebiiex["Wolwerock-Dusk"] = "-d"
                serebiiex["Barschuft-R"] = ""
                serebiiex["Wulaosu-Unlicht"] = ""
                serebiiex["Wulaosu-Wasser"] = "-r"
                serebiiex["Basculin-B"] = "b"
                serebiiex["Toxtricity-H"] = ""
                serebiiex["Toxtricity-T"] = "-l"
                serebiiex["Lycanroc-Tag"] = ""
                serebiiex["Lycanroc-Nacht"] = "-m"
                serebiiex["Lycanroc-Dusk"] = "-d"
                serebiiex["Basculin-R"] = ""
                serebiiex["Urshifu-Unlicht"] = ""
                serebiiex["Urshifu-Wasser"] = "-r"
                serebiiex["Rotom-Wash"] = "-w"
                serebiiex["Rotom-Heat"] = "-h"
                serebiiex["Rotom-Fan"] = "-s"
                serebiiex["Rotom-Mow"] = "-m"
                serebiiex["Rotom-Frost"] = "-f"
                serebiiex["Demeteros-T"] = "-s"
                serebiiex["Voltolos-T"] = "-s"
                serebiiex["Boreos-I"] = ""
                serebiiex["Burmadame-Pflz"] = ""
                serebiiex["Burmadame-Sand"] = "-c"
                serebiiex["Burmadame-Lumpen"] = "-s"
                sdex["Burmadame-Pflz"] = ""
                sdex["Burmadame-Pflanze"] = ""
                sdex["Burmadame-Sand"] = "-sandy"
                sdex["Burmadame-Boden"] = "-sandy"
                sdex["Burmadame-Lumpen"] = "-trash"
                sdex["Burmadame-Stahl"] = "-trash"
                sdex["Boreos-T"] = "-therian"
                sdex["Demeteros-T"] = "-therian"
                sdex["Deoxys-Def"] = "-defense"
                sdex["Deoxys-Speed"] = "-speed"
                sdex["Hoopa-U"] = "-unbound"
                sdex["Wulaosu-Wasser"] = "-rapidstrike"
                sdex["Demeteros-I"] = ""
                sdex["Rotom-Heat"] = "-heat"
                sdex["Rotom-Wash"] = "-wash"
                sdex["Rotom-Mow"] = "-mow"
                sdex["Rotom-Fan"] = "-fan"
                sdex["Rotom-Frost"] = "-frost"
                sdex["Wolwerock-Zw"] = "-dusk"
                sdex["Wolwerock-Dusk"] = "-dusk"
                sdex["Wolwerock-Tag"] = ""
                sdex["Wolwerock-Nacht"] = "-midnight"
                sdex["Boreos-I"] = ""
                sdex["Voltolos-T"] = "-therian"
                sdex["Voltolos-I"] = ""
                sdex["Zygarde-50%"] = ""
                sdex["Zygarde-10%"] = "-10"
                sdex["Psiaugon-W"] = "f"
                sdex["Psiaugon-M"] = ""
                sdex["Nidoran-M"] = "m"
                sdex["Nidoran-F"] = "f"/*emolgaChannel.put(Constants.ASLID, new ArrayList<>(Arrays.asList(728680506098712579L, 736501675447025704L)));
        emolgaChannel.put(Constants.BSID, new ArrayList<>(Arrays.asList(732545253344804914L, 735076688144105493L)));
        emolgaChannel.put(709877545708945438L, new ArrayList<>(Collections.singletonList(738893933462945832L)));
        emolgaChannel.put(677229415629062180L, new ArrayList<>(Collections.singletonList(731455491527540777L)));
        emolgaChannel.put(694256540642705408L, new ArrayList<>(Collections.singletonList(695157832072560651L)));
        emolgaChannel.put(747357029714231299L, new ArrayList<>(Arrays.asList(752802115096674306L, 762411109859852298L)));*/
            }
        }

        fun convertColor(hexcode: Int): Color {
            val c = java.awt.Color(hexcode)
            return Color().setRed(c.red.toFloat() / 255f).setGreen(c.green.toFloat() / 255f)
                .setBlue(c.blue.toFloat() / 255f)
        }

        private fun getSortedListOfMons(list: List<JSONObject>): List<String> {
            return list.asSequence().sortedWith(compareBy({
                getByGuild(ASLID)!!.order.indexOf(
                    it.getString("tier")
                )
            }, {
                it.getString("name")
            })).map { it.getString("name") }.toList()
        }

        fun evaluatePredictions(league: JSONObject, p1wins: Boolean, gameday: Int, uid1: String, uid2: String) {
            defaultScope.launch {
                val predictiongame = league.getJSONObject("predictiongame")
                val gd = predictiongame.getJSONObject("ids").getJSONObject((gameday + 7).toString())
                val key = if (gd.has("$uid1:$uid2")) "$uid1:$uid2" else "$uid2:$uid1"
                val message: Message =
                    emolgajda.getTextChannelById(predictiongame.getLong("channelid"))!!
                        .retrieveMessageById(gd.getLong(key))
                        .await()
                val e1: List<User> =
                    message.retrieveReactionUsers(emolgajda.getEmojiById(540970044297838597L)!!).await()
                val e2: List<User> =
                    message.retrieveReactionUsers(emolgajda.getEmojiById(645622238757781505L)!!).await()
                if (p1wins) {
                    for (user in e1) {
                        if (!e2.contains(user)) {
                            incrementPredictionCounter(user.idLong)
                        }
                    }
                } else {
                    for (user in e2) {
                        if (!e1.contains(user)) {
                            incrementPredictionCounter(user.idLong)
                        }
                    }
                }
            }
        }

        fun generateResult(
            b: RequestBuilder,
            game: Array<Player>,
            league: JSONObject,
            gameday: Int,
            uid1: String,
            sheet: String,
            leaguename: String?,
            replay: String
        ) {
            var aliveP1 = 0
            var aliveP2 = 0
            for (p in game[0].mons) {
                if (!p.isDead) aliveP1++
            }
            for (p in game[1].mons) {
                if (!p.isDead) aliveP2++
            }
            var str: String? = null
            var index = -1
            val battleorder = listOf(
                *league.getJSONObject("battleorder").getString(gameday.toString()).split(";")
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
            )
            for (s in battleorder) {
                if (s.contains(uid1)) {
                    str = s
                    index = battleorder.indexOf(s)
                    break
                }
            }
            val coords: String = when (leaguename) {
                "ZBS" -> getZBSGameplanCoords(gameday, index)
                "Wooloo" -> getWoolooGameplanCoords(gameday, index)
                "WoolooCupS4" -> getWoolooS4GameplanCoords(gameday, index)
                "ASLS10" -> getASLS10GameplanCoords(gameday, index)
                else -> ""
            }
            if (str!!.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()[0] == uid1) {
                b.addSingle("$sheet!$coords", "=HYPERLINK(\"$replay\"; \"$aliveP1:$aliveP2\")")
                //b.addSingle(sheet + "!" + coords, aliveP1 + ":" + aliveP2);
            } else {
                b.addSingle("$sheet!$coords", "=HYPERLINK(\"$replay\"; \"$aliveP2:$aliveP1\")")
                //b.addSingle(sheet + "!" + coords, aliveP2 + ":" + aliveP1);
            }
        }

        val monList: List<String>
            get() = dataJSON.keySet().filter { s: String -> !s.endsWith("gmax") && !s.endsWith("totem") }

        private fun getZBSGameplanCoords(gameday: Int, index: Int): String {
            if (gameday < 4) return "C" + (gameday * 5 + index - 2)
            return if (gameday < 7) "F" + ((gameday - 3) * 5 + index - 2) else "I" + (index + 3)
        }

        private fun getWoolooGameplanCoords(gameday: Int, index: Int): String {
            logger.info("gameday = $gameday")
            logger.info("index = $index")
            if (gameday < 4) return "C" + (gameday * 6 + index - 2)
            return if (gameday < 7) "F" + ((gameday - 3) * 6 + index - 2) else "I" + ((gameday - 6) * 6 + index - 2)
        }

        private fun getWoolooS4GameplanCoords(gameday: Int, index: Int): String {
            val x = gameday - 1
            logger.info("gameday = $gameday")
            logger.info("index = $index")
            return "${getAsXCoord(if (x in 2..7) (gameday % 3 shl 2) + 3 else (x % 2 shl 2) + 5)}${(gameday / 3 shl 3) + index + 3}"
        }

        private fun getASLS10GameplanCoords(gameday: Int, index: Int): String {
            val x = gameday - 1
            logger.info("gameday = $gameday")
            logger.info("index = $index")
            return "${getAsXCoord(if (x in 2..7) (gameday % 3 shl 2) + 3 else (x % 2 shl 2) + 5)}${gameday / 3 * 6 + index + 4}"
        }

        fun loadJSONFiles() {
            tokens = load("./tokens.json")
            loadEmolgaJSON()
            defaultScope.launch {
                //emolgaJSON = load("./emolgadata.json")
                //datajson = loadSD("pokedex.ts", 59);
                //movejson = loadSD("learnsets.ts", 62);
                shinycountjson = load("./shinycount.json")
                catchrates = load("./catchrates.json")
                val google = tokens.getJSONObject("google")
                Google.setCredentials(
                    google.getString("refreshtoken"), google.getString("clientid"), google.getString("clientsecret")
                )
                Google.generateAccessToken()
            }
        }

        fun getWithCategory(category: CommandCategory, g: Guild, mem: Member): List<Command> {
            return commands.values.filter {
                !it.disabled && it.category === category && it.allowsGuild(g) && it.allowsMember(mem)
            }.sortedBy { it.name }
        }

        @JvmStatic
        fun updatePresence() {
            if (BOT_DISABLED) {
                emolgajda.presence.setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.watching("auf den Wartungsmodus"))
                return
            }
            val count = StatisticsManager.analysisCount
            replayCount.set(count)
            if (count % 100 == 0) {
                emolgajda.getTextChannelById(904481960527794217L)!!
                    .sendMessage(SimpleDateFormat("dd.MM.yyyy").format(Date()) + ": " + count).queue()
            }
            emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("auf $count analysierte Replays"))
        }

        fun getHelpButtons(g: Guild, mem: Member): List<ActionRow> {
            return getActionRows(order.filter { cat: CommandCategory ->
                cat.allowsGuild(g) && cat.allowsMember(
                    mem
                )
            }) { s: CommandCategory ->
                Button.primary("help;" + s.categoryName.lowercase(), s.categoryName).withEmoji(
                    Emoji.fromCustom(
                        g.jda.getEmojiById(s.emote)!!
                    )
                )
            }
        }

        fun help(tco: TextChannel, mem: Member) {/*if(mem.getIdLong() != Constants.FLOID) {
            tco.sendMessage("Die Help-Funktion wird momentan umprogrammiert und steht deshalb nicht zur Verfügung.").queue();
            return;
        }*/
            val builder = EmbedBuilder()
            builder.setTitle("Commands").setColor(java.awt.Color.CYAN)
            //builder.setDescription(getHelpDescripion(tco.getGuild(), mem));
            val ma = tco.sendMessageEmbeds(builder.build())
            val g = tco.guild
            ma.setActionRows(getHelpButtons(g, mem)).queue()
        }

        suspend fun check(e: MessageReceivedEvent) {
            val mem = e.member!!
            val msg = e.message.contentDisplay
            val tco = e.channel
            val gid = e.guild.idLong
            val bot = Bot.byJDA(e.jda)
            val customcommands = Emolga.get.customcommands
            if (msg.isNotEmpty() && msg[0] == '!') {
                customcommands[msg.lowercase().substring(1)]?.let { o ->
                    val f: File? = o.image?.let { File(it) }
                    val sendmsg = o.text
                    if (sendmsg == null) {
                        tco.sendFile(f!!).queue()
                    } else {
                        val ac = tco.sendMessage(sendmsg)
                        if (f != null) ac.addFile(f)
                        ac.queue()
                    }

                }
            }
            if (bot == Bot.FLEGMON || gid == 745934535748747364L) {
                val dir = File("audio/clips/")
                for (file in dir.listFiles()!!) {
                    if (msg.equals(
                            "!" + file.name.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()[0],
                            ignoreCase = true
                        )
                    ) {
                        val voiceState = e.member!!.voiceState
                        val pepe = mem.idLong == 349978348010733569L
                        if (voiceState!!.inAudioChannel() || pepe) {
                            val am = e.guild.audioManager
                            if (!am.isConnected) {
                                if (voiceState.inAudioChannel()) {
                                    am.openAudioConnection(voiceState.channel)
                                    am.connectionListener = object : ConnectionListener {
                                        override fun onPing(ping: Long) {}
                                        override fun onStatusChange(status: ConnectionStatus) {
                                            logger.info("status = $status")
                                            if (status == ConnectionStatus.CONNECTED) {
                                                playSound(
                                                    voiceState.channel, "/home/florian/Discord/audio/clips/hi.mp3", tco
                                                )
                                                playSound(voiceState.channel, file.path, tco)
                                            }
                                        }

                                        override fun onUserSpeaking(user: User, speaking: Boolean) {}
                                    }
                                }
                            } else {
                                playSound(am.connectedChannel, file.path, tco)
                            }
                        }
                    }
                }
            }
            val command = commands[WHITESPACES_SPLITTER.split(msg)[0].lowercase()]
            if (command != null) {
                if (command.disabled || command.onlySlash) return
                if (!command.checkBot(e.jda, gid)) return
                val check = command.checkPermissions(gid, mem)
                if (check == PermissionCheck.GUILD_NOT_ALLOWED) return
                if (check == PermissionCheck.PERMISSION_DENIED) {
                    tco.sendMessage(NOPERM).queue()
                    return
                }
                if (mem.idLong != FLOID) {
                    if (BOT_DISABLED) {
                        e.channel.sendMessage(DISABLED_TEXT).queue()
                        return
                    }
                    if (command.wip) {
                        tco.sendMessage("Diese Funktion ist derzeit noch in Entwicklung und ist noch nicht einsatzbereit!")
                            .queue()
                        return
                    }
                }
                if (!command.everywhere && !command.category!!.isEverywhere) {
                    if (command.overrideChannel.containsKey(gid)) {
                        val l: List<Long> = command.overrideChannel[gid]!!
                        if (!l.contains(e.channel.idLong)) {
                            if (e.author.idLong == FLOID) {
                                tco.sendMessage("Eigentlich dürfen hier keine Commands genutzt werden, aber weil du es bist, mache ich das c:")
                                    .queue()
                            } else {
                                e.channel.sendMessage("<#" + l[0] + ">").queue()
                                return
                            }
                        }
                    } else {
                        if (emolgaChannel.containsKey(gid)) {
                            val l = emolgaChannel[gid]!!
                            if (!l.contains(e.channel.idLong) && l.isNotEmpty()) {
                                if (e.author.idLong == FLOID) {
                                    tco.sendMessage("Eigentlich dürfen hier keine Commands genutzt werden, aber weil du es bist, mache ich das c:")
                                        .queue()
                                } else {
                                    e.channel.sendMessage("<#" + l[0] + ">").queue()
                                    return
                                }
                            }
                        }
                    }
                }
                try {
                    StatisticsManager.increment("cmd_" + command.name)
                    val randnum = Random.nextInt(4096)
                    logger.info("randnum = $randnum")
                    if (randnum == 133) {
                        e.channel.sendMessage("No, I don't think I will :^)\n||Gib mal den Command nochmal ein, die Wahrscheinlichkeit, dass diese Nachricht auftritt, liegt bei 1:4096 :D||")
                            .queue()
                        sendToMe(e.guild.name + " " + e.channel.asMention + " " + e.author.id + " " + e.author.asMention + " HAT IM LOTTO GEWONNEN!")
                        return
                    }
                    if (command.beta) e.channel.sendMessage(
                        "Dieser Command befindet sich zurzeit in der Beta-Phase! Falls Fehler auftreten, kontaktiert bitte $MYTAG durch einen Ping oder eine PN!"
                    ).queue()
                    GuildCommandEvent(command, e).execute()
                } catch (ex: MissingArgumentException) {
                    if (ex.isSubCmdMissing) {
                        val subcommands = ex.subcommands!!
                        tco.sendMessage("Dieser Command beinhaltet Sub-Commands: " + subcommands.joinToString { subcmd: String -> "`$subcmd`" })
                            .queue()
                    } else {
                        val arg = ex.argument!!
                        if (arg.hasCustomErrorMessage()) tco.sendMessage(arg.customErrorMessage!!).queue() else {
                            tco.sendMessage(
                                "Das benötigte Argument `" + arg.name + "`, was eigentlich " + buildEnumeration(
                                    arg.type.getName()
                                ) + " sein müsste, ist nicht vorhanden!\n" + "Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help " + command.name + "`."
                            ).queue()
                        }
                        if (mem.idLong != FLOID) {
                            sendToMe("MissingArgument " + tco.asMention + " Server: " + tco.asGuildMessageChannel().guild.name)
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    tco.sendMessage(
                        "Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo ($MYTAG).\n${
                            command.getHelp(
                                e.guild
                            )
                        }${if (mem.idLong == FLOID) "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" else ""}"
                    ).queue()
                }
            }
        }

        fun eachWordUpperCase(s: String): String {
            val st = StringBuilder(s.length)
            for (str in s.split(" ").dropLastWhile { it.isEmpty() }.toTypedArray()) {
                st.append(firstUpperCase(str)).append(" ")
            }
            return st.substring(0, st.length - 1)
        }

        private fun firstUpperCase(s: String): String {
            return EMPTY_PATTERN.split(s)[0].uppercase() + s.substring(1).lowercase()
        }

        private fun getType(str: String): String {
            val s = str.lowercase()
            if (s.contains("-normal")) return "Normal" else if (s.contains("-kampf") || s.contains("-fighting")) return "Kampf" else if (s.contains(
                    "-flug"
                ) || s.contains("-flying")
            ) return "Flug" else if (s.contains("-gift") || s.contains("-poison")) return "Gift" else if (s.contains("-boden") || s.contains(
                    "-ground"
                )
            ) return "Boden" else if (s.contains("-gestein") || s.contains("-rock")) return "Gestein" else if (s.contains(
                    "-käfer"
                ) || s.contains("-bug")
            ) return "Käfer" else if (s.contains("-geist") || s.contains("-ghost")) return "Geist" else if (s.contains("-stahl") || s.contains(
                    "-steel"
                )
            ) return "Stahl" else if (s.contains("-feuer") || s.contains("-fire")) return "Feuer" else if (s.contains("-wasser") || s.contains(
                    "-water"
                )
            ) return "Wasser" else if (s.contains("-pflanze") || s.contains("-grass")) return "Pflanze" else if (s.contains(
                    "-elektro"
                ) || s.contains("-electric")
            ) return "Elektro" else if (s.contains("-psycho") || s.contains("-psychic")) return "Psycho" else if (s.contains(
                    "-eis"
                ) || s.contains("-ice")
            ) return "Eis" else if (s.contains("-drache") || s.contains("-dragon")) return "Drache" else if (s.contains(
                    "-unlicht"
                ) || s.contains("-dark")
            ) return "Unlicht" else if (s.contains("-fee") || s.contains("-fairy")) return "Fee"
            return ""
        }

        private fun getClass(str: String): String {
            val s = str.lowercase()
            if (s.contains("-phys")) return "Physical" else if (s.contains("-spez")) return "Special" else if (s.contains(
                    "-status"
                )
            ) return "Status"
            return ""
        }

        fun getAllForms(monname: String): List<JSONObject> {
            val json = dataJSON
            val mon = json.getJSONObject(getSDName(monname))
            //logger.info("getAllForms mon = " + mon.toString(4));
            return if (!mon.has("formeOrder")) listOf(mon) else mon.getStringList("formeOrder").asSequence()
                .map { toSDName(it) }.distinct().mapNotNull { json.optJSONObject(it) }
                .filter { !it.optString("forme").endsWith("Totem") }.toList()
        }

        private fun moveFilter(msg: String, move: String): Boolean {
            val o = Emolga.get.movefilter
            for (s in o.keys) {
                if (msg.lowercase().contains("--$s") && move !in o[s]!!) return false
            }
            return true
        }

        fun canLearnNDS(monId: String, vararg moveId: String): String {
            for (s in moveId) {
                if (canLearn(monId, s)) return "JA"
            }
            return "NEIN"
        }

        private fun canLearn(monId: String, moveId: String): Boolean {
            try {
                val movejson = learnsetJSON
                val data = dataJSON
                val o = data.getJSONObject(monId)
                var str: String?
                str = if (o.has("baseSpecies")) toSDName(o.getString("baseSpecies")) else monId
                while (str != null) {
                    val learnset = movejson.getJSONObject(str).getJSONObject("learnset")
                    val set = TranslationsManager.getTranslationList(learnset.keySet())
                    while (set.next()) {
                        val moveengl = set.getString("englishid")
                        if (moveengl == moveId) return true
                    }
                    val mon = data.getJSONObject(str)
                    str = if (mon.has("prevo")) {
                        toSDName(mon.getString("prevo"))
                    } else null
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return false
        }

        fun getAttacksFrom(pokemon: String, msg: String, form: String, maxgen: Int): List<String> {
            val already: MutableList<String> = ArrayList()
            val type = getType(msg)
            val dmgclass = getClass(msg)
            val movejson = learnsetJSON
            val atkdata = movesJSON
            val data = dataJSON
            try {
                var str: String? = getSDName(pokemon) + if (form == "Normal") "" else form.lowercase()
                while (str != null) {
                    val learnset = movejson.getJSONObject(str).getJSONObject("learnset")
                    val set = TranslationsManager.getTranslationList(learnset.keySet())
                    while (set.next()) {
                        //logger.info("moveengl = " + moveengl);
                        val moveengl = set.getString("englishid")
                        val move = set.getString("germanname")
                        //logger.info("move = " + move);
                        if (type.isEmpty() || atkdata.getJSONObject(moveengl)
                                .getString("type") == getEnglName(type) && (dmgclass.isEmpty() || (atkdata.getJSONObject(
                                moveengl
                            ).getString("category")) == dmgclass) && (!msg.lowercase()
                                .contains("--prio") || atkdata.getJSONObject(moveengl)
                                .getInt("priority") > 0) && containsGen(learnset, moveengl, maxgen) && moveFilter(
                                msg, move
                            ) && !already.contains(move)
                        ) {
                            already.add(move)
                        }
                    }
                    val mon = data.getJSONObject(str)
                    str = if (mon.has("prevo")) {
                        val s = mon.getString("prevo")
                        if (s.endsWith("-Alola") || s.endsWith("-Galar") || s.endsWith("-Unova")) HYPHEN.replace(s, "")
                            .lowercase() else s.lowercase()
                    } else null
                }
            } catch (ex: Exception) {
                sendToMe("Schau in die Konsole du kek!")
                ex.printStackTrace()
            }
            return already
        }

        fun sendToMe(msg: String, vararg bot: Bot) {
            sendToUser(FLOID, msg, *bot)
        }

        @JvmStatic
        fun sendDexEntry(msg: String) {
            emolgajda.getTextChannelById(839540004908957707L)!!.sendMessage(msg).queue()
        }

        fun sendStacktraceToMe(t: Throwable) {
            sendToMe(t.stackTraceToString())
        }

        fun sendToUser(user: User, msg: String) {
            user.openPrivateChannel().flatMap {
                it.sendMessage(msg)
            }.queue()
        }

        fun sendToUser(id: Long, msg: String, vararg bot: Bot) {
            val jda: JDA = if (bot.isEmpty()) emolgajda else bot[0].jDA
            jda.retrieveUserById(id).flatMap { obj: User -> obj.openPrivateChannel() }.flatMap { pc: PrivateChannel ->
                pc.sendMessage(
                    msg
                )
            }.queue()
        }

        private fun containsGen(learnset: JSONObject, move: String, gen: Int): Boolean {
            for (s in learnset.getStringList(move)) {
                for (i in 1..gen) {
                    if (s.startsWith(i.toString())) return true
                }
            }
            return false
        }

        @JvmStatic
        fun getAsXCoord(xc: Int): String {
            var i = xc
            var x = 0
            while (i - 26 > 0) {
                i -= 26
                x++
            }
            return (if (x > 0) (x + 64).toChar() else "").toString() + (i + 64).toChar().toString()
        }

        fun getGen5SpriteWithoutGoogle(o: JSONObject, shiny: Boolean = false): String {
            return "https://play.pokemonshowdown.com/sprites/gen5" + (if (shiny) "-shiny" else "") + "/" + toSDName(
                if (o.has(
                        "baseSpecies"
                    )
                ) o.getString("baseSpecies") else o.getString("name")
            ) + (if (o.has("forme")) "-" + toSDName(o.getString("forme")) else "") + ".png"
        }

        fun getSpriteForTeamGraphic(str: String, data: RandomTeamData): String {
            if (str == "Sen-Long") data.hasDrampa = true
            val o = getDataObject(str)
            val odds = Emolga.get.config.teamgraphicShinyOdds
            return buildString {
                append("gen5_cropped")
                if (Random.nextInt(odds) == 0) {
                    append("_shiny")
                    data.shinyCount.incrementAndGet()
                }
                append("/")
                append(toSDName(if (o.has("baseSpecies")) o.getString("baseSpecies") else o.getString("name")))
                append((if (o.has("forme")) "-" + toSDName(o.getString("forme")) else ""))
                append(".png")
            }
        }

        fun getGen5Sprite(o: JSONObject): String {
            return "=IMAGE(\"https://play.pokemonshowdown.com/sprites/gen5/" + toSDName(
                if (o.has("baseSpecies")) o.getString(
                    "baseSpecies"
                ) else o.getString("name")
            ) + (if (o.has("forme")) "-" + toSDName(o.getString("forme")) else "") + ".png\"; 1)"
        }

        fun getGen5Sprite(str: String): String {
            return getGen5Sprite(getDataObject(str))
        }

        fun getDataObject(mon: String): JSONObject {
            return dataJSON.getJSONObject(getDataName(mon))
        }

        fun <T> getActionRows(c: Collection<T>, mapper: Function<T, Button>): List<ActionRow> {
            val currRow = mutableListOf<Button>()
            val rows = mutableListOf<ActionRow>()
            for (s in c) {
                currRow.add(mapper.apply(s))
                if (currRow.size == 5) {
                    rows.addAll(currRow.into())
                    currRow.clear()
                }
            }
            if (currRow.size > 0) rows.addAll(currRow.into())
            return rows
        }

        fun <T> addAndReturn(c: MutableCollection<T>, toadd: T): Collection<T> {
            c.add(toadd)
            return c
        }

        fun analyseReplay(
            url: String,
            customReplayChannel: TextChannel?,
            resultchannel: TextChannel,
            m: Message?,
            e: DeferredSlashResponse?
        ) {
            defaultScope.launch {
                if (BOT_DISABLED && resultchannel.guild.idLong != MYSERVER) {
                    (m?.channel ?: resultchannel).sendMessage(DISABLED_TEXT).queue()
                    return@launch
                }
                logger.info("REPLAY! Channel: {}", m?.channel?.id ?: resultchannel.id)
                val game: Array<Player> = try {
                    val analysis = Analysis(url, m).analyse()
                    if (!analysis[0].isInitialized()) throw RuntimeException("Nickname is missing")
                    analysis
                    //game = Analysis.analyse(url, m);
                } catch (ex: Exception) {
                    val msg =
                        "Beim Auswerten des Replays ist ein Fehler aufgetreten! Bitte trage das Ergebnis selbst ein und melde dich gegebenenfalls bei $MYTAG!"
                    if (e != null) e.reply(msg) else {
                        resultchannel.sendMessage(msg).queue()
                    }
                    ex.printStackTrace()
                    sendToMe("Replay ERROR $url ${resultchannel.asMention}")
                    return@launch
                }
                val g = resultchannel.guild
                val gid: Long
                val msg = m?.contentDisplay ?: ""
                gid = if (m != null && m.author.idLong == FLOID) {
                    val matcher = CUSTOM_GUILD_PATTERN.find(msg)
                    matcher?.let {
                        matcher.groupValues[1].toLong()
                    } ?: g.idLong
                } else {
                    g.idLong
                }
                val u1 = game[0].nickname
                val u2 = game[1].nickname
                val uid1 = SDNamesManager.getIDByName(u1)
                val uid2 = SDNamesManager.getIDByName(u2)
                logger.info("Analysed!")
                //logger.info(g.getName() + " -> " + (m.isFromType(ChannelType.PRIVATE) ? "PRIVATE " + m.getAuthor().getId() : m.getTextChannel().getAsMention()));
                for (p in game[0].mons) {
                    game[0].addTotalDeaths(if (p.isDead) 1 else 0)
                }
                for (p in game[1].mons) {
                    game[1].addTotalDeaths(if (p.isDead) 1 else 0)
                }
                val aliveP1 = game[0].teamsize - game[0].totalDeaths
                val aliveP2 = game[1].teamsize - game[1].totalDeaths
                val t1 = StringBuilder(50)
                val t2 = StringBuilder(50)
                val winloose = "$aliveP1:$aliveP2"
                val p1wins = game[0].isWinner
                val kills = listOf<MutableMap<String, String>>(HashMap(), HashMap())
                val deaths = listOf<MutableMap<String, String>>(HashMap(), HashMap())
                val p1mons = ArrayList<String>()
                val p2mons = ArrayList<String>()
                val spoiler = spoilerTags.contains(gid)
                val typicalSets = TypicalSets
                if (spoiler) t1.append("||")
                for (p in game[0].mons) {
                    logger.info("p.getPokemon() = " + p.pokemon)
                    val monName = getMonName(p.pokemon, gid)
                    if (monName.trim { it <= ' ' }
                            .isNotEmpty() && monName.trim { it <= ' ' }[monName.trim { it <= ' ' }.length - 1] == '-') {
                        sendToMe(p.pokemon + " SD - at End")
                    }
                    logger.info("monName = $monName")
                    kills[0][monName] = p.kills.toString()
                    deaths[0][monName] = if (p.isDead) "1" else "0"
                    p1mons.add(monName)
                    if (g.idLong != MYSERVER) {
                        FullStatsManager.add(monName, p.kills, if (p.isDead) 1 else 0, game[0].isWinner)
                        typicalSets.add(monName, p.moves, p.item, p.ability)
                        if (toUsername(game[0].nickname) == "dasor54") {
                            DasorUsageManager.addPokemon(monName)
                        }
                    }
                    t1.append(monName).append(" ").append(if (p.kills > 0) p.kills.toString() + " " else "")
                        .append(if (p.isDead && (p1wins || spoiler)) "X" else "").append("\n")
                }
                run {
                    var i = 0
                    while (i < game[0].teamsize - game[0].mons.size) {
                        t1.append("_unbekannt_").append("\n")
                        i++
                    }
                }
                if (spoiler) t1.append("||")
                if (spoiler) t2.append("||")
                for (p in game[1].mons) {
                    val monName = getMonName(p.pokemon, gid)
                    if (monName.trim { it <= ' ' }
                            .isNotEmpty() && monName.trim { it <= ' ' }[monName.trim { it <= ' ' }.length - 1] == '-') {
                        sendToMe(p.pokemon + " SD - at End")
                    }
                    kills[1][monName] = p.kills.toString()
                    deaths[1][monName] = if (p.isDead) "1" else "0"
                    p2mons.add(monName)
                    if (g.idLong != MYSERVER) {
                        FullStatsManager.add(monName, p.kills, if (p.isDead) 1 else 0, game[1].isWinner)
                        typicalSets.add(monName, p.moves, p.item, p.ability)
                        if (toUsername(game[1].nickname) == "dasor54") {
                            DasorUsageManager.addPokemon(monName)
                        }
                    }
                    t2.append(monName).append(" ").append(if (p.kills > 0) p.kills.toString() + " " else "")
                        .append(if (p.isDead && (!p1wins || spoiler)) "X" else "").append("\n")
                }
                run {
                    var i = 0
                    while (i < game[1].teamsize - game[1].mons.size) {
                        t2.append("_unbekannt_").append("\n")
                        i++
                    }
                }
                if (spoiler) t2.append("||")
                logger.info("Kills")
                logger.info(kills.toString())
                logger.info("Deaths")
                logger.info(deaths.toString())
                val name1: String?
                val name2: String?
                //JSONObject showdown = json.getJSONObject("showdown").getJSONObject(String.valueOf(gid));
                logger.info("u1 = $u1")
                logger.info("u2 = $u2")/*for (String s : showdown.keySet()) {
                        if (u1.equalsIgnoreCase(s)) uid1 = showdown.getString(s);
                        if (u2.equalsIgnoreCase(s)) uid2 = showdown.getString(s);
                    }*/

                //JSONObject teamnames = json.getJSONObject("drafts").getJSONObject("NDS").getJSONObject("teamnames");
                name1 =  /*uid1 != -1 && gid == NDSID ? teamnames.getString(String.valueOf(uid1)) : */game[0].nickname
                name2 =  /*uid2 != -1 && gid == NDSID ? teamnames.getString(String.valueOf(uid2)) : */game[1].nickname
                logger.info("uid1 = $uid1")
                logger.info("uid2 = $uid2")
                val str: String = if (spoiler) {
                    "$name1 ||$winloose|| $name2\n\n$name1:\n$t1\n$name2:\n$t2"
                } else {
                    "$name1 $winloose $name2\n\n$name1: ${if (!p1wins) "(alle tot)" else ""}\n$t1\n$name2: ${if (p1wins) "(alle tot)" else ""}\n$t2"
                }
                customReplayChannel?.sendMessage(url)?.queue()
                if (e != null) {
                    e.reply(str)
                } else if (!customResult.contains(gid)) resultchannel.sendMessage(str).queue()
                StatisticsManager.increment("analysis")
                var i = 0
                while (i < 2) {
                    if (game[i].mons.any { it.pokemon == "Zoroark" || it.pokemon == "Zorua" }) resultchannel.sendMessage(
                        "Im Team von ${game[i].nickname} befindet sich ein Zorua/Zoroark! Bitte noch einmal die Kills überprüfen!"
                    ).queue()
                    i++
                }
                logger.info("In Emolga Listener!")
                //if (gid != 518008523653775366L && gid != 447357526997073930L && gid != 709877545708945438L && gid != 736555250118295622L && )
                //  return;
                typicalSets.save()
                if (uid1 == -1L || uid2 == -1L) return@launch
                Emolga.get.leagueByGuild(gid, uid1, uid2)?.docEntry?.analyse(
                    game,
                    uid1,
                    uid2,
                    kills,
                    deaths,
                    ReplayData(
                        listOf(p1mons, p2mons),
                        url,
                        str,
                        resultchannel,
                        t1,
                        t2,
                        customReplayChannel,
                        m
                    )
                )
            }
        }

        fun calculateDraftTimer(): Long {
            //long delay = calculateTimer(10, 22, 120);
            val delay = calculateTimer(DraftTimer.NDS)
            logger.info(MarkerFactory.getMarker("important"), "delay = {}", delay)
            logger.info(MarkerFactory.getMarker("important"), "expires = {}", delay + System.currentTimeMillis())
            return delay
        }

        private fun calculateTimer(draftTimer: DraftTimer): Long {
            val data = draftTimer.timerInfo
            val delayinmins = draftTimer.delayInMins
            val cal = Calendar.getInstance()
            val currentTimeMillis = cal.timeInMillis
            var elapsedMinutes = delayinmins
            while (elapsedMinutes > 0) {
                val p = data[cal[Calendar.DAY_OF_WEEK]]
                val hour = cal[Calendar.HOUR_OF_DAY]
                if (hour >= p.from && hour < p.to) elapsedMinutes-- else if (elapsedMinutes == delayinmins) cal[Calendar.SECOND] =
                    0
                cal.add(Calendar.MINUTE, 1)
            }
            return cal.timeInMillis - currentTimeMillis
        }

        fun getDraftGerName(sArg: String): Translation {
            val s = sArg.replace(otherFormatRegex) { mr ->
                mr.groupValues.let { "${it[2][0]}-${it[1]}" }
            }
            logger.info("getDraftGerName s = $s")
            val split = s.split("-").dropLastWhile { it.isEmpty() }
            logger.info("getDraftGerName Arr = {}", split)
            val slower = s.lowercase()
            if (slower.startsWith("m-")) {
                val sub = s.substring(2)
                val mon: Translation =
                    if (s.endsWith("-X")) getGerName(sub.substring(0, sub.length - 2)).append("-X") else if (s.endsWith(
                            "-Y"
                        )
                    ) getGerName(sub.substring(0, sub.length - 2)).append("-Y") else getGerName(sub)
                return if (!mon.isFromType(Translation.Type.POKEMON)) Translation.empty() else mon.before("M-")
            } else if (slower.startsWith("a-")) {
                val mon = getGerName(s.substring(2))
                return if (!mon.isFromType(Translation.Type.POKEMON)) Translation.empty() else mon.before("A-")
            } else if (slower.startsWith("g-")) {
                val mon = getGerName(s.substring(2))
                return if (!mon.isFromType(Translation.Type.POKEMON)) Translation.empty() else mon.before("G-")
            }
            val gerName = getGerName(s)
            if (gerName.isSuccess) return gerName
            logger.info("split[0] = " + split[0])
            val t = getGerName(split[0])
            print("DraftGer Trans ")
            t.print()
            if (t.isSuccess) {
                val tr = t.append("-" + split[1])
                logger.info("getDraftGerName ret = $tr")
                return tr
            }
            return Translation.empty()
        }

        fun getGerNameWithForm(name: String): String {
            var toadd = StringBuilder(name)
            val split = ArrayList(listOf(*toadd.toString().split("-").dropLastWhile { it.isEmpty() }.toTypedArray()))
            if (toadd.toString().contains("-Alola")) {
                toadd = StringBuilder("Alola-" + getGerNameNoCheck(split[0]))
                for (i in 2 until split.size) {
                    toadd.append("-").append(split[i])
                }
            } else if (toadd.toString().contains("-Galar")) {
                toadd = StringBuilder("Galar-" + getGerNameNoCheck(split[0]))
                for (i in 2 until split.size) {
                    toadd.append("-").append(split[i])
                }
            } else if (toadd.toString().contains("-Mega")) {
                toadd = StringBuilder("Mega-" + getGerNameNoCheck(split[0]))
                for (i in 2 until split.size) {
                    toadd.append("-").append(split[i])
                }
            } else if (split.size > 1) {
                toadd = StringBuilder(getGerNameNoCheck(split.removeAt(0)) + "-" + java.lang.String.join("-", split))
            } else toadd = StringBuilder(getGerNameNoCheck(toadd.toString()))
            return toadd.toString()
        }

        fun getGerName(s: String): Translation {
            return getGerName(s, false)
        }

        fun getGerName(s: String, checkOnlyEnglish: Boolean, withCap: Boolean = false): Translation {
            logger.info("getGerName s = $s")
            val id = toSDName(s)
            if (translationsCacheGerman.containsKey(id)) return translationsCacheGerman.getValue(id)
            val set = getTranslation(id, checkOnlyEnglish, withCap)
            try {
                if (set.next()) {
                    val t = Translation(
                        set.getString("germanname"),
                        Translation.Type.fromId(set.getString("type")),
                        Translation.Language.GERMAN,
                        set.getString("englishname"),
                        set.getString("forme")
                    )
                    addToCache(true, id, t)
                    return t
                }
            } catch (throwables: SQLException) {
                throwables.printStackTrace()
            }
            return Translation.empty()
        }

        @JvmStatic
        fun getGerNameNoCheck(s: String): String {
            return getGerName(s).translation
        }

        private fun addToCache(german: Boolean, sd: String, t: Translation) {
            if (german) {
                translationsCacheGerman[sd] = t
                translationsCacheOrderGerman.add(sd)
                if (translationsCacheOrderGerman.size > 1000) {
                    translationsCacheGerman.remove(translationsCacheOrderGerman.removeFirst())
                }
            } else {
                translationsCacheEnglish[sd] = t
                translationsCacheOrderEnglish.add(sd)
                if (translationsCacheOrderEnglish.size > 1000) {
                    translationsCacheEnglish.remove(translationsCacheOrderEnglish.removeFirst())
                }
            }
        }

        @JvmStatic
        fun removeNickFromCache(sd: String) {
            translationsCacheGerman.remove(sd)
            translationsCacheEnglish.remove(sd)
            translationsCacheOrderGerman.remove(sd)
            translationsCacheOrderEnglish.remove(sd)
        }

        @JvmStatic
        fun getEnglName(s: String): String {
            val str = getEnglNameWithType(s)
            return if (str.isEmpty) "" else str.translation
        }

        fun getEnglNameWithType(s: String): Translation {
            val id = toSDName(s)
            if (translationsCacheEnglish.containsKey(id)) return translationsCacheEnglish.getValue(id)
            val set = getTranslation(id)
            try {
                if (set.next()) {
                    val t = Translation(
                        set.getString("englishname"),
                        Translation.Type.fromId(set.getString("type")),
                        Translation.Language.ENGLISH,
                        set.getString("germanname"),
                        set.getString("forme")
                    )
                    addToCache(false, id, t)
                    return t
                }
            } catch (throwables: SQLException) {
                throwables.printStackTrace()
            }
            return Translation.empty()
        }

        fun getTranslation(s: String): ResultSet {
            return getTranslation(s, false)
        }

        fun getTranslation(s: String, checkOnlyEnglish: Boolean, withCap: Boolean = false): ResultSet {
            return TranslationsManager.getTranslation(s, checkOnlyEnglish, withCap)
        }

        fun getSDName(str: String): String {
            logger.info("getSDName s = $str")
            val op = sdex.keys.firstOrNull { anotherString: String -> str.equals(anotherString, ignoreCase = true) }
            val gitname: String = if (op != null) {
                val englname = getEnglName(op.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                return toSDName(englname + sdex[str])
            } else {
                if (str.startsWith("M-")) {
                    val sub = str.substring(2)
                    if (str.endsWith("-X")) getEnglName(
                        sub.substring(
                            0, sub.length - 2
                        )
                    ) + "megax" else if (str.endsWith("-Y")) getEnglName(
                        sub.substring(
                            0, sub.length - 2
                        )
                    ) + "megay" else getEnglName(sub) + "mega"
                } else if (str.startsWith("A-")) {
                    getEnglName(str.substring(2)) + "alola"
                } else if (str.startsWith("G-")) {
                    getEnglName(str.substring(2)) + "galar"
                } else if (str.startsWith("Amigento-")) {
                    "silvally" + getEnglName(str.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                } else {
                    getEnglName(str)
                }
            }
            return toSDName(gitname)
        }

        @JvmStatic
        fun toSDName(s: String): String {
            return SD_NAME_PATTERN.replace(s.lowercase(), "")
        }

        @JvmStatic
        fun toUsername(s: String): String {
            return USERNAME_PATTERN.replace(
                s.lowercase().trim().replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss"), ""
            )
        }

        fun canLearn(pokemon: String, form: String, atk: String, msg: String, maxgen: Int): Boolean {
            return getAttacksFrom(pokemon, msg, form, maxgen).contains(atk)
        }

        fun getMonName(s: String, gid: Long): String {
            return getMonName(s, gid, true)
        }

        private fun getMonName(s: String, gid: Long, withDebug: Boolean): String {
            if (withDebug) logger.info("s = $s")
            if (gid == 709877545708945438L) {
                if (s.endsWith("-Alola")) {
                    return "Alola-" + getGerName(s.substring(0, s.length - 6), true).translation
                } else if (s.endsWith("-Galar")) {
                    return "Galar-" + getGerName(s.substring(0, s.length - 6), true).translation
                }
                when (s) {
                    "Oricorio" -> return "Choreogel-Feuer"
                    "Oricorio-Pa'u" -> return "Choreogel-Psycho"
                    "Oricorio-Pom-Pom" -> return "Choreogel-Elektro"
                    "Oricorio-Sensu" -> return "Choreogel-Geist"
                    "Gourgeist" -> return "Pumpdjinn"
                    "Gourgeist-Small" -> return "Pumpdjinn-Small"
                    "Indeedee" -> return "Servol-M"
                    "Indeedee-F" -> return "Servol-W"
                    "Meowstic" -> return "Psiaugon-M"
                    "Meowstic-F" -> return "Psiaugon-W"
                }
            }
            if (s == "Calyrex-Shadow") return "Coronospa-Rappenreiter"
            if (s == "Calyrex-Ice") return "Coronospa-Schimmelreiter"
            if (s == "Shaymin-Sky") return "Shaymin-Sky"
            if (s.contains("Unown")) return "Icognito"
            if (s.contains("Zacian-Crowned")) return "Zacian-Crowned"
            if (s.contains("Zamazenta-Crowned")) return "Zamazenta-Crowned"
            if (s == "Greninja-Ash") return "Quajutsu-Ash"
            if (s == "Zarude-Dada") return "Zarude"
            if (s.contains("Toxtricity")) return "Riffex"
            if (s.contains("Furfrou")) return "Coiffwaff"
            if (s.contains("Genesect")) return "Genesect"
            if (s == "Wormadam") return "Burmadame-Pflz"
            if (s == "Wormadam-Sandy") return "Burmadame-Sand"
            if (s == "Wormadam-Trash") return "Burmadame-Lumpen"
            if (s == "Deoxys-Defense") return "Deoxys-Def"
            if (s == "Deoxys-Attack") return "Deoxys-Attack"
            if (s == "Deoxys-Speed") return "Deoxys-Speed"
            if (s.contains("Minior")) return "Meteno"
            if (s.contains("Polteageist")) return "Mortipot"
            if (s.contains("Wormadam")) return "Burmadame"
            if (s.contains("Keldeo")) return "Keldeo"
            if (s.contains("Gastrodon")) return "Gastrodon"
            if (s.contains("Eiscue")) return "Kubuin"
            if (s.contains("Oricorio")) return "Choreogel"
            if ((gid == ASLID || gid == NDSID || s == "Urshifu-Rapid-Strike") && s.contains("Urshifu")) return "Wulaosu-Wasser"
            if (s == "Urshifu") return "Wulaosu-Unlicht"
            if (s.contains("Urshifu")) return "Wulaosu"
            if (s.contains("Gourgeist")) return "Pumpdjinn"
            if (s.contains("Pumpkaboo")) return "Irrbis"
            if (s.contains("Pikachu")) return "Pikachu"
            if (s.contains("Indeedee")) return "Servol"
            if (s.contains("Meloetta")) return "Meloetta"
            if (s.contains("Alcremie")) return "Pokusan"
            if (s.contains("Mimikyu")) return "Mimigma"
            if (s == "Lycanroc") return "Wolwerock-Tag"
            if (s == "Lycanroc-Midnight") return "Wolwerock-Nacht"
            if (s == "Lycanroc-Dusk") return "Wolwerock-Zw"
            if (s == "Eevee-Starter") return "Evoli-Starter"
            if (s == "Pikachu-Starter") return "Pikachu-Starter"
            if (s.contains("Rotom")) return s
            if (s.contains("Florges")) return "Florges"
            if (s.contains("Floette")) return "Floette"
            if (s.contains("Flabébé")) return "Flabébé"
            if (s == "Giratina-Origin") return s
            if (s.endsWith("-Zen")) return getMonName(s.substring(0, s.length - 4), gid, withDebug)
            if (s.contains("Silvally")) {
                val split = s.split("-").dropLastWhile { it.isEmpty() }
                return if (split.size == 1 || s == "Silvally-*") "Amigento" else if (split[1] == "Psychic") "Amigento-Psycho" else "Amigento-" + getGerName(
                    split[1], true
                ).translation
            }
            if (s.contains("Arceus")) {
                val split = s.split("-").dropLastWhile { it.isEmpty() }
                return if (split.size == 1 || s == "Arceus-*") "Arceus" else if (split[1] == "Psychic") "Arceus-Psycho" else "Arceus-" + getGerName(
                    split[1], true
                ).translation
            }
            if (s.contains("Basculin")) return "Barschuft"
            if (s.contains("Sawsbuck")) return "Kronjuwild"
            if (s.contains("Deerling")) return "Sesokitz"
            if (s == "Kyurem-Black") return "Kyurem-Black"
            if (s == "Kyurem-White") return "Kyurem-White"
            if (s == "Meowstic") return "Psiaugon-M"
            if (s == "Meowstic-F") return "Psiaugon-W"
            if (s.equals("Hoopa-Unbound", ignoreCase = true)) return "Hoopa-U"
            if (s == "Zygarde") return "Zygarde-50%"
            if (s == "Zygarde-10%") return "Zygarde-10%"
            if (s == "Zygarde-Complete") return "Zygarde-Optimum"
            if (s.endsWith("-Mega")) {
                return "M-" + getGerName(s.substring(0, s.length - 5), true).translation.ifEmpty {
                    s.substring(
                        0, s.length - 5
                    )
                }
            } else if (s.endsWith("-Alola")) {
                return "A-" + getGerName(s.substring(0, s.length - 6), true).translation
            } else if (s.endsWith("-Galar")) {
                return "G-" + getGerName(s.substring(0, s.length - 6), true).translation
            } else if (s.endsWith("-NML")) {
                return "NML-" + getGerName(s.substring(0, s.length - 6), true).translation
            } else if (s.endsWith("-Therian")) {
                return getGerName(s.substring(0, s.length - 8), true).translation + "-T"
            } else if (s.endsWith("-X")) {
                return "M-" + getGerName(
                    s.split("-").dropLastWhile { it.isEmpty() }[0], true
                ).translation + "-X"
            } else if (s.endsWith("-Y")) {
                return "M-" + getGerName(
                    s.split("-").dropLastWhile { it.isEmpty() }[0], true
                ).translation + "-Y"
            }
            if (s == "Tornadus") return "Boreos-I"
            if (s == "Thundurus") return "Voltolos-I"
            if (s == "Landorus") return "Demeteros-I"
            val gername = getGerName(s, true)
            val split = s.split("-").toMutableList()
            if (gername.isFromType(Translation.Type.POKEMON)) {
                return gername.translation
            }
            val first: String = split.removeAt(0)
            return "${
                getGerName(
                    first, true
                ).translation.takeIf { it.isNotBlank() } ?: first
            }${if ("-" in s) "-" + split.joinToString("-") else ""}"
        }

        fun buildEnumeration(vararg types: ArgumentType): String {
            return buildEnumeration(*types.map { it.getName() }.toTypedArray())
        }

        fun buildEnumeration(vararg types: String): String {
            val builder = StringBuilder(types.size shl 3)
            for (i in types.indices) {
                if (i > 0) {
                    if (i + 1 == types.size) builder.append(" oder ") else builder.append(", ")
                }
                builder.append(types[i])
            }
            return builder.toString()
        }

        fun isChannelAllowed(tc: TextChannel): Boolean {
            val gid = tc.guild.idLong
            return !emolgaChannel.containsKey(gid) || emolgaChannel[gid]!!.contains(tc.idLong) || emolgaChannel[gid]!!.isEmpty()
        }

        fun loadEmolgaJSON() {
            Emolga.get = JSON.decodeFromString(Files.readString(Paths.get("emolgadata.json")))
        }
    }
}

fun <T> T.indexedBy(list: List<T>) = list.indexOf(this)
val embedColor = java.awt.Color.CYAN.rgb
fun Int.x(factor: Int, summand: Int) = getAsXCoord(y(factor, summand))
fun Int.xdiv(divident: Int, summand: Int, factor: Int = 1) = getAsXCoord(ydiv(divident, summand, factor))
fun Int.xmod(mod: Int, summand: Int, factor: Int = 1) = getAsXCoord(ymod(mod, summand, factor))

@Suppress("unused")
fun Int.xc() = getAsXCoord(this)

fun Int.y(factor: Int, summand: Int) = this * factor + summand
fun Int.ydiv(divident: Int, summand: Int, factor: Int = 1) = (this / divident) * factor + summand
fun Int.ymod(mod: Int, summand: Int, factor: Int = 1) = (this % mod) * factor + summand

fun <T> MutableList<T>.replace(toreplace: T, replacer: T) {
    this[this.indexOf(toreplace)] = replacer
}

fun <T, R> Map<T, R>.reverseGet(value: R): T? = this.entries.firstOrNull { it.value == value }?.key

fun List<DraftPokemon>?.names() = this!!.map { it.name }

fun String.toSDName() = Command.toSDName(this)

private val logger: Logger by SLF4J

val defaultScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, t ->
    logger.error("ERROR IN DEFAULT SCOPE", t)
    sendToMe("Error in default scope, look in console")
})
private val JSON = Json {
    serializersModule = SerializersModule {
        polymorphic(League::class) {
            subclass(NDS::class)
            subclass(GDL::class)
            subclass(Prisma::class)
            subclass(ASL::class)
        }
    }
}

fun saveEmolgaJSON() {
    Files.writeString(Paths.get("emolgadata.json"), JSONObject(JSON.encodeToString(Emolga.get)).toString(4))
}

fun String.file() = File(this)

fun Collection<String>.startsAnyIgnoreCase(other: String) = filter { it.startsWith(other, ignoreCase = true) }

data class RandomTeamData(val shinyCount: AtomicInteger = AtomicInteger(), var hasDrampa: Boolean = false)

data class ReplayData(
    val mons: List<List<String>>,
    val url: String,
    val str: String,
    val resultchannel: TextChannel,
    val t1: StringBuilder,
    val t2: StringBuilder,
    val customReplayChannel: TextChannel?,
    val m: Message?
)