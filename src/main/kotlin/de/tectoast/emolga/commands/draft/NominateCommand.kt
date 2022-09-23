package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.buttons.buttonsaves.Nominate
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.PrivateCommand
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

class NominateCommand : PrivateCommand("nominate") {
    private val tiercomparator: Comparator<DraftPokemon>

    init {
        setIsAllowed {
            it.idLong in Emolga.get.nds().picks
        }
        val tiers = listOf("S", "A", "B", "C", "D")
        tiercomparator = compareBy({ it.tier.indexedBy(tiers) }, { it.name })
    }

    override fun process(e: MessageReceivedEvent) {
        val nds = Emolga.get.nds()
        val nom = nds.nominations
        if (e.author.idLong in nom.nominated.getOrPut(nom.currentDay) { mutableMapOf() }) {
            e.channel.sendMessage("Du hast für diesen Spieltag dein Team bereits nominiert!").queue()
            return
        }
        val list =
            nds.picks[if (e.author.idLong == Constants.FLOID) Command.WHITESPACES_SPLITTER.split(e.message.contentDisplay)[1].toLong() else e.author.idLong]!!.sortedWith(
                tiercomparator
            )
        val n = Nominate(list)
        e.channel.sendMessage(MessageCreate(embeds = Embed(
            title = "Nominierungen", color = embedColor, description = n.generateDescription()
        ).into(), components = Command.getActionRows(list.map { it.name }) {
            Button.primary(
                "nominate;$it", it
            )
        }.toMutableList().also { it.add(ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))) })
        ).queue { Command.nominateButtons[it.idLong] = n }
    }
}